// Имя файла: app/src/main/java/com/dnd/app/data/local/dao/ReferenceDao.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.dnd.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceDao {
    // --- КЛАССЫ ---
    @Query("SELECT * FROM classes ORDER BY name ASC")
    suspend fun getAllClasses(): List<ClassEntity>

    @Query("SELECT * FROM subclasses WHERE class_index = :classIndex ORDER BY name ASC")
    suspend fun getSubclassesForClass(classIndex: String): List<SubclassEntity>

    @Query("SELECT * FROM progression WHERE class_index = :classIndex AND level = :level")
    suspend fun getProgressionForLevel(classIndex: String, level: Int): List<ProgressionEntity>

    // --- ФИЧИ ---
    @Query("SELECT * FROM features WHERE index_name IN (:indexes) ORDER BY level ASC")
    suspend fun getFeaturesByIndexes(indexes: List<String>): List<FeatureEntity>

    // ИСПРАВЛЕНИЕ: Добавлены недостающие методы поиска фич
    @Query("SELECT * FROM features WHERE id = :id")
    suspend fun getFeatureById(id: Int): FeatureEntity?

    @Query("SELECT * FROM features WHERE index_name = :indexName")
    suspend fun getFeatureByIndex(indexName: String): FeatureEntity?

    @Query("SELECT * FROM features WHERE class_index = :classIdx OR subclass_index = :subIdx OR race_index = :raceIdx OR subrace_index = :subraceIdx OR background_index = :bgIdx")
    suspend fun findFeaturesByContext(
        classIdx: String? = null,
        subIdx: String? = null,
        raceIdx: String? = null,
        subraceIdx: String? = null,
        bgIdx: String? = null
    ): List<FeatureEntity>

    // --- СНАРЯЖЕНИЕ И КАТЕГОРИИ ---
    @Query("""
        SELECT * FROM equipment WHERE category_index = :catIdx
        UNION
        SELECT * FROM equipment WHERE index_name IN (SELECT item_index FROM equipment_category_links WHERE category_index = :catIdx)
    """)
    suspend fun getEquipmentByCategory(catIdx: String): List<EquipmentEntity>

    // --- РАСЫ ---
    // ИСПРАВЛЕНИЕ: Унифицировано название метода getAllRaces
    @Query("SELECT * FROM races ORDER BY name ASC")
    suspend fun getAllRaces(): List<RaceEntity>

    @Query("SELECT * FROM subraces WHERE race_index = :raceIndex ORDER BY name ASC")
    suspend fun getSubracesForRace(raceIndex: String): List<SubraceEntity>

    // --- БЭКГРАУНДЫ ---
    @Query("SELECT * FROM backgrounds ORDER BY name ASC")
    suspend fun getAllBackgrounds(): List<BackgroundEntity>

    @Query("SELECT * FROM alignments ORDER BY id ASC")
    suspend fun getAllAlignments(): List<AlignmentEntity>

    // --- МАГИЯ ---
    @Query("SELECT * FROM spells WHERE index_name IN (:indexes)")
    suspend fun getSpellsByIndexes(indexes: List<String>): List<SpellEntity>

    @Query("SELECT * FROM spells WHERE level = :level AND (classes_json LIKE '%' || :clsIdx || '%' OR :clsIdx IS NULL) ORDER BY name ASC")
    suspend fun getSpellsByLevel(level: Int, clsIdx: String?): List<SpellEntity>

    @Query("SELECT * FROM spells ORDER BY name ASC")
    fun getAllSpells(): Flow<List<SpellEntity>>

    @Query("SELECT * FROM spells WHERE id IN (:ids)")
    suspend fun getSpellsByIds(ids: List<Int>): List<SpellEntity>

    // --- ИНВЕНТАРЬ ---
    @Query("SELECT * FROM weapons ORDER BY label ASC")
    fun getAllWeapons(): Flow<List<WeaponEntity>>

    @Query("SELECT * FROM weapons WHERE id IN (:ids)")
    suspend fun getWeaponsByIds(ids: List<Int>): List<WeaponEntity>

    @Query("SELECT * FROM armor ORDER BY name ASC")
    fun getAllArmor(): Flow<List<ArmorEntity>>

    @Query("SELECT * FROM equipment WHERE name LIKE '%' || :query || '%'")
    suspend fun searchEquipment(query: String): List<EquipmentEntity>

    @Query("SELECT id FROM equipment WHERE index_name IN (:idxNames)")
    suspend fun getEquipmentIdsByIdxNames(idxNames: List<String>): List<Int>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/dao/ReferenceDao.kt