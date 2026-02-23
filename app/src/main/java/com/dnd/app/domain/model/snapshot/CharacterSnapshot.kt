// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\CharacterSnapshot.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model.snapshot

import com.dnd.app.domain.model.MonsterRecord
import kotlinx.serialization.Serializable

@Serializable
data class CharacterSnapshot(
    val versionId: Long = 0,
    val global: GlobalInfo = GlobalInfo(),
    val stats: List<StatModel> = emptyList(),
    val statsMap: Map<String, StatModel> = emptyMap(),
    val skills: List<SkillModel> = emptyList(),
    val maxHp: Int = 0,
    val hitDice: String = "",
    val hitDiceCount: Int = 0,
    val finalArmorClass: Int = 10,
    val finalSpeed: Int = 30,
    val initiativeBonus: String = "+0",
    val totalWeight: Double = 0.0,
    val maxCarryWeight: Int = 0,
    val isEncumbered: Boolean = false,
    val proficiencyBonus: Int = 0,
    val features: List<FeatureDisplayModel> = emptyList(),
    val magic: MagicalRegistrySnapshot? = null,
    val resourcePools: List<ResourcePoolSnapshot> = emptyList(),
    val inventory: List<InventoryItemSnapshot> = emptyList(),
    val combatActions: List<CombatAction> = emptyList(),
    val notes: String = "",
    val familiar: MonsterRecord? = null,
    val transformationMonster: MonsterRecord? = null,
    val languages: List<String> = emptyList(),
    val proficiencies: Map<String, Int> = emptyMap(),
    val proficiencyLabels: Map<String, String> = emptyMap(),
    val isPurePactCaster: Boolean = false,
    val canWildShape: Boolean = false
)

@Serializable
data class ResourcePoolSnapshot(
    val id: String,
    val name: String,
    val max: Int,
    val resetRule: ResetRule = ResetRule.LONG_REST,
    val displayPriority: Int = 100,
    val uiColorHex: Long = 0xFF1976D2
)

@Serializable
enum class MagicSourceType { CLASS, SUBCLASS, RACE, ITEM }

@Serializable
enum class PreparationMode { KNOWN, PREPARED, NONE }

@Serializable
data class MagicalRegistrySnapshot(
    val sources: List<MagicSourceSnapshot> = emptyList(),
    val globalSlots: Map<Int, Int> = emptyMap(),
    val raceSlots: Map<Int, Int> = emptyMap(),
    val pactMagic: PactMagicSnapshot? = null,
    val hasHybridMagic: Boolean = false,
    val raceDualSpellIds: Set<String> = emptySet()
)

@Serializable
data class MagicSourceSnapshot(
    val sourceId: String,
    val displayName: String,
    val sourceType: MagicSourceType,
    val preparationMode: PreparationMode,
    val saveDc: Int,
    val attackBonus: Int,
    val castingStatCode: String,
    val maxPreparedSpells: Int = 0,
    val spells: List<SpellSnapshot> = emptyList(),
    val exclusiveResourcePoolId: String? = null,
    val resetRule: ResetRule = ResetRule.LONG_REST
)

@Serializable
data class FeatureDisplayModel(
    val id: String,
    val name: String,
    val description: String,
    val source: String,
    val hasChoices: Boolean = false,
    val level: Int? = null,
    val displayPriority: Int = 100,
    val poolId: String? = null,
    val resetRule: ResetRule = ResetRule.LONG_REST,
    val referenceJson: String? = null
)

@Serializable
data class PactMagicSnapshot(val maxSlots: Int, val slotLevel: Int)

@Serializable
enum class ActionType { WEAPON, CANTRIP, SPELL, ITEM, FEATURE_TOGGLE }

@Serializable
data class CombatAction(
    val uniqueId: String,
    val name: String,
    val hitBonus: String,
    val damageFormula: String,
    val damageType: String,
    val range: String = "5 фт.",
    val type: ActionType = ActionType.WEAPON,
    val isSpell: Boolean = false,
    val isConcentration: Boolean = false,
    val spellId: String? = null,
    val sourceUniqueId: String? = null,
    val quantity: Int? = null,
    val level: Int? = null,
    val isRitual: Boolean = false,
    val damageMap: Map<Int, String> = emptyMap(),
    val isTwoHanded: Boolean = false,
    val isHeavy: Boolean = false,
    val isVersatile: Boolean = false,
    val ammoType: String? = null,


    val currentCharges: Int? = null,
    val maxCharges: Int? = null,
    val resourceId: String? = null,


    val isToggle: Boolean = false,
    val isActive: Boolean = false,
    val effectId: String? = null,
    val parentEffectId: String? = null,


    val isBlocked: Boolean = false,


    val saveDcInfo: String? = null,
    val actionCostDescription: String? = null,


    val nestedActions: List<CombatAction> = emptyList(),
    val description: String? = null,
    val triggerDescriptions: List<String> = emptyList()
)

@Serializable
data class GlobalInfo(
    val name: String = "",
    val race: String = "",
    val subrace: String? = null,
    val raceDescription: String = "",
    val subraceDescription: String = "",
    val classTitle: String = "",
    val subclassName: String = "",
    val subclassDescription: String = "",
    val level: Int = 1,
    val alignment: String = "",
    val alignmentDescription: String = "",
    val gender: String = "",
    val personalityTrait: String = "",
    val ideal: String = "",
    val bond: String = "",
    val flaw: String = "",
    val appearance: String = "",
    val backstory: String = "",
    val backgroundName: String = "",
    val classes: List<ClassLevelSnapshot> = emptyList()
)

@Serializable
data class ClassLevelSnapshot(
    val className: String,
    val level: Int
)

@Serializable
data class StatModel(
    val code: String,
    val value: Int,
    val modifier: String,
    val saveModifier: String,
    val isProficientSave: Boolean
)

@Serializable
data class SkillModel(
    val code: String,
    val name: String,
    val modifier: String,
    val statCode: String,
    val profType: ProficiencyType = ProficiencyType.NONE
)

@Serializable
enum class ProficiencyType { NONE, PROFICIENCY, EXPERTISE }

@Serializable
data class SpellSnapshot(
    val uniqueId: String,
    val id: String,
    val name: String,
    val level: Int,
    val school: String,
    val time: String,
    val range: String,
    val components: String,
    val duration: String,
    val description: String,
    val isRitual: Boolean,
    val isConcentration: Boolean,
    val isAlwaysPrepared: Boolean,
    val attackType: String? = null,
    val damageDice: String? = null,
    val damageMap: Map<Int, String> = emptyMap(),
    val damageType: String? = null,
    val saveStat: String? = null,
    val isFreeCast: Boolean = false
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\CharacterSnapshot.kt
