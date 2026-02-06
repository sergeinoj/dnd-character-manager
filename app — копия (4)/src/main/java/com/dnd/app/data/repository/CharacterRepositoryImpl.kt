// Имя файла: app/src/main/java/com/dnd/app/data/repository/CharacterRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.entity.CharacterEntity
import com.dnd.app.domain.model.Bio
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.Stats
import com.dnd.app.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CharacterRepositoryImpl @Inject constructor(
    private val dao: CharacterDao
) : CharacterRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllCharacters(): Flow<List<CharacterDomain>> {
        return dao.getAllCharacters().map { entities ->
            entities.map { mapEntityToDomain(it) }
        }
    }

    override suspend fun getCharacterById(id: Long): CharacterDomain? {
        val entity = dao.getCharacterById(id) ?: return null
        return mapEntityToDomain(entity)
    }

    /**
     * Новый метод: Получение черновика для редактирования.
     * Если черновика нет в базе (старый персонаж), мы должны попытаться восстановить его из домена,
     * но пока возвращаем пустой (это повод для миграции данных в будущем).
     */
    suspend fun getDraftById(id: Long): DraftCharacter? {
        return dao.getCharacterById(id)?.draftData
    }

    override suspend fun saveCharacter(character: CharacterDomain): Long {
        // ВНИМАНИЕ: Этот метод теперь сохраняет только SNAPSHOT.
        // DraftCharacter должен сохраняться отдельно или передаваться сюда же,
        // но в текущей архитектуре Assembler возвращает Domain, теряя Draft.
        // Эту логику мы поправим в ViewModel: она будет сохранять Entity, имея на руках И Draft И Domain.

        // Временная заглушка, чтобы код компилировался.
        // Реальное сохранение происходит через ViewModel, которая собирает Entity вручную.
        val entity = mapDomainToEntity(character, null)
        return dao.insertCharacter(entity)
    }

    suspend fun saveFullCharacter(domain: CharacterDomain, draft: DraftCharacter) {
        val entity = mapDomainToEntity(domain, draft)
        if (domain.id == 0L) {
            dao.insertCharacter(entity)
        } else {
            dao.updateCharacter(entity)
        }
    }

    override suspend fun deleteCharacter(characterId: Long) {
        // Для удаления Room требует Entity, но ему достаточно только ID, если он помечен @PrimaryKey
        // Однако, безопаснее сделать Query в DAO: DELETE FROM characters WHERE id = :id
        // Пока используем старый метод:
        val dummy = dao.getCharacterById(characterId)
        if (dummy != null) dao.deleteCharacter(dummy)
    }

    private fun mapEntityToDomain(entity: CharacterEntity): CharacterDomain {
        return try {
            CharacterDomain(
                id = entity.id,
                name = entity.name,
                raceName = entity.raceName,
                className = entity.className,
                level = entity.level,
                hpCurrent = entity.hpCurrent,
                hpMax = entity.hpMax,
                stats = try { json.decodeFromString<Stats>(entity.statsJson) } catch (e: Exception) { Stats() },
                inventoryIds = try { json.decodeFromString<List<Int>>(entity.inventoryIdsJson) } catch (e: Exception) { emptyList() },
                spellsKnownIds = try { json.decodeFromString<List<Int>>(entity.spellsKnownIdsJson) } catch (e: Exception) { emptyList() },
                bio = try { json.decodeFromString<Bio>(entity.bioJson) } catch (e: Exception) { Bio() },
                skillProficiencies = try { json.decodeFromString<Map<String, Int>>(entity.skillProficienciesJson) } catch (e: Exception) { emptyMap() }
            )
        } catch (e: Exception) {
            CharacterDomain(id = entity.id, name = "Error Data")
        }
    }

    private fun mapDomainToEntity(domain: CharacterDomain, draft: DraftCharacter?): CharacterEntity {
        return CharacterEntity(
            id = domain.id,
            name = domain.name,
            raceName = domain.raceName,
            className = domain.className,
            level = domain.level,
            hpCurrent = domain.hpCurrent,
            hpMax = domain.hpMax,
            statsJson = json.encodeToString(domain.stats),
            inventoryIdsJson = json.encodeToString(domain.inventoryIds),
            spellsKnownIdsJson = json.encodeToString(domain.spellsKnownIds),
            bioJson = json.encodeToString(domain.bio),
            skillProficienciesJson = json.encodeToString(domain.skillProficiencies),
            draftData = draft
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/CharacterRepositoryImpl.kt