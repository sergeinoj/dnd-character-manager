// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app/data/local/entity/ReferenceEntities.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "classes",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class ClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name")
    val indexName: String,

    val name: String,

    @ColumnInfo(name = "hit_die", defaultValue = "0")
    val hitDie: Int = 0,

    @ColumnInfo(name = "primary_stat")
    val primaryStat: String?,

    @ColumnInfo(name = "caster_type")
    val casterType: String?,

    @ColumnInfo(name = "caster_weight", defaultValue = "0.0")
    val casterWeight: Double = 0.0,

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

@Entity(
    tableName = "subclasses",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["class_index"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["index_name"], unique = true),
        Index(value = ["class_index"])
    ]
)
data class SubclassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name")
    val indexName: String,

    @ColumnInfo(name = "class_index")
    val classIndex: String,

    val name: String,

    @ColumnInfo(name = "sub_caster_weight", defaultValue = "0.0")
    val subCasterWeight: Double = 0.0,

    @ColumnInfo(name = "subclass_flavor") val subclassFlavor: String?,
    val desc: String?,
    @ColumnInfo(name = "spells_json") val spellsJson: String?,
    @ColumnInfo(name = "subclass_levels_url") val subclassLevelsUrl: String?
)

@Entity(
    tableName = "progression",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["class_index"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["entity_index"], unique = true),
        Index(value = ["class_index", "level"], name = "idx_prog_search")
    ]
)
data class ProgressionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "entity_index") val entityIndex: String,
    val level: Int,
    @ColumnInfo(name = "class_index") val classIndex: String,
    @ColumnInfo(name = "subclass_index") val subclassIndex: String?,

    @ColumnInfo(name = "max_charges", defaultValue = "0") val maxCharges: Int = 0,
    @ColumnInfo(name = "resource_name") val resourceName: String?,
    @ColumnInfo(name = "charge_reset_rule") val chargeResetRule: String?,

    @ColumnInfo(name = "max_charges_2", defaultValue = "0") val maxCharges2: Int = 0,
    @ColumnInfo(name = "resource_name_2") val resourceName2: String?,
    @ColumnInfo(name = "charge_reset_rule_2") val chargeResetRule2: String?,

    @ColumnInfo(name = "max_charges_3", defaultValue = "0") val maxCharges3: Int = 0,
    @ColumnInfo(name = "resource_name_3") val resourceName3: String?,
    @ColumnInfo(name = "charge_reset_rule_3") val chargeResetRule3: String?,

    @ColumnInfo(name = "die_count", defaultValue = "0") val dieCount: Int = 0,
    @ColumnInfo(name = "die_size", defaultValue = "0") val dieSize: Int = 0,
    @ColumnInfo(name = "scaling_bonus", defaultValue = "0") val scalingBonus: Int = 0,
    @ColumnInfo(name = "movement_bonus", defaultValue = "0") val movementBonus: Int = 0,

    @ColumnInfo(name = "caster_level_increment", defaultValue = "0.0")
    val casterLevelIncrement: Double = 0.0,

    @ColumnInfo(name = "is_pact_increment", defaultValue = "0")
    val isPactIncrement: Int = 0,

    @ColumnInfo(name = "prep_formula_type") val prepFormulaType: String?,

    @ColumnInfo(name = "ability_score_bonuses", defaultValue = "0") val abilityScoreBonuses: Int = 0,
    @ColumnInfo(name = "prof_bonus", defaultValue = "0") val profBonus: Int = 0,

    @ColumnInfo(name = "feature_indices_json") val featureIndicesJson: String?,
    @ColumnInfo(name = "class_specific_json") val classSpecificJson: String?,
    @ColumnInfo(name = "subclass_specific_json") val subclassSpecificJson: String?,
    @ColumnInfo(name = "spellcasting_json") val spellcastingJson: String?
)

@Entity(
    tableName = "features",
    indices = [
        Index(value = ["index_name"], unique = true),
        Index(value = ["class_index", "level"], name = "idx_feat_class"),
        Index(value = ["subclass_index"], name = "idx_feat_subclass")
    ]
)
data class FeatureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    val level: Int?,
    @ColumnInfo(name = "class_index") val classIndex: String?,
    @ColumnInfo(name = "subclass_index") val subclassIndex: String?,
    @ColumnInfo(name = "race_index") val raceIndex: String?,
    @ColumnInfo(name = "subrace_index") val subraceIndex: String?,
    @ColumnInfo(name = "background_index") val backgroundIndex: String?,

    @ColumnInfo(name = "max_charges", defaultValue = "0") val maxCharges: Int = 0,
    @ColumnInfo(name = "charge_reset_rule") val chargeResetRule: String?,

    @ColumnInfo(name = "choices_json") val choicesJson: String?,
    @ColumnInfo(name = "spell_show_json") val spellShowJson: String?,

    @ColumnInfo(name = "change_rule", defaultValue = "0") val changeRule: Int = 0,

    @ColumnInfo(name = "prerequisites_json") val prerequisitesJson: String?,
    @ColumnInfo(name = "reference_json") val referenceJson: String?,
    @ColumnInfo(name = "ui_group") val uiGroup: String?
) {
    val isStartingFeature: Boolean
        get() = indexName.endsWith("-skills")
}

@Entity(tableName = "conditions")
data class ConditionEntity(
    @PrimaryKey
    @ColumnInfo(name = "index_name")
    val indexName: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "ui_color_hex")
    val uiColorHex: String?,
    @ColumnInfo(name = "mechanics_json")
    val mechanicsJson: String
)

@Entity(
    tableName = "races",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class RaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,

    @ColumnInfo(defaultValue = "30")
    val speed: Int = 30,

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

@Entity(
    tableName = "subraces",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class SubraceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    @ColumnInfo(name = "race_index") val raceIndex: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "ability_bonuses_json") val abilityBonusesJson: String?,
    @ColumnInfo(name = "traits_json") val traitsJson: String?,
    @ColumnInfo(name = "starting_proficiencies_json") val startingProficienciesJson: String?,
    @ColumnInfo(name = "language_options_json") val languageOptionsJson: String?
)

@Entity(
    tableName = "backgrounds",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class BackgroundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "starting_proficiencies_json") val startingProficienciesJson: String?,
    @ColumnInfo(name = "language_options_json") val languageOptionsJson: String?,
    @ColumnInfo(name = "starting_equipment_json") val startingEquipmentJson: String?,
    @ColumnInfo(name = "starting_equipment_options_json") val startingEquipmentOptionsJson: String?,

    @ColumnInfo(name = "starting_gold", defaultValue = "0")
    val startingGold: Int = 0,

    @ColumnInfo(name = "feature_index") val featureIndex: String?,
    @ColumnInfo(name = "feature_name") val featureName: String?,
    @ColumnInfo(name = "feature_desc") val featureDesc: String?,
    @ColumnInfo(name = "feature_indices_json") val featureIndicesJson: String?,
    @ColumnInfo(name = "personality_traits_json") val personalityTraitsJson: String?,
    @ColumnInfo(name = "ideals_json") val idealsJson: String?,
    @ColumnInfo(name = "bonds_json") val bondsJson: String?,
    @ColumnInfo(name = "flaws_json") val flawsJson: String?
)

@Entity(
    tableName = "alignments",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class AlignmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val abbreviation: String?,
    val desc: String?
)

@Entity(
    tableName = "spells",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class SpellEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,

    @ColumnInfo(defaultValue = "0")
    val level: Int = 0,

    val school: String?,
    @ColumnInfo(name = "casting_time") val castingTime: String?,
    val range: String?,
    @ColumnInfo(name = "components_json") val componentsJson: String?,
    val material: String?,
    val duration: String?,

    @ColumnInfo(defaultValue = "0")
    val concentration: Int = 0,

    @ColumnInfo(defaultValue = "0")
    val ritual: Int = 0,

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

@Entity(
    tableName = "magic_schools",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class MagicSchoolEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?
)

@Entity(
    tableName = "equipment",
    indices = [
        Index(value = ["index_name"], unique = true),
        Index(value = ["cost_cp"], name = "idx_equipment_cost")
    ]
)
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,

    @ColumnInfo(defaultValue = "0.0")
    val weight: Double = 0.0,

    val description: String?,
    @ColumnInfo(name = "armor_class_json") val armorClassJson: String?,

    @ColumnInfo(name = "str_minimum", defaultValue = "0")
    val strMinimum: Int = 0,

    @ColumnInfo(name = "stealth_disadvantage", defaultValue = "0")
    val stealthDisadvantage: Int = 0,

    @ColumnInfo(name = "damage_json") val damageJson: String?,
    @ColumnInfo(name = "range_json") val rangeJson: String?,
    @ColumnInfo(name = "properties_json") val propertiesJson: String?,
    @ColumnInfo(name = "contents_json") val contentsJson: String?,
    @ColumnInfo(name = "cost_json") val costJson: String?,

    @ColumnInfo(name = "cost_cp", defaultValue = "0")
    val costCp: Int = 0
)

@Entity(
    tableName = "weapons",
    indices = [
        Index(value = ["index_name"], unique = true),
        Index(value = ["cost_cp"], name = "idx_weapons_cost")
    ]
)
data class WeaponEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    @ColumnInfo(name = "damage_dice") val damage: String?,
    @ColumnInfo(name = "damage_type") val damageType: String?,

    @ColumnInfo(defaultValue = "0.0")
    val weight: Double = 0.0,

    @ColumnInfo(name = "properties_json") val propertiesJson: String?,
    @ColumnInfo(name = "range_json") val rangeJson: String?,
    val rarity: String?,
    @ColumnInfo(name = "cost_json") val costJson: String?,

    @ColumnInfo(name = "cost_cp", defaultValue = "0")
    val costCp: Int = 0,

    @ColumnInfo(name = "description")
    val description: String? = null
)

@Entity(
    tableName = "armor",
    indices = [
        Index(value = ["index_name"], unique = true),
        Index(value = ["cost_cp"], name = "idx_armor_cost")
    ]
)
data class ArmorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,

    @ColumnInfo(name = "ac_base", defaultValue = "0")
    val acBase: Int = 0,

    @ColumnInfo(name = "dex_bonus", defaultValue = "0")
    val dexBonus: Int = 0,

    @ColumnInfo(name = "max_bonus", defaultValue = "0")
    val maxBonus: Int = 0,

    @ColumnInfo(name = "str_minimum", defaultValue = "0")
    val strMinimum: Int = 0,

    @ColumnInfo(name = "stealth_disadvantage", defaultValue = "0")
    val stealthDisadvantage: Int = 0,

    @ColumnInfo(defaultValue = "0.0")
    val weight: Double = 0.0,

    val rarity: String?,
    @ColumnInfo(name = "cost_json") val costJson: String?,

    @ColumnInfo(name = "cost_cp", defaultValue = "0")
    val costCp: Int = 0,

    @ColumnInfo(name = "description")
    val description: String? = null
)

@Entity(
    tableName = "magic_items",
    indices = [
        Index(value = ["index_name"], unique = true),
        Index(value = ["cost_cp"], name = "idx_magic_items_cost"),
        Index(value = ["category_index"], name = "idx_magic_items_category"),
        Index(value = ["rarity"], name = "idx_magic_items_rarity")
    ]
)
data class MagicItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    val rarity: String?,

    @ColumnInfo(defaultValue = "0.0")
    val weight: Double = 0.0,

    @ColumnInfo(name = "cost_cp", defaultValue = "0")
    val costCp: Int = 0,

    @ColumnInfo(name = "base_item_index") val baseItemIndex: String?,

    @ColumnInfo(name = "change_rule", defaultValue = "0")
    val changeRule: Int = 0,

    @ColumnInfo(name = "requires_attunement", defaultValue = "0")
    val requiresAttunement: Int = 0,

    @ColumnInfo(name = "max_charges", defaultValue = "0")
    val maxCharges: Int = 0,

    @ColumnInfo(name = "charge_reset_rule") val chargeResetRule: String?,

    @ColumnInfo(name = "bonus_ac", defaultValue = "0")
    val bonusAc: Int = 0,

    @ColumnInfo(name = "bonus_attack", defaultValue = "0")
    val bonusAttack: Int = 0,

    @ColumnInfo(name = "bonus_damage", defaultValue = "0")
    val bonusDamage: Int = 0,

    @ColumnInfo(name = "bonus_save_dc", defaultValue = "0")
    val bonusSaveDc: Int = 0,

    @ColumnInfo(name = "granted_spells_json") val grantedSpellsJson: String?,
    @ColumnInfo(name = "stat_overrides_json") val statOverridesJson: String?,
    @ColumnInfo(name = "mechanics_json") val mechanicsJson: String?,


    @ColumnInfo(name = "variant")
    val variant: Int? = null,

    @ColumnInfo(name = "variants_json") val variantsJson: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "reference_json") val referenceJson: String?,
    @ColumnInfo(name = "cost_json") val costJson: String?
)

@Entity(
    tableName = "weapon_properties",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class WeaponPropertyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String
)

@Entity(
    tableName = "proficiencies",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class ProficiencyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val type: String?,
    val name: String,
    @ColumnInfo(name = "reference_json") val referenceJson: String?,
    @ColumnInfo(name = "classes_json") val classesJson: String?,
    @ColumnInfo(name = "races_json") val racesJson: String?
)

@Entity(
    tableName = "skills",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class SkillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "ability_score_index") val abilityScoreIndex: String?
)

@Entity(
    tableName = "languages",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class LanguageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val type: String?,
    val script: String?,
    val description: String?,
    @ColumnInfo(name = "typical_speakers_json") val typicalSpeakersJson: String?
)

@Entity(
    tableName = "damage_types",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class DamageTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?
)

@Entity(
    tableName = "equipment_categories",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class EquipmentCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "parent_index") val parentIndex: String?
)

@Entity(
    tableName = "equipment_category_links",
    indices = [Index(value = ["category_index", "item_index"], unique = true)]
)
data class EquipmentCategoryLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "category_index") val categoryIndex: String,
    @ColumnInfo(name = "item_index") val itemIndex: String
)

@Entity(
    tableName = "monsters",
    indices = [Index(value = ["index_name"], unique = true)]
)
data class MonsterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "name_ru") val nameRu: String?,
    val size: String?,
    val type: String?,
    val subtype: String?,
    val alignment: String?,
    @ColumnInfo(name = "challenge_rating") val challengeRating: Double?,
    @ColumnInfo(name = "proficiency_bonus") val proficiencyBonus: Int?,
    val xp: Int?,
    @ColumnInfo(name = "armor_class_json") val armorClassJson: String?,
    @ColumnInfo(name = "hit_points") val hitPoints: Int?,
    @ColumnInfo(name = "hit_points_roll") val hitPointsRoll: String?,
    @ColumnInfo(name = "hit_dice") val hitDice: String?,
    @ColumnInfo(name = "hit_die_count") val hitDieCount: Int?,
    @ColumnInfo(name = "hit_die_size") val hitDieSize: Int?,
    @ColumnInfo(name = "hit_die_bonus") val hitDieBonus: Int?,
    @ColumnInfo(name = "speed_json") val speedJson: String?,
    @ColumnInfo(name = "stats_json") val statsJson: String?,
    @ColumnInfo(name = "condition_immunities_json") val conditionImmunitiesJson: String?,
    @ColumnInfo(name = "senses_json") val sensesJson: String?,
    val languages: String?,
    val description: String?,
    @ColumnInfo(name = "description_ru") val descriptionRu: String?,
    @ColumnInfo(name = "desc_ru") val descRu: String?
)

@Entity(
    tableName = "monster_actions",
    foreignKeys = [
        ForeignKey(
            entity = MonsterEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["monster_index"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["monster_index"])]
)
data class MonsterActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "monster_index") val monsterIndex: String,
    @ColumnInfo(name = "action_index") val actionIndex: String?,
    val name: String,
    val desc: String?,
    @ColumnInfo(name = "attack_bonus") val attackBonus: Int?,
    @ColumnInfo(name = "attack_json") val attackJson: String?,
    @ColumnInfo(name = "damage_json") val damageJson: String?,
    @ColumnInfo(name = "dc_json") val dcJson: String?,
    @ColumnInfo(name = "usage_json") val usageJson: String?,
    @ColumnInfo(name = "options_json") val optionsJson: String?,
    val type: String?
)

@Entity(
    tableName = "monster_attack_patterns",
    foreignKeys = [
        ForeignKey(
            entity = MonsterEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["monster_index"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["monster_index"])]
)
data class MonsterAttackPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    @ColumnInfo(name = "monster_index") val monsterIndex: String,
    @ColumnInfo(name = "pattern_slug") val patternSlug: String,
    @ColumnInfo(name = "logic_operator") val logicOperator: String,
    val description: String?
)

@Entity(
    tableName = "monster_attack_pattern_entries",
    foreignKeys = [
        ForeignKey(
            entity = MonsterAttackPatternEntity::class,
            parentColumns = ["id"],
            childColumns = ["pattern_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pattern_id"])]
)
data class MonsterAttackPatternEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    @ColumnInfo(name = "pattern_id") val patternId: Int,
    @ColumnInfo(name = "entry_type") val entryType: String,
    @ColumnInfo(name = "entry_index") val entryIndex: String,
    @ColumnInfo(defaultValue = "1") val count: Int? = 1
)

@Entity(
    tableName = "monster_action_effects",
    foreignKeys = [
        ForeignKey(
            entity = MonsterEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["monster_index"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["monster_index", "action_index"], name = "idx_effects_lookup")]
)
data class MonsterActionEffectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    @ColumnInfo(name = "monster_index") val monsterIndex: String,
    @ColumnInfo(name = "action_index") val actionIndex: String,
    @ColumnInfo(name = "trigger_event") val triggerEvent: String,
    @ColumnInfo(name = "trigger_condition") val triggerCondition: String?,
    @ColumnInfo(name = "effect_type") val effectType: String,
    @ColumnInfo(defaultValue = "TARGET") val target: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "save_dc_override") val saveDcOverride: Int?,
    @ColumnInfo(name = "save_stat") val saveStat: String?
)

@Entity(
    tableName = "monster_special_abilities",
    foreignKeys = [
        ForeignKey(
            entity = MonsterEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["monster_index"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["monster_index"])]
)
data class MonsterSpecialAbilityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "monster_index") val monsterIndex: String,
    val name: String,
    val desc: String?,
    @ColumnInfo(name = "dc_json") val dcJson: String?,
    @ColumnInfo(name = "usage_json") val usageJson: String?,
    @ColumnInfo(name = "spellcasting_json") val spellcastingJson: String?
)

@Entity(
    tableName = "monster_legendary_actions",
    foreignKeys = [
        ForeignKey(
            entity = MonsterEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["monster_index"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["monster_index"])]
)
data class MonsterLegendaryActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "monster_index") val monsterIndex: String,
    val name: String,
    val desc: String?,
    @ColumnInfo(name = "attack_json") val attackJson: String?,
    @ColumnInfo(name = "damage_json") val damageJson: String?,
    @ColumnInfo(name = "dc_json") val dcJson: String?,
    @ColumnInfo(name = "usage_json") val usageJson: String?,
    val cost: Int?
)

@Entity(
    tableName = "monster_reactions",
    foreignKeys = [
        ForeignKey(
            entity = MonsterEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["monster_index"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["monster_index"])]
)
data class MonsterReactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "monster_index") val monsterIndex: String,
    val name: String,
    val desc: String?,
    @ColumnInfo(name = "attack_json") val attackJson: String?,
    @ColumnInfo(name = "damage_json") val damageJson: String?,
    @ColumnInfo(name = "dc_json") val dcJson: String?,
    @ColumnInfo(name = "usage_json") val usageJson: String?
)

@Entity(
    tableName = "monster_proficiencies",
    foreignKeys = [
        ForeignKey(
            entity = MonsterEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["monster_index"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["monster_index"])]
)
data class MonsterProficiencyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "monster_index") val monsterIndex: String,
    @ColumnInfo(name = "proficiency_index") val proficiencyIndex: String?,
    val value: Int?
)

@Entity(
    tableName = "monster_damage_mods",
    foreignKeys = [
        ForeignKey(
            entity = MonsterEntity::class,
            parentColumns = ["index_name"],
            childColumns = ["monster_index"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["monster_index"])]
)
data class MonsterDamageModEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "monster_index") val monsterIndex: String,
    val kind: String?,
    val value: String?
)

// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app/data/local/entity/ReferenceEntities.kt
