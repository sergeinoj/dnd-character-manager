// Имя файла: app/src/main/java/com/dnd/app/data/repository/CharacterRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.entity.CharacterEntity
import com.dnd.app.domain.model.Bio
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.model.Stats
import com.dnd.app.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

// Зависимость теперь идет от CharacterDao, который предоставляется из AppDatabase
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

    override suspend fun saveCharacter(character: CharacterDomain): Long {
        val entity = mapDomainToEntity(character)
        return dao.insertCharacter(entity)
    }

    override suspend fun deleteCharacter(characterId: Long) {
        val dummyEntity = CharacterEntity(
            id = characterId,
            name = "", raceId = 0, classId = 0, level = 0,
            hpCurrent = 0, hpMax = 0, statsJson = "", inventoryIdsJson = "",
            spellsKnownIdsJson = "", bioJson = "", skillProficienciesJson = ""
        )
        dao.deleteCharacter(dummyEntity)
    }

    private fun mapEntityToDomain(entity: CharacterEntity): CharacterDomain {
        return try {
            CharacterDomain(
                id = entity.id,
                name = entity.name,
                raceId = entity.raceId,
                classId = entity.classId,
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

    private fun mapDomainToEntity(domain: CharacterDomain): CharacterEntity {
        return CharacterEntity(
            id = domain.id,
            name = domain.name,
            raceId = domain.raceId,
            classId = domain.classId,
            level = domain.level,
            hpCurrent = domain.hpCurrent,
            hpMax = domain.hpMax,
            statsJson = json.encodeToString(domain.stats),
            inventoryIdsJson = json.encodeToString(domain.inventoryIds),
            spellsKnownIdsJson = json.encodeToString(domain.spellsKnownIds),
            bioJson = json.encodeToString(domain.bio),
            skillProficienciesJson = json.encodeToString(domain.skillProficiencies)
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/CharacterRepositoryImpl.kt