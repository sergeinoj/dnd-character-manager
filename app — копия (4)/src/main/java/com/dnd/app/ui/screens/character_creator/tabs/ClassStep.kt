// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/ClassStep.kt
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.components.*
import com.dnd.app.util.stripHtml

@Composable
fun ClassStep(
    availableClasses: List<ClassInfo>,
    selectedClassIndex: String,
    onClassSelect: (String) -> Unit,
    onSubclassSelect: (String) -> Unit,
    currentSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    globalExclusions: Set<String>,
    pickedSkills: List<String>,
    aggregatedSpellFeature: Feature?,
    availableSubclasses: List<SubclassInfo>,
    currentSubclassIndex: String?,
    expandedStates: MutableMap<String, Boolean>,
    classFeatures: List<Feature>,
    subclassChoiceFeature: Feature?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(WizardUiConfig.WIZARD_STEP_SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(WizardUiConfig.WIZARD_STEP_SECTION_GAP)
    ) {
        item {
            FlatWizardSection(title = "Класс") {
                val opts = availableClasses.map { ChoiceOption(it.index, it.name) }
                SmartDropdown(opts, selectedClassIndex, onSelected = { onClassSelect(it.id) })
            }
        }

        val selClass = availableClasses.find { it.index == selectedClassIndex }
        if (selClass != null) {
            item {
                FlatWizardSection(title = "Хиты") {
                    Column {
                        Text("Кость здоровья: 1к${selClass.hitDie}", fontSize = WizardUiConfig.FONT_SIZE_CONTENT)
                        Text(
                            "Начальное здоровье: ${selClass.hitDie} + Мод. Телосложения",
                            fontSize = WizardUiConfig.FONT_SIZE_CONTENT,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            items(classFeatures, key = { "base_feat_${it.index}" }) { feature ->
                if (feature.uiGroup == "SPELLS" && aggregatedSpellFeature != null) {
                    // Пропускаем, так как будет обработано ниже в агрегированном виде
                } else {
                    FeatureSection(
                        feature = feature,
                        allSelections = currentSelections,
                        onSelectionChanged = onSelectionChanged,
                        globalExclusions = globalExclusions,
                        pickedSkills = pickedSkills,
                        expandedStates = expandedStates
                    )
                }
            }

            aggregatedSpellFeature?.let { feature ->
                item(key = feature.index) {
                    FeatureSection(
                        feature = feature,
                        allSelections = currentSelections,
                        onSelectionChanged = onSelectionChanged,
                        globalExclusions = globalExclusions,
                        pickedSkills = pickedSkills,
                        expandedStates = expandedStates
                    )
                }
            }

            subclassChoiceFeature?.let { feature ->
                if (availableSubclasses.isNotEmpty()) {
                    item(key = "subclass_choice") {
                        FlatWizardSection(title = feature.name) {
                            Column {
                                Text(feature.description.stripHtml(), fontSize = WizardUiConfig.FONT_SIZE_CONTENT, lineHeight = 17.sp)
                                val subOpts = availableSubclasses.map { ChoiceOption(it.index, it.name) }
                                SmartDropdown(
                                    options = subOpts,
                                    selectedId = currentSubclassIndex,
                                    onSelected = { onSubclassSelect(it.id) },
                                    placeholder = "Выберите путь..."
                                )
                            }
                        }
                    }
                }
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
    pickedSkills: List<String>,
    expandedStates: MutableMap<String, Boolean>
) {
    val isSpellContainer = feature.index == "aggregated-spell-choice"

    FlatWizardSection(
        title = feature.name,
        applyDefaultContentPadding = !isSpellContainer
    ) {
        Column {
            if (feature.description.isNotBlank()) {
                Text(feature.description.stripHtml(), fontSize = WizardUiConfig.FONT_SIZE_CONTENT, lineHeight = 17.sp)
            }
            if (feature.embeddedSpells.isNotEmpty()) {
                Column {
                    feature.embeddedSpells.forEach { spell ->
                        val spellExpandedKey = "granted_${feature.index}_${spell.index}"
                        val isExpanded = expandedStates.getOrPut(spellExpandedKey) { false }
                        UnifiedSpellListItem(
                            spell = spell,
                            actionType = SpellActionType.ADD,
                            onActionClick = {},
                            isActionEnabled = false,
                            isExpanded = isExpanded,
                            onToggleExpand = { expandedStates[spellExpandedKey] = !isExpanded }
                        )
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
                    globalExclusions = globalExclusions,
                    pickedSkills = pickedSkills
                )
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/ClassStep.kt