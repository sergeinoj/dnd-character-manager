// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/ReferenceEntities.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "classes", indices = [Index(value = ["index_name"], unique = true)])
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "hit_die") val hitDie: Int?,
    @ColumnInfo(name = "proficiency_choices_json") val proficiencyChoicesJson: String?,
    @ColumnInfo(name = "proficiencies_json") val proficienciesJson: String?,
    @ColumnInfo(name = "saving_throws_json") val savingThrowsJson: String?,
    @ColumnInfo(name = "starting_equipment_json") val startingEquipmentJson: String?,
    @ColumnInfo(name = "starting_equipment_options_json") val startingEquipmentOptionsJson: String?,
    @ColumnInfo(name = "spellcasting_json") val spellcastingJson: String?,
    @ColumnInfo(name = "class_levels_url") val classLevelsUrl: String?,
    @ColumnInfo(name = "multi_classing_json") val multiClassingJson: String?,
    @ColumnInfo(name = "subclasses_json") val subclassesJson: String?,
    @ColumnInfo(name = "spells_url") val spellsUrl: String?
)

@Entity(tableName = "subclasses", indices = [Index(value = ["index_name"], unique = true)])
data class SubclassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    @ColumnInfo(name = "class_index") val classIndex: String,
    val name: String,
    @ColumnInfo(name = "subclass_flavor") val subclassFlavor: String?,
    val desc: String?,
    @ColumnInfo(name = "spells_json") val spellsJson: String?,
    @ColumnInfo(name = "subclass_levels_url") val subclassLevelsUrl: String?
)

@Entity(tableName = "progression", indices = [Index(value = ["entity_index"], unique = true)])
data class ProgressionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "entity_index") val entityIndex: String,
    val level: Int,
    @ColumnInfo(name = "class_index") val classIndex: String,
    @ColumnInfo(name = "subclass_index") val subclassIndex: String?,
    @ColumnInfo(name = "ability_score_bonuses") val abilityScoreBonuses: Int?,
    @ColumnInfo(name = "prof_bonus") val profBonus: Int?,
    @ColumnInfo(name = "feature_indices_json") val featureIndicesJson: String?,
    @ColumnInfo(name = "class_specific_json") val classSpecificJson: String?,
    @ColumnInfo(name = "subclass_specific_json") val subclassSpecificJson: String?,
    @ColumnInfo(name = "spellcasting_json") val spellcastingJson: String?
)

@Entity(tableName = "features", indices = [Index(value = ["index_name"], unique = true)])
data class FeatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    val level: Int?,
    @ColumnInfo(name = "class_index") val classIndex: String?,
    @ColumnInfo(name = "subclass_index") val subclassIndex: String?,
    @ColumnInfo(name = "race_index") val raceIndex: String?,
    @ColumnInfo(name = "subrace_index") val subraceIndex: String?,
    @ColumnInfo(name = "background_index") val backgroundIndex: String?,
    @ColumnInfo(name = "choices_json") val choicesJson: String?,
    @ColumnInfo(name = "spell_show_json") val spellShowJson: String?,
    @ColumnInfo(name = "change_rule") val changeRule: Int?,
    @ColumnInfo(name = "prerequisites_json") val prerequisitesJson: String?,
    @ColumnInfo(name = "reference_json") val referenceJson: String?
)

@Entity(tableName = "races", indices = [Index(value = ["index_name"], unique = true)])
data class RaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    val speed: Int?,
    @ColumnInfo(name = "ability_bonuses_json") val abilityBonusesJson: String?,
    val age: String?,
    val alignment: String?,
    val size: String?,
    @ColumnInfo(name = "size_desc") val sizeDescription: String?,
    @ColumnInfo(name = "languages_json") val languagesJson: String?,
    @ColumnInfo(name = "language_desc") val languageDesc: String?,
    @ColumnInfo(name = "traits_json") val traitsJson: String?,
    @ColumnInfo(name = "starting_proficiencies_json") val startingProficienciesJson: String?,
    @ColumnInfo(name = "starting_proficiency_options_json") val startingProficiencyOptionsJson: String?,
    @ColumnInfo(name = "language_options_json") val languageOptionsJson: String?,
    @ColumnInfo(name = "subraces_json") val subracesJson: String?
)

@Entity(tableName = "subraces", indices = [Index(value = ["index_name"], unique = true)])
data class SubraceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    @ColumnInfo(name = "race_index") val raceIndex: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "ability_bonuses_json") val abilityBonusesJson: String?,
    @ColumnInfo(name = "traits_json") val traitsJson: String?,
    @ColumnInfo(name = "starting_proficiencies_json") val startingProficienciesJson: String?,
    @ColumnInfo(name = "language_options_json") val languageOptionsJson: String?
)

@Entity(tableName = "backgrounds", indices = [Index(value = ["index_name"], unique = true)])
data class BackgroundEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "starting_proficiencies_json") val startingProficienciesJson: String?,
    @ColumnInfo(name = "language_options_json") val languageOptionsJson: String?,
    @ColumnInfo(name = "starting_equipment_json") val startingEquipmentJson: String?,
    @ColumnInfo(name = "feature_index") val featureIndex: String?,
    @ColumnInfo(name = "feature_name") val featureName: String?,
    @ColumnInfo(name = "feature_desc") val featureDesc: String?,
    @ColumnInfo(name = "personality_traits_json") val personalityTraitsJson: String?,
    @ColumnInfo(name = "ideals_json") val idealsJson: String?,
    @ColumnInfo(name = "bonds_json") val bondsJson: String?,
    @ColumnInfo(name = "flaws_json") val flawsJson: String?,
    @ColumnInfo(name = "starting_equipment_options_json") val startingEquipmentOptionsJson: String?
)

@Entity(tableName = "alignments", indices = [Index(value = ["index_name"], unique = true)])
data class AlignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val abbreviation: String?,
    val desc: String?
)

@Entity(tableName = "spells", indices = [Index(value = ["index_name"], unique = true)])
data class SpellEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val level: Int?,
    val school: String?,
    @ColumnInfo(name = "casting_time") val castingTime: String?,
    val range: String?,
    @ColumnInfo(name = "components_json") val componentsJson: String?,
    val material: String?,
    val duration: String?,
    val concentration: Int?,
    val ritual: Int?,
    val description: String?,
    @ColumnInfo(name = "higher_level") val higherLevel: String?,
    @ColumnInfo(name = "classes_json") val classesJson: String?,
    @ColumnInfo(name = "damage_json") val damageJson: String?,
    @ColumnInfo(name = "attack_type") val attackType: String?,
    @ColumnInfo(name = "dc_json") val dcJson: String?,
    @ColumnInfo(name = "area_of_effect_json") val areaOfEffectJson: String?,
    @ColumnInfo(name = "heal_at_slot_level_json") val healAtSlotLevelJson: String?,
    @ColumnInfo(name = "subclasses_json") val subclassesJson: String?
)

@Entity(tableName = "magic_schools", indices = [Index(value = ["index_name"], unique = true)])
data class MagicSchoolEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?
)

@Entity(tableName = "equipment", indices = [Index(value = ["index_name"], unique = true)])
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    @ColumnInfo(name = "cost_json") val costJson: String?,
    val weight: Double?,
    val description: String?,
    @ColumnInfo(name = "armor_class_json") val armorClassJson: String?,
    @ColumnInfo(name = "str_minimum") val strMinimum: Int?,
    @ColumnInfo(name = "stealth_disadvantage") val stealthDisadvantage: Int?,
    @ColumnInfo(name = "damage_json") val damageJson: String?,
    @ColumnInfo(name = "range_json") val rangeJson: String?,
    @ColumnInfo(name = "properties_json") val propertiesJson: String?,
    @ColumnInfo(name = "contents_json") val contentsJson: String?
)

@Entity(tableName = "weapons", indices = [Index(value = ["index_name"], unique = true)])
data class WeaponEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    @ColumnInfo(name = "label") val name: String,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    @ColumnInfo(name = "damage_dice") val damage: String?,
    @ColumnInfo(name = "damage_type") val damageType: String?,
    val cost: String?,
    val weight: Double?,
    @ColumnInfo(name = "properties_json") val propertiesJson: String?,
    val rarity: String?
)

@Entity(tableName = "armor", indices = [Index(value = ["index_name"], unique = true)])
data class ArmorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    @ColumnInfo(name = "ac_base") val acBase: Int?,
    @ColumnInfo(name = "dex_bonus") val dexBonus: Int?,
    @ColumnInfo(name = "max_bonus") val maxBonus: Int?,
    @ColumnInfo(name = "str_minimum") val strMinimum: Int?,
    @ColumnInfo(name = "stealth_disadvantage") val stealthDisadvantage: Int?,
    val cost: String?,
    val weight: Double?,
    val rarity: String?
)

@Entity(tableName = "magic_items", indices = [Index(value = ["index_name"], unique = true)])
data class MagicItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    val rarity: String?,
    val variant: Int?,
    @ColumnInfo(name = "variants_json") val variantsJson: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "reference_json") val referenceJson: String?
)

@Entity(tableName = "weapon_properties", indices = [Index(value = ["index_name"], unique = true)])
data class WeaponPropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String
)

@Entity(tableName = "proficiencies", indices = [Index(value = ["index_name"], unique = true)])
data class ProficiencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val type: String?,
    val name: String,
    @ColumnInfo(name = "reference_json") val referenceJson: String?,
    @ColumnInfo(name = "classes_json") val classesJson: String?,
    @ColumnInfo(name = "races_json") val racesJson: String?
)

@Entity(tableName = "skills", indices = [Index(value = ["index_name"], unique = true)])
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "ability_score_index") val abilityScoreIndex: String?
)

@Entity(tableName = "languages", indices = [Index(value = ["index_name"], unique = true)])
data class LanguageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val type: String?,
    val script: String?,
    val description: String?,
    @ColumnInfo(name = "typical_speakers_json") val typicalSpeakersJson: String?
)

@Entity(tableName = "damage_types", indices = [Index(value = ["index_name"], unique = true)])
data class DamageTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?
)

@Entity(tableName = "equipment_categories", indices = [Index(value = ["index_name"], unique = true)])
data class EquipmentCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String?
)

@Entity(tableName = "equipment_category_links", indices = [Index(value = ["category_index", "item_index"], unique = true)])
data class EquipmentCategoryLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "category_index") val categoryIndex: String,
    @ColumnInfo(name = "item_index") val itemIndex: String
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/ReferenceEntities.kt