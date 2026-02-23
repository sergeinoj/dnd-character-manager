// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\tabs\ClassStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.CharacterCreatorViewModel
import com.dnd.app.ui.screens.character_creator.CreatorUiState
import com.dnd.app.ui.screens.character_creator.evaluateClassMulticlassRequirements
import com.dnd.app.ui.screens.character_creator.components.*

@Composable
fun ClassStep(
    state: CreatorUiState,
    viewModel: CharacterCreatorViewModel,
    isExpanded: (String) -> Boolean,
    onToggleExpanded: (String) -> Unit,
) {
    val currentLevelIndex = state.editingLevelIndex
    val levelStep = state.draft.levelStack.getOrNull(currentLevelIndex)
    val previousClassIndex = if (currentLevelIndex > 0) state.draft.levelStack.getOrNull(currentLevelIndex - 1)?.classIndex else null
    val effectiveStats = state.draft.resolveEffectiveStats()
    val isMulticlassSelection = previousClassIndex != null
    val requirementEvaluations = state.availableClasses.associate { classInfo ->
        classInfo.index to evaluateClassMulticlassRequirements(classInfo, effectiveStats)
    }
    val disabledClassIds = if (isMulticlassSelection) {
        requirementEvaluations
            .filterValues { !it.meetsRequirements }
            .filterKeys { it != previousClassIndex }
            .keys
    } else {
        emptySet()
    }
    val classOptions = state.availableClasses.map { classInfo ->
        val evaluation = requirementEvaluations[classInfo.index]
        ChoiceOption(
            classInfo.index,
            classInfo.name,
            info = if (isMulticlassSelection && classInfo.index != previousClassIndex) evaluation?.requirementLabel else null
        )
    }
    val currentEvaluation = levelStep?.classIndex?.let { requirementEvaluations[it] }
    val invalidMulticlassReason = if (isMulticlassSelection) currentEvaluation?.failureMessage else null
    val statusColor = if (invalidMulticlassReason != null) Color.Red else Color.Blue

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(WizardUiConfig.WIZARD_STEP_SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(WizardUiConfig.WIZARD_STEP_SECTION_GAP)
    ) {
        item {
            FlatWizardSection(title = if (currentLevelIndex == 0) "Класс" else "Повышение уровня (Ур. ${currentLevelIndex + 1})") {
                if (currentLevelIndex == 0 && state.isEditMode) {
                    val className = state.availableClasses.find { it.index == levelStep?.classIndex }?.name ?: "Неизвестно"
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp)); Text(text = className, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                } else {
                    ClassSelectionBlock(
                        levelStep?.classIndex ?: "",
                        previousClassIndex,
                        classOptions,
                        { if (currentLevelIndex == 0) viewModel.selectClass(it) else viewModel.setClassForCurrentLevel(it) },
                        disabledClassIds = disabledClassIds,
                        statusText = invalidMulticlassReason,
                        statusColor = statusColor
                    )
                }
            }
        }

        val selClass = state.availableClasses.find { it.index == levelStep?.classIndex }
        if (selClass != null) {
            item {
                FlatWizardSection(title = "Хиты") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Кость здоровья: 1к${selClass.hitDie}", fontSize = 14.sp)
                        if (currentLevelIndex == 0) {
                            Text(text = "На 1-м уровне: ${selClass.hitDie} + Модификатор Телосложения", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        } else {
                            val average = (selClass.hitDie / 2) + 1
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = if (levelStep?.hpIncrease == 0) "" else levelStep?.hpIncrease.toString(),
                                    onValueChange = { val valInt = it.toIntOrNull() ?: 0; viewModel.updateHpIncrease(valInt) },
                                    label = { Text("Бросок") },
                                    modifier = Modifier.width(100.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Button(onClick = { viewModel.updateHpIncrease(average) }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), shape = RoundedCornerShape(4.dp)) { Text("Среднее ($average)") }
                            }
                        }
                    }
                }
            }

            state.subclassChoiceFeature?.let { f ->
                item(key = "subclass_choice") {
                    FeatureSection(
                        feature = f, selectionSource = SelectionSource.CLASS,
                        allSelections = levelStep?.selections ?: emptyMap(), onSelectionChanged = viewModel::onClassSelectionChange,
                        proficiencyExclusions = state.proficiencyExclusions, pickedProficiencies = state.draft.getPickedProficiencies(),
                        isExpanded = isExpanded, onToggleExpanded = onToggleExpanded, featRegistry = state.featMetadataRegistry,
                        extraContent = {
                            val subOpts = state.availableSubclasses.map { ChoiceOption(it.index, it.name) }
                            SmartDropdown(
                                options = subOpts,
                                selectedId = levelStep?.subclassIndex,
                                onSelected = { viewModel.selectSubclass(it.id) },
                                placeholder = "Выберите специализацию..."
                            )
                        }
                    )
                }
            }

            items(state.classStepFeatures.sortedBy { it.priority }, key = { "feat_${it.id}_${it.index}" }) { f ->
                if (f.uiGroup != "SPELLS") FeatureSection(f, SelectionSource.CLASS, levelStep?.selections ?: emptyMap(), viewModel::onClassSelectionChange, state.proficiencyExclusions, state.draft.getPickedProficiencies(), isExpanded, onToggleExpanded, state.featMetadataRegistry)
            }

            state.aggregatedSpellFeature?.let { f ->
                item(key = f.index) { FeatureSection(f, SelectionSource.CLASS, levelStep?.selections ?: emptyMap(), viewModel::onClassSelectionChange, state.proficiencyExclusions, state.draft.getPickedProficiencies(), isExpanded, onToggleExpanded, state.featMetadataRegistry) }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\tabs\ClassStep.kt
