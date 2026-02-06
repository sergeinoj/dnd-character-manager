// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/RaceStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.Race
import com.dnd.app.ui.screens.character_creator.components.* // Импортирует WizardUiConfig и компоненты
import com.dnd.app.util.stripHtml

@Composable
fun RaceStep(
    availableRaces: List<Race>,
    selectedRaceIndex: String,
    onRaceSelect: (String) -> Unit,
    availableSubraces: List<Race>,
    selectedSubraceIndex: String?,
    onSubraceSelect: (String) -> Unit,
    baseFeatures: List<Feature>,
    subraceFeatures: List<Feature>,
    currentSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    globalExclusions: Set<String>,
    selectedFeatDetails: Feature?,
    expandedStates: MutableMap<String, Boolean>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(WizardUiConfig.WIZARD_STEP_SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(WizardUiConfig.WIZARD_STEP_SECTION_GAP)
    ) {
        item {
            FlatWizardSection(title = "Раса") {
                val opts = availableRaces.map { ChoiceOption(it.index, it.name) }
                SmartDropdown(opts, selectedRaceIndex, onSelected = { onRaceSelect(it.id) })
            }
        }

        items(baseFeatures, key = { "base_f_${it.id}_${it.index}" }) { feature ->
            if (feature.isSubraceSelector) {
                FlatWizardSection(title = feature.name) {
                    val opts = availableSubraces.map { ChoiceOption(it.index, it.name) }
                    SmartDropdown(
                        opts,
                        selectedSubraceIndex,
                        onSelected = { onSubraceSelect(it.id) },
                        placeholder = "Выберите разновидность..."
                    )
                }
            } else {
                // [ИЗМЕНЕНО] Логика выбора черты теперь применяется и к базовым способностям расы.
                if (feature.index.contains("feat")) {
                    FlatWizardSection(title = feature.name) {
                        FeatSelectionCard(
                            grantingFeature = feature,
                            selectedFeatDetails = selectedFeatDetails,
                            allSelections = currentSelections,
                            proficiencyExclusions = globalExclusions,
                            onSelectionUpdated = onSelectionChanged,
                            expandedStates = expandedStates
                        )
                    }
                } else {
                    FeatureSection(
                        feature = feature,
                        allSelections = currentSelections,
                        onSelectionChanged = onSelectionChanged,
                        globalExclusions = globalExclusions,
                        expandedStates = expandedStates
                    )
                }
            }
        }

        items(subraceFeatures, key = { "sub_f_${it.id}_${it.index}" }) { feature ->
            if (feature.index.contains("feat")) {
                FlatWizardSection(title = feature.name) {
                    FeatSelectionCard(
                        grantingFeature = feature,
                        selectedFeatDetails = selectedFeatDetails,
                        allSelections = currentSelections,
                        proficiencyExclusions = globalExclusions,
                        onSelectionUpdated = onSelectionChanged,
                        expandedStates = expandedStates
                    )
                }
            } else {
                FeatureSection(
                    feature = feature,
                    allSelections = currentSelections,
                    onSelectionChanged = onSelectionChanged,
                    globalExclusions = globalExclusions,
                    expandedStates = expandedStates
                )
            }
        }
    }
}

@Composable
private fun FeatureSection(
    feature: Feature,
    allSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    globalExclusions: Set<String>,
    expandedStates: MutableMap<String, Boolean>
) {
    if (feature.name == "Описание" && feature.description.isBlank()) return

    FlatWizardSection(title = feature.name) {
        Column(verticalArrangement = Arrangement.spacedBy(WizardUiConfig.WIZARD_STEP_SECTION_GAP)) {
            if (feature.description.isNotBlank()) {
                Text(
                    text = feature.description.stripHtml(),
                    fontSize = WizardUiConfig.FONT_SIZE_CONTENT,
                    lineHeight = 18.sp
                )
            }
            if (feature.embeddedSpells.isNotEmpty()) {
                val key = "spells_${feature.index}"
                val isExpanded = expandedStates.getOrPut(key) { true }
                CollapsibleChoiceSection(
                    title = "Добавленные заклинания",
                    expanded = isExpanded,
                    onToggle = { expandedStates[key] = !isExpanded }
                ) {
                    Column {
                        feature.embeddedSpells.forEach { spell ->
                            val spellExpandedKey = "granted_${feature.index}_${spell.index}"
                            val isSpellExpanded = expandedStates.getOrPut(spellExpandedKey) { false }
                            UnifiedSpellListItem(
                                spell = spell,
                                actionType = SpellActionType.ADD,
                                onActionClick = {},
                                isActionEnabled = false,
                                isExpanded = isSpellExpanded,
                                onToggleExpand = { expandedStates[spellExpandedKey] = !isSpellExpanded }
                            )
                        }
                    }
                }
            }
            feature.choices.forEach { choice ->
                FeatureChoiceBlock(
                    choice = choice,
                    allSelections = allSelections,
                    onSelectionUpdated = onSelectionChanged,
                    selectionKey = feature.index,
                    expandedStates = expandedStates,
                    globalExclusions = globalExclusions
                )
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/RaceStep.kt