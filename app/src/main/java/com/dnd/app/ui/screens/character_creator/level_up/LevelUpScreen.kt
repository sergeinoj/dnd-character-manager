// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\level_up\LevelUpScreen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.level_up

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.domain.model.*
import com.dnd.app.ui.components.DndActionTopBar
import com.dnd.app.ui.screens.character_creator.evaluateClassMulticlassRequirements
import com.dnd.app.ui.screens.character_creator.components.*
import com.dnd.app.ui.theme.DndBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelUpScreen(
    onBack: () -> Unit,
    viewModel: LevelUpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(onBack = onBack)

    LaunchedEffect(state.interactionError) {
        state.interactionError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val title = state.currentClassInfo?.let {
        "Повышение: ${it.name} (Ур. ${state.draft.levelStack.size})"
    } ?: "Повышение уровня"

    Scaffold(
        topBar = {
            DndActionTopBar(
                title = title,
                onBack = onBack,
                onActionClick = { viewModel.commitLevel(onSuccess = onBack) },
                actionIcon = { Icon(Icons.Default.Check, "Confirm Level Up", tint = Color.Black) },
                isActionEnabled = !state.isLoading
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = DndBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LevelUpContent(state, viewModel)

            if (state.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelUpContent(state: LevelUpUiState, viewModel: LevelUpViewModel) {
    val levelStep = state.draft.levelStack.lastOrNull()
    if (levelStep == null) {
        if (!state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Данные уровня не загружены")
            }
        }
        return
    }



    val effectiveStats = state.draft.resolveEffectiveStats()

    val previousClassIndex = if (state.draft.levelStack.size > 1)

        state.draft.levelStack[state.draft.levelStack.size - 2].classIndex else null

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

    val currentEvaluation = requirementEvaluations[levelStep.classIndex]

    val invalidMulticlassReason = if (isMulticlassSelection) currentEvaluation?.failureMessage else null

    val statusColor = if (invalidMulticlassReason != null) Color.Red else Color.Blue



    val isExpanded = { key: String -> state.expandedStates[key] ?: false }
    val onToggleExpanded = viewModel::toggleExpandedState

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(WizardUiConfig.WIZARD_STEP_SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(WizardUiConfig.WIZARD_STEP_SECTION_GAP)
    ) {
        item {
            FlatWizardSection(title = "Целевой класс") {
                ClassSelectionBlock(
                    currentClassIndex = levelStep.classIndex,
                    previousClassIndex = previousClassIndex,
                    availableClasses = classOptions,
                    onClassSelected = { viewModel.setClassForCurrentLevel(it) },
                    disabledClassIds = disabledClassIds,
                    statusText = invalidMulticlassReason,
                    statusColor = statusColor
                )
            }
        }

        item {
            FlatWizardSection(title = "Хиты") {
                val hitDie = state.currentClassInfo?.hitDie ?: 8
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Кость здоровья: 1к$hitDie", fontSize = 14.sp)
                    val average = (hitDie / 2) + 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = if (levelStep.hpIncrease == 0) "" else levelStep.hpIncrease.toString(),
                            onValueChange = {
                                val valInt = it.toIntOrNull()?.coerceIn(0, hitDie) ?: 0
                                viewModel.updateHpIncrease(valInt)
                            },
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
                        Button(
                            onClick = { viewModel.updateHpIncrease(average) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(4.dp)
                        ) { Text("Среднее ($average)") }
                    }
                }
            }
        }

        state.subclassChoiceFeature?.let { f ->
            item(key = "subclass_choice") {
                FeatureSection(
                    feature = f, selectionSource = SelectionSource.CLASS,
                    allSelections = levelStep.selections,
                    onSelectionChanged = viewModel::handleSelection,
                    proficiencyExclusions = state.proficiencyExclusions,
                    pickedProficiencies = state.draft.getPickedProficiencies(),
                    isExpanded = isExpanded, onToggleExpanded = onToggleExpanded, featRegistry = state.featMetadataRegistry,
                    extraContent = {
                        val subOpts = state.availableSubclasses.map { ChoiceOption(it.index, it.name) }
                        SmartDropdown(
                            options = subOpts,
                            selectedId = levelStep.subclassIndex,
                            onSelected = { viewModel.selectSubclass(it.id) },
                            placeholder = "Выберите специализацию..."
                        )
                    }
                )
            }
        }

        items(state.classStepFeatures.sortedBy { it.priority }, key = { "feat_${it.id}_${it.index}" }) { f ->
            if (f.uiGroup != "SPELLS") {
                FeatureSection(
                    f,
                    SelectionSource.CLASS,
                    levelStep.selections,
                    viewModel::handleSelection,
                    state.proficiencyExclusions,
                    state.draft.getPickedProficiencies(),
                    isExpanded,
                    onToggleExpanded,
                    state.featMetadataRegistry
                )
            }
        }

        state.aggregatedSpellFeature?.let { f ->
            item(key = f.index) {
                FeatureSection(
                    f,
                    SelectionSource.CLASS,
                    levelStep.selections,
                    viewModel::handleSelection,
                    state.proficiencyExclusions,
                    state.draft.getPickedProficiencies(),
                    isExpanded,
                    onToggleExpanded,
                    state.featMetadataRegistry
                )
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\level_up\LevelUpScreen.kt
