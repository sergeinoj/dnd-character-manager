// Имя файла: app/src/main/java/com/dnd/app/data/repository/LibraryRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.domain.model.ClassInfo
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.Race
import com.dnd.app.domain.model.Spell
import com.dnd.app.domain.model.Weapon
import com.dnd.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

// Зависимость теперь идет от ReferenceDao, который предоставляется из ReferenceDatabase
class LibraryRepositoryImpl @Inject constructor(
    private val dao: ReferenceDao
) : LibraryRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAllRaces(): List<Race> {
        return dao.getAllRacesRaw().map { entity ->
            val bonuses = try {
                entity.statsJson?.let { json.decodeFromString<Map<String, Int>>(it) } ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
            Race(id = entity.id, name = entity.name, statBonuses = bonuses)
        }
    }

    override suspend fun getAllClasses(): List<ClassInfo> {
        return dao.getAllClassesRaw().map { entity ->
            ClassInfo(id = entity.id, name = entity.name, hitDie = entity.hitDie ?: 8)
        }
    }

    override fun getAllSpells(): Flow<List<Spell>> = dao.getAllSpells().map { list -> list.map { it.toDomain() } }
    override suspend fun getSpellsByLevel(level: Int): List<Spell> = dao.getSpellsByLevel(level).map { it.toDomain() }
    override suspend fun getSpellsByIds(ids: List<Int>): List<Spell> = dao.getSpellsByIds(ids).map { it.toDomain() }
    override fun getAllWeapons(): Flow<List<Weapon>> = dao.getAllWeapons().map { list -> list.map { it.toDomain() } }
    override suspend fun searchWeapons(query: String): List<Weapon> = dao.searchWeapons(query).map { it.toDomain() }
    override suspend fun getWeaponsByIds(ids: List<Int>): List<Weapon> = dao.getWeaponsByIds(ids).map { it.toDomain() }
    override suspend fun getFeatureById(id: Int): Feature? = dao.getFeatureById(id)?.toDomain()
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/LibraryRepositoryImpl.kt