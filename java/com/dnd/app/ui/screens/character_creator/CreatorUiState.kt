// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CreatorUiState.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import androidx.compose.ui.graphics.vector.ImageVector
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.domain.model.Background
import com.dnd.app.domain.model.ClassInfo
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.InventoryMode
import com.dnd.app.domain.model.Money
import com.dnd.app.domain.model.Race
import com.dnd.app.domain.model.ShopCategory
import com.dnd.app.domain.model.ShopItem
import com.dnd.app.domain.model.ShopView
import com.dnd.app.domain.model.SubclassInfo

data class EquipmentOptionDetails(val name: String, val contents: List<String>)

data class CreatorUiState(
    val draft: DraftCharacter = DraftCharacter(),
    val availableRaces: List<Race> = emptyList(),
    val availableClasses: List<ClassInfo> = emptyList(),
    val availableBackgrounds: List<Background> = emptyList(),
    val availableAlignments: List<AlignmentEntity> = emptyList(),
    val baseRaceFeatures: List<Feature> = emptyList(),
    val subraceFeatures: List<Feature> = emptyList(),
    val backgroundFeatures: List<Feature> = emptyList(),
    val globalExclusions: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val selectedFeatDetails: Feature? = null,
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
    val unpackedEquipmentOptions: Map<String, EquipmentOptionDetails> = emptyMap()
)

data class CreatorTab(val title: String, val icon: ImageVector)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CreatorUiState.kt