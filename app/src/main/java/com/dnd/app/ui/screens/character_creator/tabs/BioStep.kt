// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\tabs\BioStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.CharacterCreatorViewModel
import com.dnd.app.ui.screens.character_creator.CreatorUiState
import com.dnd.app.ui.screens.character_creator.components.*

@Composable
fun BioStep(
    state: CreatorUiState,
    viewModel: CharacterCreatorViewModel,
    isExpanded: (String) -> Boolean,
    onToggleExpanded: (String) -> Unit,
) {

    val selectedBg = state.availableBackgrounds.find { it.index == state.draft.baseInfo.backgroundIndex }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WizardUiConfig.WIZARD_STEP_SCREEN_PADDING)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(WizardUiConfig.WIZARD_STEP_SECTION_GAP)
    ) {
        FlatWizardSection(title = "Личность") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BioRow("Имя", state.draft.name, viewModel::updateName)
                Column {
                    Text("Пол", fontWeight = FontWeight.SemiBold)
                    val genderOptions = listOf(
                        ChoiceOption("male", "Мужчина"),
                        ChoiceOption("female", "Женщина"),
                        ChoiceOption("other", "Иное")
                    )
                    SmartDropdown(
                        options = genderOptions,
                        selectedId = state.draft.baseInfo.gender,
                        onSelected = { viewModel.updateGender(it.id) }
                    )
                }
                Column {
                    Text("Мировоззрение", fontWeight = FontWeight.SemiBold)
                    val opts = state.availableAlignments.map { ChoiceOption(it.indexName, it.name) }
                    SmartDropdown(opts, state.draft.baseInfo.alignmentIndex, onSelected = { viewModel.selectAlignment(it.id) })
                }
                BioRow("Черты характера", state.draft.baseInfo.personalityTrait, viewModel::updatePersonalityTrait)
                BioRow("Идеалы", state.draft.baseInfo.ideal, viewModel::updateIdeal)
                BioRow("Привязанности", state.draft.baseInfo.bond, viewModel::updateBond)
                BioRow("Слабости", state.draft.baseInfo.flaw, viewModel::updateFlaw)
                BioRow("Внешность", state.draft.baseInfo.appearance, viewModel::updateAppearance)
            }
        }
        FlatWizardSection(title = "Предыстория") {

            val opts = state.availableBackgrounds.map { ChoiceOption(it.index, it.name) }


            SmartDropdown(
                options = opts,
                selectedId = state.draft.baseInfo.backgroundIndex,
                onSelected = { o ->

                    state.availableBackgrounds.find { it.index == o.id }?.let { viewModel.selectBackground(it) }
                }
            )
        }


        if (selectedBg != null) {
            state.backgroundFeatures.forEach { f ->
                FeatureSection(
                    feature = f,
                    selectionSource = SelectionSource.BACKGROUND,
                    allSelections = state.draft.baseInfo.backgroundSelections,
                    onSelectionChanged = viewModel::onBgSelectionChange,
                    proficiencyExclusions = state.proficiencyExclusions,
                    isExpanded = isExpanded, onToggleExpanded = onToggleExpanded,
                    pickedProficiencies = state.draft.getPickedProficiencies()
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\tabs\BioStep.kt
