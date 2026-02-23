// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\CreatorUiState.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import androidx.compose.ui.graphics.vector.ImageVector
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.domain.model.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

data class EquipmentOptionDetails(
    val name: String,
    val contents: List<String>,
    val description: String? = null
)


data class CreatorUiState(
    val draft: DraftCharacter = DraftCharacter(),
    val availableRaces: List<Race> = emptyList(),
    val availableClasses: List<ClassInfo> = emptyList(),
    val availableBackgrounds: List<Background> = emptyList(),
    val availableAlignments: List<AlignmentEntity> = emptyList(),

    val baseRaceFeatures: List<Feature> = emptyList(),
    val subraceFeatures: List<Feature> = emptyList(),
    val backgroundFeatures: List<Feature> = emptyList(),

    val proficiencyExclusions: Map<Int, Set<String>> = emptyMap(),
    val isLoading: Boolean = true,

    val featMetadataRegistry: Map<String, Feature> = emptyMap(),

    val aggregatedSpellFeature: Feature? = null,
    val availableSubraces: List<Race> = emptyList(),
    val availableSubclasses: List<SubclassInfo> = emptyList(),
    val classStepFeatures: List<Feature> = emptyList(),
    val inventoryStepFeatures: List<Feature> = emptyList(),
    val subclassChoiceFeature: Feature? = null,
    val inventoryMode: InventoryMode = InventoryMode.STANDARD_PACKS,
    val shopView: ShopView = ShopView.CATEGORIES,
    val shopCategories: List<ShopCategory> = emptyList(),
    val shopItems: List<ShopItem> = emptyList(),
    val shoppingCart: List<ShopItem> = emptyList(),
    val remainingGold: Money = Money(),
    val initialGold: Money = Money(),
    val currentShopTitle: String = "Магазин",
    val unpackedEquipmentOptions: Map<String, EquipmentOptionDetails> = emptyMap(),
    val tabErrors: Map<Int, Boolean> = emptyMap(),
    val validationIssues: List<ValidationIssue> = emptyList(),
    val expandedStates: PersistentMap<String, Boolean> = persistentMapOf(),

    val editingLevelIndex: Int = 0,
    val pendingClassChange: String? = null,

    val interactionError: String? = null
) {
    val isEditMode: Boolean get() = draft.id != 0L
}

data class CreatorTab(val title: String, val icon: ImageVector)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\CreatorUiState.kt