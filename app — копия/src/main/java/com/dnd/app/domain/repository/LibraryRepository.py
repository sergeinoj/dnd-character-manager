// Имя файла: app/src/main/java/com/dnd/app/domain/repository/LibraryRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.repository

import com.dnd.app.domain.model.ClassInfo
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.Race
import com.dnd.app.domain.model.Spell
import com.dnd.app.domain.model.Weapon
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    suspend fun getAllRaces(): List<Race>
    suspend fun getAllClasses(): List<ClassInfo>

    fun getAllSpells(): Flow<List<Spell>>
    suspend fun getSpellsByLevel(level: Int): List<Spell>
    suspend fun getSpellsByIds(ids: List<Int>): List<Spell>

    fun getAllWeapons(): Flow<List<Weapon>>
    suspend fun searchWeapons(query: String): List<Weapon>
    suspend fun getWeaponsByIds(ids: List<Int>): List<Weapon>
    suspend fun getFeatureById(id: Int): Feature?
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/repository/LibraryRepository.kt