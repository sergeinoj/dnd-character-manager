// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.model.creator.ClassStepData
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.*
import com.dnd.app.domain.usecase.creator.*
import com.dnd.app.ui.screens.character_creator.handlers.InventoryHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatorUiState(
    val draft: DraftCharacter = DraftCharacter(),
    // Data Sources
    val availableRaces: List<Race> = emptyList(),
    val availableClasses: List<ClassInfo> = emptyList(),
    val availableBackgrounds: List<Background> = emptyList(),
    val availableAlignments: List<AlignmentEntity> = emptyList(),
    // UI-specific Data
    val availableSubraces: List<Race> = emptyList(),
    val baseRaceFeatures: List<Feature> = emptyList(),
    val subraceFeatures: List<Feature> = emptyList(),
    val backgroundFeatures: List<Feature> = emptyList(),
    val classStepData: ClassStepData = ClassStepData(),
    val selectedFeatDetails: Feature? = null,
    val unpackedEquipmentOptions: Map<String, EquipmentOptionDetails> = emptyMap(),
    // State
    val isLoading: Boolean = true,
    val inventoryMode: InventoryMode = InventoryMode.STANDARD_PACKS,
)

@OptIn(FlowPreview::class) // [ИСПРАВЛЕНО] Добавлена аннотация для debounce
@HiltViewModel
class CharacterCreatorViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val characterRepository: CharacterRepository,
    private val assembler: CharacterAssembler,
    private val bakeUseCase: BakeCharacterUseCase,
    private val handleSelectionUseCase: HandleSelectionUseCase,
    private val updateStatUseCase: UpdateStatUseCase,
    private val updateBioUseCase: UpdateBioUseCase,
    private val getClassStepDataUseCase: GetClassStepDataUseCase,
    private val calculator: DndCalculator,
    private val draftStatsUseCase: DraftStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorUiState())
    val uiState: StateFlow<CreatorUiState> = _uiState.asStateFlow()

    val inventoryHandler = InventoryHandler(viewModelScope, libraryRepository) { cart ->
        val result = handleSelectionUseCase(
            SelectionSource.INVENTORY, "shop_cart", ChoiceResult.SelectedOptions(cart.map { it.index }), _uiState.value.draft
        )
        _uiState.update { it.copy(draft = result.draft) }
    }

    init {
        loadInitialData()
        observeFeatSelections()
        observeMasterFlow()
    }

    private fun loadInitialData() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val backgrounds = libraryRepository.getAllBackgrounds()
        _uiState.update { it.copy(
            availableRaces = libraryRepository.getAllParentRaces(),
            availableClasses = libraryRepository.getAllClasses(),
            availableBackgrounds = backgrounds,
            availableAlignments = libraryRepository.getAllAlignments(),
            isLoading = false
        )}
    }

    private fun observeMasterFlow() {
        viewModelScope.launch {
            uiState.map { it.draft }
                .distinctUntilChanged()
                .drop(1)
                .debounce(50)
                .collectLatest { draft ->
                    Log.d("VMLog", "Master Flow Triggered by Draft Change")
                    val baked = bakeUseCase(draft, _uiState.value.availableBackgrounds)
                    refreshDependentData(baked)
                }
        }
    }


    private fun observeFeatSelections() {
        viewModelScope.launch {
            uiState
                .map { it.draft.baseInfo.raceSelections }
                .map { selections ->
                    selections.values
                        .filterIsInstance<ChoiceResult.SelectedOptions>()
                        .firstNotNullOfOrNull { result -> result.items.firstOrNull { item -> item.startsWith("feat-") } }
                }
                .distinctUntilChanged()
                .collect { featIndex: String? ->
                    val currentFeat = _uiState.value.selectedFeatDetails
                    if (currentFeat?.index != featIndex) {
                        val newFeatDetails = if (featIndex != null) {
                            libraryRepository.getFeatureByIndex(featIndex)
                        } else {
                            null
                        }
                        _uiState.update { it.copy(selectedFeatDetails = newFeatDetails) }
                    }
                }
        }
    }

    fun selectRace(idx: String) = viewModelScope.launch {
        val r = _uiState.value.availableRaces.find { it.index == idx } ?: return@launch
        val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(
            raceIndex = idx, subraceIndex = null, raceSelections = emptyMap()
        ))
        _uiState.update { it.copy(
            draft = newDraft,
            baseRaceFeatures = libraryRepository.getBaseRaceFeatures(r.id),
            availableSubraces = libraryRepository.getSubracesFromDb(r.id),
            subraceFeatures = emptyList(),
            selectedFeatDetails = null
        )}
    }

    fun selectSubrace(idx: String) = viewModelScope.launch {
        val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(subraceIndex = idx))
        _uiState.update { it.copy(
            draft = newDraft,
            subraceFeatures = libraryRepository.getSubraceFeatures(idx)
        )}
    }

    fun selectClass(idx: String) = viewModelScope.launch {
        val newDraft = _uiState.value.draft.copy(
            levelStack = listOf(LevelStep(classIndex = idx)),
            baseInfo = _uiState.value.draft.baseInfo.copy(inventorySelections = emptyMap())
        )
        _uiState.update { it.copy(draft = newDraft) }
    }

    fun selectSubclass(idx: String) = viewModelScope.launch {
        val stack = _uiState.value.draft.levelStack.toMutableList()
        if(stack.isEmpty()) return@launch
        stack[0] = stack[0].copy(subclassIndex = idx)
        val newDraft = _uiState.value.draft.copy(levelStack = stack)
        _uiState.update { it.copy(draft = newDraft) }
    }

    fun selectBackground(bg: Background) = viewModelScope.launch {
        val newDraft = _uiState.value.draft.copy(
            baseInfo = _uiState.value.draft.baseInfo.copy(backgroundIndex = bg.name, backgroundSelections = emptyMap())
        )
        _uiState.update { it.copy(
            draft = newDraft,
            backgroundFeatures = bg.features
        )}
    }

    fun onSelectionChange(source: SelectionSource, key: String, res: ChoiceResult) = viewModelScope.launch {
        val result = handleSelectionUseCase(source, key, res, _uiState.value.draft)
        _uiState.update { it.copy(draft = result.draft) }
    }

    fun updateStat(key: String, delta: Int) = viewModelScope.launch {
        val newDraft = updateStatUseCase(_uiState.value.draft, key, delta)
        _uiState.update { it.copy(draft = newDraft) }
    }

    fun updateBio(type: String, value: String) {
        _uiState.update { it.copy(draft = updateBioUseCase.updateField(it.draft, type, value)) }
    }

    fun rollBioTrait(type: String) {
        _uiState.update { it.copy(draft = updateBioUseCase.rollTrait(it.draft, type, it.availableBackgrounds)) }
    }

    fun setInventoryMode(mode: InventoryMode) {
        val classIndex = _uiState.value.draft.levelStack.firstOrNull()?.classIndex
        if(mode == InventoryMode.BUY_WITH_GOLD && classIndex != null){
            inventoryHandler.initializeForGoldBuy(classIndex)
        }
        val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(inventorySelections = emptyMap()))
        _uiState.update { it.copy(draft = newDraft, inventoryMode = mode) }
    }

    fun saveCharacter(onSuccess: () -> Unit) = viewModelScope.launch {
        val finalBakedDraft = bakeUseCase(_uiState.value.draft, _uiState.value.availableBackgrounds)
        val domain = assembler.assemble(finalBakedDraft)
        (characterRepository as com.dnd.app.data.repository.CharacterRepositoryImpl).saveFullCharacter(domain, finalBakedDraft)
        onSuccess()
    }

    private fun getRelevantAbilityModifier(draft: DraftCharacter): Int {
        val classIndex = draft.levelStack.firstOrNull()?.classIndex ?: return 0
        val primaryStat = when (classIndex) {
            "cleric", "druid", "ranger" -> "WIS"; "wizard" -> "INT"; "paladin", "bard", "sorcerer", "warlock" -> "CHA"; else -> null
        } ?: return 0
        val totalScore = (draft.baseInfo.baseAbilityScores[primaryStat] ?: 10) + (draft.baseInfo.aggregateStatBonuses[primaryStat] ?: 0)
        return calculator.calculateModifier(totalScore)
    }

    private suspend fun refreshDependentData(draft: DraftCharacter) {
        Log.d("VMLog", "Refreshing dependent data for class...")
        val classIndex = draft.levelStack.firstOrNull()?.classIndex
        if (classIndex.isNullOrBlank()) {
            _uiState.update { it.copy(draft = draft, classStepData = ClassStepData()) }
            return
        }
        val subclassIndex = draft.levelStack.firstOrNull()?.subclassIndex

        val statRecalculatedDraft = draftStatsUseCase(draft)
        val abilityModifier = getRelevantAbilityModifier(statRecalculatedDraft)

        val data = getClassStepDataUseCase(classIndex, subclassIndex, abilityModifier)
        val unpacked = getClassStepDataUseCase.unpackAllEquipmentOptions(data.inventoryChoiceFeatures)

        val preparedSpellsFeature = data.classFeatures.find { it.index == "virtual-prepared-spells" }
        val newLimit = (preparedSpellsFeature?.choices?.firstOrNull() as? FeatureChoiceDomain.SelectSpell)?.count ?: 0
        val selections = statRecalculatedDraft.levelStack.first().selections.toMutableMap()
        val currentPrepared = (selections["virtual-prepared-spells"] as? ChoiceResult.Spells)?.spellIndexes.orEmpty()

        var finalDraft = statRecalculatedDraft
        if (currentPrepared.size > newLimit) {
            selections["virtual-prepared-spells"] = ChoiceResult.Spells(currentPrepared.take(newLimit))
            val newStack = statRecalculatedDraft.levelStack.toMutableList().apply { this[0] = this[0].copy(selections = selections) }
            finalDraft = statRecalculatedDraft.copy(levelStack = newStack)
        }

        _uiState.update { it.copy(
            draft = finalDraft,
            classStepData = data,
            unpackedEquipmentOptions = unpacked
        )}
        Log.d("VMLog", "State updated with new class data.")
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt