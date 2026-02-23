// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\dao\ReferenceDao.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.dnd.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.ArrayDeque

@Dao
abstract class ReferenceDao {
    @Query("SELECT * FROM classes ORDER BY name ASC")
    abstract suspend fun getAllClasses(): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE index_name = :indexName")
    abstract suspend fun getClassByIndex(indexName: String): ClassEntity?

    @Query("SELECT * FROM subclasses WHERE class_index = :classIndex ORDER BY name ASC")
    abstract suspend fun getSubclassesForClass(classIndex: String): List<SubclassEntity>

    @Query("SELECT * FROM subclasses WHERE index_name = :indexName")
    abstract suspend fun getSubclassByIndex(indexName: String): SubclassEntity?

    @Query("SELECT * FROM progression WHERE class_index = :classIndex AND level = :level")
    abstract suspend fun getProgressionForLevel(classIndex: String, level: Int): List<ProgressionEntity>

    @Query("SELECT * FROM progression WHERE class_index = :classIndex AND level IN (:levels)")
    abstract suspend fun getProgressionForLevels(classIndex: String, levels: List<Int>): List<ProgressionEntity>


    @Transaction
    @Query("""
        SELECT * FROM progression
        WHERE class_index = :classIndex AND level = :level
        AND (subclass_index IS NULL OR subclass_index = :subclassIndex)
        ORDER BY subclass_index DESC LIMIT 1
    """)
    abstract suspend fun getProgressionForLevelAndSubclass(classIndex: String, level: Int, subclassIndex: String?): List<ProgressionEntity>

    @Query("SELECT * FROM races ORDER BY name ASC")
    abstract suspend fun getAllRaces(): List<RaceEntity>

    @Query("SELECT * FROM subraces WHERE race_index = :raceIndex ORDER BY name ASC")
    abstract suspend fun getSubracesForRace(raceIndex: String): List<SubraceEntity>

    @Query("SELECT * FROM subraces WHERE index_name = :index")
    abstract suspend fun getSubraceByIndex(index: String): SubraceEntity?

    @Query("""
        SELECT * FROM features
        WHERE (race_index = :raceIdx AND subrace_index IS NULL)
        OR (subrace_index = :subIdx)
        OR (class_index = :classIdx AND subclass_index IS NULL)
        OR (subclass_index = :subclassIdx)
        OR (background_index = :bgIdx)
        ORDER BY level ASC, id ASC
    """)
    abstract suspend fun findFeaturesByContext(
        raceIdx: String? = null,
        subIdx: String? = null,
        classIdx: String? = null,
        subclassIdx: String? = null,
        bgIdx: String? = null
    ): List<FeatureEntity>

    @Query("SELECT * FROM features WHERE index_name IN (:indexes) ORDER BY level ASC")
    abstract suspend fun getFeaturesByIndexes(indexes: List<String>): List<FeatureEntity>

    @Query("SELECT * FROM features WHERE index_name = :indexName")
    abstract suspend fun getFeatureByIndex(indexName: String): FeatureEntity?

    @Query("SELECT * FROM features WHERE id = :id")
    abstract suspend fun getFeatureById(id: Int): FeatureEntity?

    @Query("SELECT * FROM features WHERE ui_group = 'FEAT' ORDER BY name ASC")
    abstract suspend fun getAllFeats(): List<FeatureEntity>

    @Query("SELECT * FROM features WHERE index_name LIKE :pattern")
    abstract suspend fun getFeaturesLike(pattern: String): List<FeatureEntity>

    @Query("SELECT * FROM conditions WHERE index_name NOT LIKE 'exhaustion_%' ORDER BY name ASC")
    abstract suspend fun getAllConditions(): List<ConditionEntity>

    @Query("SELECT * FROM conditions WHERE index_name = :indexName")
    abstract suspend fun getConditionByIndex(indexName: String): ConditionEntity?

    @Query("SELECT * FROM conditions WHERE index_name IN (:indexes)")
    abstract suspend fun getConditionsByIndexes(indexes: List<String>): List<ConditionEntity>

    @Query("SELECT * FROM spells WHERE index_name IN (:indexes)")
    abstract suspend fun getSpellsByIndexes(indexes: List<String>): List<SpellEntity>

    @Query("SELECT * FROM spells WHERE id IN (:ids)")
    abstract suspend fun getSpellsByIds(ids: List<Int>): List<SpellEntity>

    @Query("SELECT * FROM spells ORDER BY name ASC")
    abstract fun getAllSpells(): Flow<List<SpellEntity>>

    @Query("SELECT * FROM spells ORDER BY name ASC")
    abstract suspend fun getAllSpellsSuspend(): List<SpellEntity>

    @Query("SELECT * FROM spells WHERE level = :level ORDER BY name ASC")
    abstract suspend fun getSpellsByLevel(level: Int): List<SpellEntity>

    @Query("SELECT * FROM weapons ORDER BY name ASC")
    abstract fun getAllWeapons(): Flow<List<WeaponEntity>>

    @Query("SELECT * FROM weapons ORDER BY name ASC")
    abstract suspend fun getAllWeaponsSuspend(): List<WeaponEntity>

    @Query("SELECT * FROM weapons WHERE id IN (:ids)")
    abstract suspend fun getWeaponsByIds(ids: List<Int>): List<WeaponEntity>

    @Query("SELECT * FROM weapons WHERE index_name IN (:indexes) ORDER BY name ASC")
    abstract suspend fun getWeaponsByIndexes(indexes: List<String>): List<WeaponEntity>

    @Query("SELECT * FROM weapons WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    abstract suspend fun searchWeapons(query: String): List<WeaponEntity>

    @Query("SELECT * FROM armor ORDER BY name ASC")
    abstract fun getAllArmor(): Flow<List<ArmorEntity>>

    @Query("SELECT * FROM armor ORDER BY name ASC")
    abstract suspend fun getAllArmorSuspend(): List<ArmorEntity>

    @Query("SELECT * FROM armor WHERE index_name IN (:indexes) ORDER BY name ASC")
    abstract suspend fun getArmorByIndexes(indexes: List<String>): List<ArmorEntity>

    @Query("SELECT * FROM armor WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    abstract suspend fun searchArmor(query: String): List<ArmorEntity>

    @Query("""
        SELECT * FROM equipment WHERE category_index = :catIdx
        UNION
        SELECT * FROM equipment WHERE index_name IN (SELECT item_index FROM equipment_category_links WHERE category_index = :catIdx)
    """)
    abstract suspend fun getEquipmentByCategory(catIdx: String): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE index_name IN (:indexes) ORDER BY name ASC")
    abstract suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    abstract suspend fun searchEquipment(query: String): List<EquipmentEntity>

    @Query("SELECT id FROM equipment WHERE index_name IN (:idxNames)")
    abstract suspend fun getEquipmentIdsByIdxNames(idxNames: List<String>): List<Int>

    @Query("SELECT * FROM magic_items WHERE index_name IN (:indexes) ORDER BY name ASC")
    abstract suspend fun getMagicItemsByIndexes(indexes: List<String>): List<MagicItemEntity>

    @Query("SELECT * FROM magic_items WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    abstract suspend fun searchMagicItems(query: String): List<MagicItemEntity>

    @Query("SELECT * FROM equipment_categories WHERE index_name = :index")
    abstract suspend fun getEquipmentCategoryByIndex(index: String): EquipmentCategoryEntity?

    @Query("SELECT * FROM weapons WHERE category_index = :categoryIndex ORDER BY name ASC")
    abstract suspend fun getWeaponsByCategory(categoryIndex: String): List<WeaponEntity>

    @Query("SELECT * FROM armor WHERE category_index = :categoryIndex ORDER BY name ASC")
    abstract suspend fun getArmorByCategory(categoryIndex: String): List<ArmorEntity>

    @Query("SELECT item_index FROM equipment_category_links WHERE category_index = :categoryIndex")
    abstract suspend fun getLinksForCategory(categoryIndex: String): List<String>

    @Query("SELECT * FROM equipment_categories ORDER BY name ASC")
    abstract suspend fun getAllEquipmentCategories(): List<EquipmentCategoryEntity>

    @Query("SELECT * FROM equipment_categories WHERE parent_index IS NULL ORDER BY name ASC")
    abstract suspend fun getRootEquipmentCategories(): List<EquipmentCategoryEntity>

    @Query("SELECT * FROM equipment_categories WHERE parent_index = :parentIndex ORDER BY name ASC")
    abstract suspend fun getChildEquipmentCategories(parentIndex: String): List<EquipmentCategoryEntity>

    @Query("SELECT * FROM backgrounds ORDER BY name ASC")
    abstract suspend fun getAllBackgrounds(): List<BackgroundEntity>

    @Query("SELECT * FROM backgrounds WHERE index_name = :index")
    abstract suspend fun getBackgroundByIndex(index: String): BackgroundEntity?

    @Query("SELECT * FROM alignments ORDER BY id ASC")
    abstract suspend fun getAllAlignments(): List<AlignmentEntity>

    @Query("SELECT * FROM languages ORDER BY name ASC")
    abstract suspend fun getAllLanguages(): List<LanguageEntity>

    @Query("SELECT * FROM skills ORDER BY name ASC")
    abstract suspend fun getAllSkills(): List<SkillEntity>

    @Query("SELECT * FROM proficiencies ORDER BY name ASC")
    abstract suspend fun getAllProficiencies(): List<ProficiencyEntity>

    @Query("SELECT * FROM damage_types WHERE index_name = :index")
    abstract suspend fun getDamageTypeByIndex(index: String): DamageTypeEntity?

    @Query("SELECT type FROM proficiencies WHERE index_name = :index")
    abstract suspend fun getProficiencyType(index: String): String?

    @Query("SELECT category_index FROM equipment WHERE index_name = :index")
    abstract suspend fun getEquipmentCategoryFor(index: String): String?

    @Query("SELECT * FROM monsters WHERE index_name = :index")
    abstract suspend fun getMonsterByIndex(index: String): MonsterEntity?

    @Query("SELECT * FROM monsters ORDER BY challenge_rating ASC, name ASC")
    abstract suspend fun getAllMonsters(): List<MonsterEntity>

    @Query("SELECT * FROM monster_actions WHERE monster_index = :index")
    abstract suspend fun getMonsterActions(index: String): List<MonsterActionEntity>

    @Query("SELECT * FROM monster_special_abilities WHERE monster_index = :index")
    abstract suspend fun getMonsterSpecialAbilities(index: String): List<MonsterSpecialAbilityEntity>

    @Query("SELECT * FROM monster_legendary_actions WHERE monster_index = :index")
    abstract suspend fun getMonsterLegendaryActions(index: String): List<MonsterLegendaryActionEntity>

    @Query("SELECT * FROM monster_reactions WHERE monster_index = :index")
    abstract suspend fun getMonsterReactions(index: String): List<MonsterReactionEntity>

    @Query("SELECT * FROM monster_proficiencies WHERE monster_index = :index")
    abstract suspend fun getMonsterProficiencies(index: String): List<MonsterProficiencyEntity>

    @Query("SELECT * FROM monster_damage_mods WHERE monster_index = :index")
    abstract suspend fun getMonsterDamageMods(index: String): List<MonsterDamageModEntity>

    @Query("SELECT * FROM monster_attack_patterns WHERE monster_index = :monsterIndex")
    abstract suspend fun getMonsterAttackPatterns(monsterIndex: String): List<MonsterAttackPatternEntity>

    @Query("SELECT * FROM monster_attack_pattern_entries WHERE pattern_id = :patternId ORDER BY id ASC")
    abstract suspend fun getAttackPatternEntries(patternId: Int): List<MonsterAttackPatternEntryEntity>

    @Query("""
        SELECT * FROM monster_action_effects
        WHERE monster_index = :monsterIndex AND action_index = :actionIndex
        ORDER BY id ASC
    """)
    abstract suspend fun getMonsterActionEffects(monsterIndex: String, actionIndex: String): List<MonsterActionEffectEntity>

    suspend fun getAllItemIndexesByCategoryRecursive(categoryIndex: String): List<String> {
        val allCategoryIndexes = mutableSetOf(categoryIndex)
        allCategoryIndexes.addAll(getAllChildCategoryIndexesRecursive(categoryIndex))
        return allCategoryIndexes.flatMap { getLinksForCategory(it) }.distinct()
    }

    private suspend fun getAllChildCategoryIndexesRecursive(parentIndex: String): Set<String> {
        val result = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(parentIndex)

        while (queue.isNotEmpty()) {
            val currentParent = queue.removeFirst()
            val children = getChildEquipmentCategories(currentParent)
            for (child in children) {
                if (result.add(child.indexName)) {
                    queue.add(child.indexName)
                }
            }
        }
        return result
    }

}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\dao\ReferenceDao.kt
