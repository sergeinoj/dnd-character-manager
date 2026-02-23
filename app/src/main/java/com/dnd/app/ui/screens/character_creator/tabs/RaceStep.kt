// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\tabs\RaceStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.CharacterCreatorViewModel
import com.dnd.app.ui.screens.character_creator.CreatorUiState
import com.dnd.app.ui.screens.character_creator.components.*

@Composable
fun RaceStep(
    state: CreatorUiState,
    viewModel: CharacterCreatorViewModel,
    isExpanded: (String) -> Boolean,
    onToggleExpanded: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(WizardUiConfig.WIZARD_STEP_SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(WizardUiConfig.WIZARD_STEP_SECTION_GAP)
    ) {
        item {
            FlatWizardSection(title = "Раса") {
                if (state.isEditMode) {
                    val raceName = state.availableRaces.find { it.index == state.draft.baseInfo.raceIndex }?.name ?: "Unknown"
                    Text(text = raceName, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Расу нельзя изменить у созданного персонажа.", fontSize = 12.sp, color = Color.Black)
                } else {
                    val opts = state.availableRaces.map { ChoiceOption(it.index, it.name) }
                    SmartDropdown(opts, state.draft.baseInfo.raceIndex, onSelected = { viewModel.selectRace(it.id) })
                }
            }
        }

        items(state.baseRaceFeatures, key = { "base_f_${it.id}_${it.index}" }) { feature ->
            if (feature.isSubraceSelector) {
                FlatWizardSection(title = feature.name) {
                    if (state.isEditMode) {
                        val subName = state.availableSubraces.find { it.index == state.draft.baseInfo.subraceIndex }?.name ?: "Без подрасы"
                        Text(text = subName, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Разновидность нельзя изменить.", fontSize = 12.sp, color = Color.Black)
                    } else {
                        val opts = state.availableSubraces.map { ChoiceOption(it.index, it.name) }
                        SmartDropdown(opts, state.draft.baseInfo.subraceIndex, onSelected = { viewModel.selectSubrace(it.id) }, placeholder = "Выберите разновидность...")
                    }
                }
            } else {
                FeatureSection(
                    feature = feature,
                    selectionSource = SelectionSource.RACE,
                    allSelections = state.draft.baseInfo.raceSelections,
                    onSelectionChanged = viewModel::onRaceSelectionChange,
                    proficiencyExclusions = state.proficiencyExclusions,
                    isExpanded = isExpanded,
                    onToggleExpanded = onToggleExpanded,
                    pickedProficiencies = state.draft.getPickedProficiencies(),
                    featRegistry = state.featMetadataRegistry
                )
            }
        }

        items(state.subraceFeatures, key = { "sub_f_${it.id}_${it.index}" }) { feature ->
            FeatureSection(
                feature = feature,
                selectionSource = SelectionSource.RACE,
                allSelections = state.draft.baseInfo.raceSelections,
                onSelectionChanged = viewModel::onRaceSelectionChange,
                proficiencyExclusions = state.proficiencyExclusions,
                isExpanded = isExpanded,
                onToggleExpanded = onToggleExpanded,
                pickedProficiencies = state.draft.getPickedProficiencies(),
                featRegistry = state.featMetadataRegistry
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\tabs\RaceStep.kt
