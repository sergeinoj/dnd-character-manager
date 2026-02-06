// Имя файла: data/local/dao/ReferenceDao.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.dnd.app.data.local.entity.ClassRawEntity
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.data.local.entity.RaceRawEntity
import com.dnd.app.data.local.entity.SpellEntity
import com.dnd.app.data.local.entity.WeaponEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceDao {
    @Query("SELECT * FROM races ORDER BY name ASC")
    suspend fun getAllRacesRaw(): List<RaceRawEntity>

    @Query("SELECT * FROM classes ORDER BY name ASC")
    suspend fun getAllClassesRaw(): List<ClassRawEntity>

    @Query("SELECT * FROM spells ORDER BY name ASC")
    fun getAllSpells(): Flow<List<SpellEntity>>

    @Query("SELECT * FROM weapons ORDER BY label ASC")
    fun getAllWeapons(): Flow<List<WeaponEntity>>

    // ... остальные методы для полноты
    @Query("SELECT * FROM spells WHERE level = :level ORDER BY name ASC")
    suspend fun getSpellsByLevel(level: Int): List<SpellEntity>

    @Query("SELECT * FROM spells WHERE id IN (:ids)")
    suspend fun getSpellsByIds(ids: List<Int>): List<SpellEntity>

    @Query("SELECT * FROM weapons WHERE label LIKE '%' || :query || '%'")
    suspend fun searchWeapons(query: String): List<WeaponEntity>

    @Query("SELECT * FROM weapons WHERE id IN (:ids)")
    suspend fun getWeaponsByIds(ids: List<Int>): List<WeaponEntity>

    @Query("SELECT * FROM features WHERE id = :id")
    suspend fun getFeatureById(id: Int): FeatureEntity?
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: data/local/dao/ReferenceDao.kt