// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\CharacterRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import android.util.Log
import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.entity.CharacterEntity
import com.dnd.app.domain.model.*
import com.dnd.app.domain.model.snapshot.CharacterLiveState
import com.dnd.app.domain.model.snapshot.CharacterSnapshot
import com.dnd.app.domain.model.snapshot.ProficiencyType
import com.dnd.app.domain.model.snapshot.SheetCharacter
import com.dnd.app.domain.model.snapshot.PurchasedItemRecord
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.DataConflictException
import com.dnd.app.domain.repository.MutationResult
import com.dnd.app.domain.usecase.inventory.UnpackedItem
import com.dnd.app.domain.usecase.inventory.UnpackItemUseCase
import com.dnd.app.domain.usecase.snapshot.SnapshotAssembler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class CharacterRepositoryImpl @Inject constructor(
    private val dao: CharacterDao,
    private val snapshotAssembler: SnapshotAssembler,
    private val unpackItemUseCase: UnpackItemUseCase,
    private val json: Json
) : CharacterRepository {

    private val repositoryMutex = Mutex()
    private val TAG = "ResonanceEngine"
    private val MAX_RETRIES = 3

    companion object {
        private val AMMO_MAPPING = mapOf(
            "ammo_arrow" to ("arrow" to 20),
            "ammo_bolt" to ("crossbow-bolt" to 20),
            "ammo_bullet" to ("sling-bullet" to 20),
            "ammo_needle" to ("blowgun-needle" to 50)
        )


        private val WEAPON_TO_AMMO_FALLBACK = mapOf(
            "longbow" to ("arrow" to 20),
            "shortbow" to ("arrow" to 20),
            "crossbow-light" to ("crossbow-bolt" to 20),
            "crossbow-heavy" to ("crossbow-bolt" to 20),
            "crossbow-hand" to ("crossbow-bolt" to 20),
            "sling" to ("sling-bullet" to 20),
            "blowgun" to ("blowgun-needle" to 50)
        )
    }

    override fun getAllCharacters(): Flow<List<CharacterDomain>> = dao.getAllCharacters().mapLatest { entities ->
        entities.map(::mapEntityToLegacyDomain)
    }

    override suspend fun getDraftById(id: Long): DraftCharacter? = withContext(Dispatchers.IO) {
        dao.getCharacterById(id)?.toSanitizedDraft()
    }

    override fun getCharacterForSheet(id: Long): Flow<SheetCharacter?> = dao.getCharacterFlowById(id)
        .distinctUntilChanged { old, new ->
            old?.versionId == new?.versionId
        }
        .mapLatest { entity ->
            if (entity == null) return@mapLatest null
            val snapshot = entity.snapshotJson ?: return@mapLatest null
            val liveState = entity.liveStateJson ?: CharacterLiveState(hpCurrent = snapshot.maxHp)
            val draft = entity.draftData
            Log.d(
                TAG,
                "OPEN_SHEET id=${entity.id} version=${entity.versionId} draftNull=${draft == null} " +
                    "draftLevels=${draft?.levelStack?.size ?: -1} snapLevel=${snapshot.global.level} " +
                    "name='${snapshot.global.name}'"
            )
            SheetCharacter(entity.id, snapshot, liveState)
        }

    override suspend fun <T> performAtomicMutation(
        id: Long,
        block: suspend (snapshot: CharacterSnapshot, liveState: CharacterLiveState, draft: DraftCharacter) -> Result<MutationResult<T>>
    ): Result<T> = withContext(Dispatchers.Default) {
        repeat(MAX_RETRIES) { attempt ->
            val stepResult = repositoryMutex.withLock {
                val entity = dao.getCharacterById(id) ?: return@withLock Result.failure(Exception("Char $id not found"))
                val snapshot = entity.snapshotJson ?: return@withLock Result.failure(Exception("Snap $id corrupted"))
                val liveState = entity.liveStateJson ?: CharacterLiveState(hpCurrent = snapshot.maxHp)
                val draft = entity.toSanitizedDraft() ?: return@withLock Result.failure(Exception("Draft $id corrupted"))

                val oldVersion = entity.versionId

                block(snapshot, liveState, draft).fold(
                    onSuccess = { (newLiveState, value) ->
                        val unpackedInv = buildAndUnpackInventory(draft, newLiveState.purchasedItems)

                        val (newSnap, finalLive) = snapshotAssembler(
                            draft = draft,
                            oldSnapshot = snapshot,
                            oldLiveState = newLiveState,
                            entityCurrentHp = newLiveState.hpCurrent,
                            unpackedInventory = unpackedInv
                        )

                        val nextVersion = oldVersion + 1
                        val finalSnap = newSnap.copy(versionId = nextVersion)

                        val snapJson = json.encodeToString(finalSnap)
                        val liveJson = json.encodeToString(finalLive)

                        val rowsAffected = dao.updateWithOptimisticLock(
                            id = id,
                            expectedVersion = oldVersion,
                            newVersion = nextVersion,
                            draftData = draft,
                            snapshotJson = snapJson,
                            liveStateJson = liveJson,
                            name = finalSnap.global.name,
                            raceName = finalSnap.global.race,
                            className = finalSnap.global.classTitle,
                            level = finalSnap.global.level,
                            hpCurrent = finalLive.hpCurrent,
                            hpMax = finalSnap.maxHp
                        )

                        if (rowsAffected > 0) Result.success(value) else null
                    },
                    onFailure = { Result.failure(it) }
                )
            }

            if (stepResult != null) return@withContext stepResult
            delay(attempt * 50L)
            Log.w(TAG, "OCC Conflict on $id, attempt $attempt/$MAX_RETRIES")
        }
        Result.failure(DataConflictException("OCC Conflict retry limit reached."))
    }

    override suspend fun syncLiveState(
        id: Long,
        transform: (CharacterLiveState) -> CharacterLiveState
    ): Result<Unit> = performAtomicMutation(id) { _, live, _ ->
        val nextLive = transform(live)
        Result.success(nextLive to Unit)
    }

    override suspend fun commitFullCharacter(draft: DraftCharacter): Result<Long> = withContext(Dispatchers.Default) {
        runCatching {
            withContext(NonCancellable) {
                repositoryMutex.withLock {
                    Log.d(
                        TAG,
                        "COMMIT_ENTER_V2 draftId=${draft.id} name='${draft.name}' levels=${draft.levelStack.size} " +
                            "class='${draft.baseInfo.startingClassIndex}' race='${draft.baseInfo.raceIndex}' bg='${draft.baseInfo.backgroundIndex}'"
                    )
                    val old = if (draft.id != 0L) dao.getCharacterById(draft.id) else null
                    val existingPurchased = old?.liveStateJson?.purchasedItems ?: emptyList()
                    val unpackedInv = buildAndUnpackInventory(draft, existingPurchased)

                    val (snap, live) = snapshotAssembler(
                        draft = draft,
                        oldSnapshot = old?.snapshotJson,
                        oldLiveState = old?.liveStateJson,
                        entityCurrentHp = old?.hpCurrent,
                        unpackedInventory = unpackedInv
                    )

                    val nextVersion = (old?.versionId ?: 0L) + 1L
                    val finalSnap = snap.copy(versionId = nextVersion)

                    val entity = CharacterEntity(
                        id = draft.id,
                        versionId = nextVersion,
                        name = draft.name.ifBlank { "Hero" },
                        draftData = draft,
                        snapshotJson = finalSnap,
                        liveStateJson = live,
                        raceName = finalSnap.global.race,
                        className = finalSnap.global.classTitle,
                        level = finalSnap.global.level,
                        hpMax = finalSnap.maxHp,
                        hpCurrent = live.hpCurrent
                    )

                    val upsertId = dao.upsertCharacter(entity)
                    val storedId = if (draft.id != 0L) draft.id else upsertId
                    val rawDraft = dao.getDraftRawJson(storedId)
                    Log.d(
                        TAG,
                        "POST_SAVE id=$storedId draftRawNull=${rawDraft == null} draftRawLen=${rawDraft?.length ?: -1} " +
                            "draftLevels=${draft.levelStack.size} race='${draft.baseInfo.raceIndex}' bg='${draft.baseInfo.backgroundIndex}'"
                    )
                    storedId
                }
            }
        }
    }


    private suspend fun buildAndUnpackInventory(
        draft: DraftCharacter,
        purchasedItems: List<PurchasedItemRecord>
    ): List<UnpackedItem> {
        val sourceMap = mutableMapOf<String, String>()


        draft.baseInfo.staticEquipment.forEachIndexed { index, itemId ->
            sourceMap["static_$index"] = itemId
        }


        draft.baseInfo.inventorySelections.forEach { (sourceKey, result) ->
            if (result is ChoiceResult.SelectedOptions) {

                val isInventoryItem = result.proficiencyKind == ProficiencyKind.NONE ||
                        result.proficiencyKind == ProficiencyKind.WEAPON ||
                        result.proficiencyKind == ProficiencyKind.ARMOR

                if (isInventoryItem) {
                    result.items.forEachIndexed { idx, item ->
                        sourceMap["inv_${sourceKey}_$idx"] = item
                    }
                }
            }
        }

        purchasedItems.forEach { record ->
            sourceMap[record.traceKey] = record.refIndex
        }

        val initialUnpacked = unpackItemUseCase(sourceMap)
        val ammoMap = mutableMapOf<String, String>()




        val quantities = mutableMapOf<String, Int>()

        initialUnpacked.forEach { item ->

            if (item.sourceKey.startsWith("static_") || item.sourceKey.startsWith("inv_")) {
                var ammoFound = false


                item.properties.forEach { props ->
                    val match = Regex("""\[(ammo_[a-z0-9_]+)\]""").find(props)
                    if (match != null) {
                        val tag = match.groupValues[1]
                        AMMO_MAPPING[tag]?.let { (ammoIndex, amount) ->
                            val ammoKey = "${item.sourceKey}_auto_ammo"
                            ammoMap[ammoKey] = ammoIndex
                            quantities[ammoKey] = amount
                            ammoFound = true
                        }
                    }
                }


                if (!ammoFound) {
                    val fallbackIndex = item.itemId
                    WEAPON_TO_AMMO_FALLBACK[fallbackIndex]?.let { (ammoIndex, amount) ->
                         val ammoKey = "${item.sourceKey}_auto_ammo"
                         ammoMap[ammoKey] = ammoIndex
                         quantities[ammoKey] = amount
                    }
                }
            }
        }

        val additionalAmmo = if (ammoMap.isNotEmpty()) {
            unpackItemUseCase(ammoMap).map {

                it.copy(quantity = quantities[it.sourceKey] ?: 1)
            }
        } else emptyList()
        return initialUnpacked + additionalAmmo
    }

    override suspend fun deleteCharacter(characterId: Long) {
        repositoryMutex.withLock { dao.getCharacterById(characterId)?.let { dao.deleteCharacter(it) } }
    }

    private fun CharacterEntity.toSanitizedDraft(): DraftCharacter? {
        return this.draftData?.copy(id = this.id)
    }

    private fun mapEntityToLegacyDomain(entity: CharacterEntity): CharacterDomain {
        val snap = entity.snapshotJson ?: return CharacterDomain(
            id = entity.id,
            name = entity.name,
            raceName = entity.raceName,
            className = entity.className,
            level = entity.level,
            stats = Stats(),
            hpCurrent = entity.hpCurrent,
            hpMax = entity.hpMax
        )
        val live = entity.liveStateJson ?: CharacterLiveState()
        return CharacterDomain(
            id = entity.id, name = entity.name, raceName = entity.raceName, className = entity.className,
            level = entity.level, hpCurrent = entity.hpCurrent, hpMax = entity.hpMax,
            stats = Stats(
                strength = snap.statsMap["STR"]?.value ?: 10,
                dexterity = snap.statsMap["DEX"]?.value ?: 10,
                constitution = snap.statsMap["CON"]?.value ?: 10,
                intelligence = snap.statsMap["INT"]?.value ?: 10,
                wisdom = snap.statsMap["WIS"]?.value ?: 10,
                charisma = snap.statsMap["CHA"]?.value ?: 10,
                copper = live.coins.cp, silver = live.coins.sp, gold = live.coins.gp
            ),
            bio = Bio(alignment = snap.global.alignment, backgroundName = snap.global.backgroundName),
            skillProficiencies = snap.skills.filter { it.profType != ProficiencyType.NONE }.associate { "skill-${it.code}" to if (it.profType == ProficiencyType.EXPERTISE) 2 else 1 }
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\CharacterRepositoryImpl.kt
