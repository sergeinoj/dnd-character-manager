// Имя файла: app/src/main/java/com/dnd/app/data/local/dao/ReferenceDao.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.dnd.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceDao {
    // --- КЛАССЫ И ПРОГРЕССИЯ ---
    @Query("SELECT * FROM classes ORDER BY name ASC")
    suspend fun getAllClasses(): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE index_name = :indexName")
    suspend fun getClassByIndex(indexName: String): ClassEntity?

    @Query("SELECT * FROM subclasses WHERE class_index = :classIndex ORDER BY name ASC")
    suspend fun getSubclassesForClass(classIndex: String): List<SubclassEntity>

    // [НОВЫЙ МЕТОД v1.26.2] Прямой поиск подкласса по индексу.
    @Query("SELECT * FROM subclasses WHERE index_name = :indexName")
    suspend fun getSubclassByIndex(indexName: String): SubclassEntity?

    @Query("SELECT * FROM progression WHERE class_index = :classIndex AND level = :level")
    suspend fun getProgressionForLevel(classIndex: String, level: Int): List<ProgressionEntity>

    @Query("SELECT * FROM progression WHERE class_index = :classIndex AND level IN (:levels)")
    suspend fun getProgressionForLevels(classIndex: String, levels: List<Int>): List<ProgressionEntity>

    // --- РАСЫ ---
    @Query("SELECT * FROM races ORDER BY name ASC")
    suspend fun getAllRaces(): List<RaceEntity>

    @Query("SELECT * FROM subraces WHERE race_index = :raceIndex ORDER BY name ASC")
    suspend fun getSubracesForRace(raceIndex: String): List<SubraceEntity>

    @Query("SELECT * FROM subraces WHERE index_name = :index")
    suspend fun getSubraceByIndex(index: String): SubraceEntity?

    // --- ФИЧИ (Универсальный поиск) ---
    @Query("""
        SELECT * FROM features 
        WHERE (race_index = :raceIdx AND subrace_index IS NULL)
        OR (subrace_index = :subIdx)
        OR (class_index = :classIdx AND subclass_index IS NULL)
        OR (subclass_index = :subclassIdx)
        OR (background_index = :bgIdx)
        ORDER BY level ASC, id ASC
    """)
    suspend fun findFeaturesByContext(
        raceIdx: String? = null,
        subIdx: String? = null,
        classIdx: String? = null,
        subclassIdx: String? = null,
        bgIdx: String? = null
    ): List<FeatureEntity>

    @Query("SELECT * FROM features WHERE index_name IN (:indexes) ORDER BY level ASC")
    suspend fun getFeaturesByIndexes(indexes: List<String>): List<FeatureEntity>

    @Query("SELECT * FROM features WHERE index_name = :indexName")
    suspend fun getFeatureByIndex(indexName: String): FeatureEntity?

    @Query("SELECT * FROM features WHERE id = :id")
    suspend fun getFeatureById(id: Int): FeatureEntity?

    // [ИЗМЕНЕНО v1.26] Запрос теперь ищет по ui_group, а не по префиксу.
    @Query("SELECT * FROM features WHERE ui_group = 'FEAT' ORDER BY name ASC")
    suspend fun getAllFeats(): List<FeatureEntity>

    @Query("SELECT * FROM features WHERE index_name LIKE :pattern")
    suspend fun getFeaturesLike(pattern: String): List<FeatureEntity>

    // --- ЗАКЛИНАНИЯ ---
    @Query("SELECT * FROM spells WHERE index_name IN (:indexes)")
    suspend fun getSpellsByIndexes(indexes: List<String>): List<SpellEntity>

    @Query("SELECT * FROM spells WHERE id IN (:ids)")
    suspend fun getSpellsByIds(ids: List<Int>): List<SpellEntity>

    @Query("SELECT * FROM spells ORDER BY name ASC")
    fun getAllSpells(): Flow<List<SpellEntity>>

    @Query("SELECT * FROM spells ORDER BY name ASC")
    suspend fun getAllSpellsSuspend(): List<SpellEntity>

    @Query("SELECT * FROM spells WHERE level = :level ORDER BY name ASC")
    suspend fun getSpellsByLevel(level: Int): List<SpellEntity>

    // --- СНАРЯЖЕНИЕ ---
    @Query("SELECT * FROM weapons ORDER BY name ASC")
    fun getAllWeapons(): Flow<List<WeaponEntity>>

    @Query("SELECT * FROM weapons ORDER BY name ASC")
    suspend fun getAllWeaponsSuspend(): List<WeaponEntity>

    @Query("SELECT * FROM weapons WHERE id IN (:ids)")
    suspend fun getWeaponsByIds(ids: List<Int>): List<WeaponEntity>

    @Query("SELECT * FROM weapons WHERE index_name IN (:indexes) ORDER BY name ASC")
    suspend fun getWeaponsByIndexes(indexes: List<String>): List<WeaponEntity>

    @Query("SELECT * FROM weapons WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchWeapons(query: String): List<WeaponEntity>

    @Query("SELECT * FROM armor ORDER BY name ASC")
    fun getAllArmor(): Flow<List<ArmorEntity>>

    @Query("SELECT * FROM armor ORDER BY name ASC")
    suspend fun getAllArmorSuspend(): List<ArmorEntity>

    @Query("SELECT * FROM armor WHERE index_name IN (:indexes) ORDER BY name ASC")
    suspend fun getArmorByIndexes(indexes: List<String>): List<ArmorEntity>

    @Query("SELECT * FROM armor WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchArmor(query: String): List<ArmorEntity>

    @Query("""
        SELECT * FROM equipment WHERE category_index = :catIdx
        UNION
        SELECT * FROM equipment WHERE index_name IN (SELECT item_index FROM equipment_category_links WHERE category_index = :catIdx)
    """)
    suspend fun getEquipmentByCategory(catIdx: String): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE index_name IN (:indexes) ORDER BY name ASC")
    suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchEquipment(query: String): List<EquipmentEntity>

    @Query("SELECT id FROM equipment WHERE index_name IN (:idxNames)")
    suspend fun getEquipmentIdsByIdxNames(idxNames: List<String>): List<Int>

    // Magic Items
    @Query("SELECT * FROM magic_items WHERE index_name IN (:indexes) ORDER BY name ASC")
    suspend fun getMagicItemsByIndexes(indexes: List<String>): List<MagicItemEntity>

    @Query("SELECT * FROM magic_items WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchMagicItems(query: String): List<MagicItemEntity>

    // Categories & Links
    @Query("SELECT * FROM equipment_categories WHERE index_name = :index")
    suspend fun getEquipmentCategoryByIndex(index: String): EquipmentCategoryEntity?

    @Query("SELECT item_index FROM equipment_category_links WHERE category_index = :categoryIndex")
    suspend fun getLinksForCategory(categoryIndex: String): List<String>

    @Query("SELECT * FROM equipment_categories ORDER BY name ASC")
    suspend fun getAllEquipmentCategories(): List<EquipmentCategoryEntity>

    @Query("SELECT * FROM equipment_categories WHERE parent_index IS NULL ORDER BY name ASC")
    suspend fun getRootEquipmentCategories(): List<EquipmentCategoryEntity>

    @Query("SELECT * FROM equipment_categories WHERE parent_index = :parentIndex ORDER BY name ASC")
    suspend fun getChildEquipmentCategories(parentIndex: String): List<EquipmentCategoryEntity>

    // --- ОБЩЕЕ ---
    @Query("SELECT * FROM backgrounds ORDER BY name ASC")
    suspend fun getAllBackgrounds(): List<BackgroundEntity>

    @Query("SELECT * FROM alignments ORDER BY id ASC")
    suspend fun getAllAlignments(): List<AlignmentEntity>

    @Query("SELECT * FROM languages ORDER BY name ASC")
    suspend fun getAllLanguages(): List<LanguageEntity>

    @Query("SELECT * FROM skills ORDER BY name ASC")
    suspend fun getAllSkills(): List<SkillEntity>

    @Query("SELECT * FROM proficiencies ORDER BY name ASC")
    suspend fun getAllProficiencies(): List<ProficiencyEntity>

    @Query("SELECT * FROM damage_types WHERE index_name = :index")
    suspend fun getDamageTypeByIndex(index: String): DamageTypeEntity?
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/dao/ReferenceDao.kt