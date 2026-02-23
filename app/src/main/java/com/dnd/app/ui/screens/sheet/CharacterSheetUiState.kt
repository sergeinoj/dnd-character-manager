// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\CharacterSheetUiState.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet

import androidx.compose.runtime.Immutable
import com.dnd.app.domain.model.MonsterRecord
import com.dnd.app.domain.model.Money
import com.dnd.app.domain.model.ShopItem
import com.dnd.app.domain.model.snapshot.CombatAction
import com.dnd.app.domain.model.snapshot.DeathSavesState
import com.dnd.app.domain.model.snapshot.ResourcePoolSnapshot
import com.dnd.app.domain.model.snapshot.SkillModel
import com.dnd.app.domain.model.snapshot.StatModel
import com.dnd.app.ui.screens.sheet.magic.MagicUiState

@Immutable
data class CharacterSheetUiState(
    val isLoading: Boolean = true,
    val fatalError: String? = null,
    val interactionError: String? = null,
    val isBusy: Boolean = false,
    val isShopOpen: Boolean = false,
    val merchantState: MerchantUiState = MerchantUiState(),
    val pendingActions: Set<String> = emptySet(),
    val data: CharacterSheetUiData? = null,
    val activeTacticalAction: CombatAction? = null,
    val lootSearchQuery: String = "",
    val lootSearchResults: List<ShopItem> = emptyList(),
    val concentrationDialogMessage: String? = null,
    val showHitDiceDialog: Boolean = false,
    val hitDicePoolViews: List<HitDicePoolView> = emptyList()
)

@Immutable
data class CharacterSheetUiData(
    val base: BaseUiData,
    val magic: MagicUiState
)

@Immutable
data class BaseUiData(
    val name: String,
    val classTitle: String,
    val level: Int,
    val proficiencyBonus: String,
    val passivePerception: String,
    val hpCurrent: Int,
    val hpTemp: Int,
    val hpMax: Int,
    val transformationHp: Int = 0,
    val deathSaves: DeathSavesState = DeathSavesState(),
    val initiative: String,
    val displayArmorClass: Int,
    val displaySpeed: String,
    val coins: Money,
    val formattedTotalWeight: String,
    val maxCarryWeight: Int,
    val isEncumbered: Boolean,
    val weapons: List<DisplayInventoryItem>,
    val armorAndShields: List<DisplayInventoryItem>,
    val gear: List<DisplayInventoryItem>,
    val filteredCombatActions: List<CombatAction>,
    val stats: List<StatModel>,
    val skillsByStat: Map<String, List<SkillModel>>,
    val bioFields: List<Pair<String, String>>,
    val manualBioFields: List<Pair<String, String>> = emptyList(),
    val autoLore: List<AutoLoreSection> = emptyList(),
    val notes: String,
    val systemLogs: List<String>,
    val defenseResistances: List<DefenseTrait> = emptyList(),
    val defenseImmunities: List<DefenseTrait> = emptyList(),
    val heroDefenseResistances: List<DefenseTrait> = emptyList(),
    val heroDefenseImmunities: List<DefenseTrait> = emptyList(),
    val beastDefenseResistances: List<DefenseTrait> = emptyList(),
    val beastDefenseImmunities: List<DefenseTrait> = emptyList(),
    val senses: List<SenseTrait> = emptyList(),
    val languages: List<String> = emptyList(),
    val toolProficiencies: List<String> = emptyList(),
    val proficiencies: Map<String, Int> = emptyMap(),
    val proficiencyLabels: Map<String, String> = emptyMap(),

    val classResources: List<ResourcePoolSnapshot> = emptyList(),
    val resourceCharges: Map<String, Int> = emptyMap(),
    val canWildShape: Boolean = false,
    val isTransformed: Boolean = false,
    val transformationName: String? = null,

    val activeEffects: Set<String> = emptySet(),
    val activeConditions: Set<String> = emptySet(),
    val exhaustionLevel: Int = 0,
    val isConcentrating: Boolean = false,
    val concentrationSpellId: String? = null,
    val availableConditions: List<ConditionUiModel> = emptyList(),

    val familiar: MonsterRecord? = null,
    val transformedMonster: MonsterRecord? = null,

    val hitDiceFormula: String = "",
    val hitDicePools: List<HitDicePool> = emptyList(),
    val totalHitDice: Int = 0,
    val remainingHitDice: Int = 0
)

@Immutable
data class AutoLoreSection(
    val id: String,
    val title: String,
    val blocks: List<AutoLoreBlock> = emptyList()
)

@Immutable
data class AutoLoreBlock(
    val id: String,
    val title: String,
    val text: String
)

@Immutable
data class DisplayInventoryItem(
    val uniqueId: String,
    val name: String,
    val description: String,
    val formattedWeight: String,
    val quantity: Int,
    val canChangeQuantity: Boolean,
    val canBeEquipped: Boolean,
    val isEquipped: Boolean,
    val requiresAttunement: Boolean,
    val isAttuned: Boolean,
    val isSellable: Boolean,
    val sellPrice: Money,
    val rarity: String? = null,
    val isPack: Boolean = false,
    val containerId: String? = null,
    val isShield: Boolean = false
)

@Immutable
data class HitDicePool(
    val dieType: Int,
    val count: Int
)

@Immutable
data class HitDicePoolView(
    val dieType: Int,
    val total: Int,
    val remaining: Int
)

@Immutable
data class DefenseTrait(
    val title: String,
    val detail: String? = null
)

@Immutable
data class SenseTrait(
    val title: String,
    val detail: String? = null
)

@Immutable
data class ConditionUiModel(
    val indexName: String,
    val name: String,
    val uiColorHex: String? = null
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\CharacterSheetUiState.kt
