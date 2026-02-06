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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.domain.usecase.creator.SelectionSource
import com.dnd.app.ui.components.DndActionTopBar
import com.dnd.app.ui.debug.DraftInspectorSheet
import com.dnd.app.ui.screens.character_creator.tabs.*
import com.dnd.app.ui.theme.DndBackground
import com.dnd.app.ui.theme.DndPrimary

data class CreatorTab(val title: String, val icon: ImageVector)
data class EquipmentOptionDetails(val name: String, val contents: List<String>)

@Composable
fun CharacterCreatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CharacterCreatorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val inventoryState by viewModel.inventoryHandler.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showInspector by remember { mutableStateOf(false) }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    val tabs = listOf(
        CreatorTab("Раса", Icons.Default.Person),
        CreatorTab("Класс", Icons.Default.AccountBox),
        CreatorTab("Статы", Icons.Default.Build),
        CreatorTab("Био", Icons.Default.Face),
        CreatorTab("Вещи", Icons.Default.ShoppingCart)
    )

    if (showInspector) {
        Dialog(onDismissRequest = { showInspector = false }) {
            DraftInspectorSheet(draft = state.draft, onDismiss = { showInspector = false })
        }
    }

    Scaffold(
        topBar = {
            DndActionTopBar(
                title = if (state.draft.name.isBlank()) "Новый герой" else state.draft.name,
                onBack = onNavigateBack,
                onActionClick = { viewModel.saveCharacter(onSuccess = onNavigateBack) },
                actionIcon = { Icon(Icons.Default.Check, null, tint = Color.Black) },
                onDebugClick = { showInspector = true }
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
                        baseFeatures = state.baseRaceFeatures,
                        subraceFeatures = state.subraceFeatures,
                        currentSelections = state.draft.baseInfo.raceSelections,
                        onSelectionChanged = { key, res -> viewModel.onSelectionChange(SelectionSource.RACE, key, res) },
                        globalExclusions = state.draft.getProficiencyExclusions(),
                        selectedFeatDetails = state.selectedFeatDetails,
                        expandedStates = expandedStates
                    )
                    1 -> ClassStep(
                        availableClasses = state.availableClasses,
                        selectedClassIndex = state.draft.levelStack.firstOrNull()?.classIndex ?: "",
                        onClassSelect = viewModel::selectClass,
                        onSubclassSelect = viewModel::selectSubclass,
                        currentSelections = state.draft.levelStack.firstOrNull()?.selections ?: emptyMap(),
                        onSelectionChanged = { key, res -> viewModel.onSelectionChange(SelectionSource.CLASS, key, res) },
                        globalExclusions = state.draft.getProficiencyExclusions(),
                        pickedSkills = state.draft.getPickedSkills(),
                        expandedStates = expandedStates,
                        classStepData = state.classStepData,
                        currentSubclassIndex = state.draft.levelStack.firstOrNull()?.subclassIndex
                    )
                    2 -> StatsStep(
                        scores = state.draft.baseInfo.baseAbilityScores,
                        aggregateBonuses = state.draft.baseInfo.aggregateStatBonuses,
                        onStatChange = viewModel::updateStat
                    )
                    3 -> BioStep(
                        name = state.draft.name,
                        onNameChange = { viewModel.updateBio("name", it) },
                        availableAlignments = state.availableAlignments,
                        selectedAlignment = state.draft.baseInfo.alignmentIndex,
                        onAlignmentSelect = { viewModel.updateBio("alignment", it) },
                        availableBackgrounds = state.availableBackgrounds,
                        selectedBackground = state.draft.baseInfo.backgroundIndex,
                        onBackgroundSelect = viewModel::selectBackground,
                        backgroundFeatures = state.backgroundFeatures,
                        currentSelections = state.draft.baseInfo.backgroundSelections,
                        onSelectionChanged = { key, res -> viewModel.onSelectionChange(SelectionSource.BACKGROUND, key, res) },
                        personalityTrait = state.draft.baseInfo.personalityTrait,
                        ideal = state.draft.baseInfo.ideal,
                        bond = state.draft.baseInfo.bond,
                        flaw = state.draft.baseInfo.flaw,
                        onRollTrait = viewModel::rollBioTrait,
                        onManualBioChange = { type, value -> viewModel.updateBio(type, value) },
                        expandedStates = expandedStates,
                        globalExclusions = state.draft.getProficiencyExclusions(),
                        pickedSkills = state.draft.getPickedSkills()
                    )
                    4 -> InventoryStep(
                        state = state,
                        inventoryState = inventoryState,
                        handler = viewModel.inventoryHandler,
                        onModeChange = viewModel::setInventoryMode,
                        onSelectionChanged = { key, res -> viewModel.onSelectionChange(SelectionSource.INVENTORY, key, res) }
                    )
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorScreen.kt