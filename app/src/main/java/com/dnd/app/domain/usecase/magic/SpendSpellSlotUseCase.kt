// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\magic\SpendSpellSlotUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.magic

import com.dnd.app.domain.model.magic.SlotPreference
import com.dnd.app.domain.model.magic.SpellCastContext
import com.dnd.app.domain.model.snapshot.CharacterLiveState
import com.dnd.app.domain.model.snapshot.CharacterSnapshot
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.usecase.ConcentrationProtocol
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SpendSpellSlotUseCase @Inject constructor(
    private val repository: CharacterRepository,
    private val concentrationProtocol: ConcentrationProtocol
) {
    suspend operator fun invoke(
        characterId: Long,
        spellLevel: Int,
        preference: SlotPreference = SlotPreference.AUTO,
        innateSpellId: String? = null,
        spellContext: SpellCastContext? = null
    ): Result<Unit> {

        return repository.performAtomicMutation(characterId) { snapshot, liveState, _ ->
            val registry = snapshot.magic ?: return@performAtomicMutation Result.failure(Exception("Магия недоступна"))


            val pact = registry.pactMagic
            val globalSlots = registry.globalSlots

            if (innateSpellId != null) {
                val usedCount = liveState.innateUsage[innateSpellId] ?: 0
                if (usedCount >= 1) {
                    return@performAtomicMutation Result.failure(Exception("Р—Р°СЂСЏРґ РЅРµ РґРѕСЃС‚СѓРїРµРЅ"))
                }
                val nextUsage = liveState.innateUsage.toMutableMap()
                nextUsage[innateSpellId] = usedCount + 1
                val nextLive = liveState.copy(innateUsage = nextUsage)
                return@performAtomicMutation Result.success(nextLive to Unit)
            }
            if (spellLevel == 0) return@performAtomicMutation Result.success(liveState to Unit)

            fun finalizeLive(nextLive: CharacterLiveState): Result<Pair<CharacterLiveState, Unit>> {
                val finalLive = spellContext?.let { concentrationProtocol.applySpellCast(snapshot, nextLive, it) } ?: nextLive
                return Result.success(finalLive to Unit)
            }

            fun usePactSlot(): Result<Pair<CharacterLiveState, Unit>>? {
                if (pact == null) return null
                if (pact.slotLevel < spellLevel) return null
                if (liveState.spentPactSlots >= pact.maxSlots) return null
                val nextLive = liveState.copy(spentPactSlots = liveState.spentPactSlots + 1)
                return finalizeLive(nextLive)
            }

            fun useGlobalSlot(): Result<Pair<CharacterLiveState, Unit>>? {
                val availableSlotLevel = (spellLevel..9).firstOrNull { lvl ->
                    val max = globalSlots[lvl] ?: 0
                    val spent = liveState.spentGlobalSlots[lvl] ?: 0
                    spent < max
                } ?: return null
                val nextGlobalMap = liveState.spentGlobalSlots.toMutableMap()
                nextGlobalMap[availableSlotLevel] = (nextGlobalMap[availableSlotLevel] ?: 0) + 1
                val nextLive = liveState.copy(spentGlobalSlots = nextGlobalMap)
                return finalizeLive(nextLive)
            }

            if (preference == SlotPreference.GLOBAL_FIRST) {
                useGlobalSlot()?.let { return@performAtomicMutation it }
                return@performAtomicMutation Result.failure(
                    Exception("Нет доступных глобальных ячеек уровня $spellLevel+")
                )
            }

            val attempts = listOf(::usePactSlot, ::useGlobalSlot)

            for (attempt in attempts) {
                attempt()?.let { return@performAtomicMutation it }
            }

            Result.failure(Exception("Нет доступных ячеек уровня $spellLevel+"))
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\magic\SpendSpellSlotUseCase.kt


