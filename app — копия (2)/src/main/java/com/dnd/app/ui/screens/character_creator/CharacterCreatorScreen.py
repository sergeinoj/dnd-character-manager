// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorScreen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.ui.components.DndActionTopBar
import com.dnd.app.ui.screens.character_creator.tabs.*
import com.dnd.app.ui.theme.DndBackground
import com.dnd.app.ui.theme.DndPrimary

@Composable
fun CharacterCreatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CharacterCreatorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        CreatorTab("Раса", Icons.Default.Person),
        CreatorTab("Класс", Icons.Default.AccountBox),
        CreatorTab("Статы", Icons.Default.Build),
        CreatorTab("Био", Icons.Default.Face),
        CreatorTab("Вещи", Icons.Default.ShoppingCart)
    )

    val dynamicRaceBonuses = remember(state.draft.baseInfo.raceSelections) {
        val bonuses = mutableMapOf<String, Int>()
        state.draft.baseInfo.raceSelections.values.forEach { result ->
            if (result is ChoiceResult.StatBonus) {
                result.bonuses.forEach { (stat, value) ->
                    val key = stat.take(3).uppercase()
                    bonuses[key] = (bonuses[key] ?: 0) + value
                }
            }
        }
        bonuses
    }

    Scaffold(
        topBar = {
            DndActionTopBar(
                title = if (state.draft.name.isBlank()) "Новый герой" else state.draft.name,
                onBack = onNavigateBack,
                onActionClick = { viewModel.saveCharacter(onSuccess = onNavigateBack) },
                actionIcon = { Icon(Icons.Default.Check, null, tint = Color.Black) }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DndPrimary) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        containerColor = DndBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                when (selectedTab) {
                    0 -> RaceStep(
                        availableRaces = state.availableRaces,
                        selectedRaceIndex = state.draft.baseInfo.raceIndex,
                        onRaceSelect = viewModel::selectRace,
                        availableSubraces = state.availableSubraces,
                        selectedSubraceIndex = state.draft.baseInfo.subraceIndex,
                        onSubraceSelect = viewModel::selectSubrace,
                        features = state.raceFeatures,
                        currentSelections = state.draft.baseInfo.raceSelections,
                        onSelectionChanged = viewModel::onRaceSelectionChange,
                        globalExclusions = state.globalExclusions
                    )
                    1 -> ClassStep(
                        availableClasses = state.availableClasses,
                        selectedClassIndex = state.draft.levelStack.firstOrNull()?.classIndex ?: "",
                        onClassSelect = viewModel::selectClass,
                        onSubclassSelect = viewModel::selectSubclass,
                        classFeatures = state.classFeatures,
                        currentSelections = state.draft.levelStack.firstOrNull()?.selections ?: emptyMap(),
                        onSelectionChanged = viewModel::onClassSelectionChange,
                        globalExclusions = state.globalExclusions,
                        currentSubclassIndex = state.draft.levelStack.firstOrNull()?.subclassIndex,
                        pickedSkills = state.draft.getPickedSkills() // ПРОБРОС ВЛАДЕНИЙ
                    )
                    2 -> StatsStep(
                        scores = state.draft.baseInfo.baseAbilityScores,
                        staticBonuses = state.draft.baseInfo.staticRaceBonuses,
                        dynamicBonuses = dynamicRaceBonuses,
                        onStatChange = viewModel::updateStat
                    )
                    3 -> BioStep(
                        name = state.draft.name,
                        onNameChange = { viewModel.updateName(it) },
                        availableAlignments = state.availableAlignments,
                        selectedAlignment = state.draft.baseInfo.alignmentIndex,
                        onAlignmentSelect = { viewModel.selectAlignment(it) },
                        availableBackgrounds = state.availableBackgrounds,
                        selectedBackground = state.draft.baseInfo.backgroundIndex,
                        onBackgroundSelect = { viewModel.selectBackground(it) },
                        backgroundFeatures = state.backgroundFeatures,
                        currentSelections = state.draft.baseInfo.backgroundSelections,
                        onSelectionChanged = { id, res -> viewModel.onBgSelectionChange(id, res) },
                        personalityTrait = state.draft.baseInfo.personalityTrait,
                        ideal = state.draft.baseInfo.ideal,
                        bond = state.draft.baseInfo.bond,
                        flaw = state.draft.baseInfo.flaw,
                        onRollTrait = { viewModel.rollCharacterTrait(it) },
                        onManualBioChange = { type, value -> viewModel.updateBioField(type, value) }
                    )
                    4 -> InventoryStep(
                        className = state.draft.levelStack.firstOrNull()?.classIndex,
                        backgroundName = state.draft.baseInfo.backgroundIndex
                    )
                }
            }
        }
    }
}

data class CreatorTab(val title: String, val icon: ImageVector)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorScreen.kt