// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\CharacterCreatorViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.*
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class CharacterCreatorViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val classFeatureRepository: ClassFeatureRepository,
    private val characterRepository: CharacterRepository,
    private val updateStatUseCase: UpdateStatUseCase,
    private val bakeCharacterUseCase: BakeCharacterUseCase,
    private val getClassProgressionDataUseCase: GetClassProgressionDataUseCase,
    private val handleSelectionUseCase: HandleSelectionUseCase,
    private val inventoryHandler: InventoryHandler,
    private val calculator: DndCalculator,
    private val validateDraftUseCase: ValidateDraftUseCase,
    private val getBackgroundDataUseCase: GetBackgroundDataUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorUiState())
    val uiState = _uiState.asStateFlow()
    private val TAG = "DND_LOG_CC_VM"
    private val selectionMutex = Mutex()

    init {
        inventoryHandler.initialize(viewModelScope, _uiState)
        val draftId = savedStateHandle.get<Long>("draftId") ?: 0L
        if (draftId != 0L) loadExistingDraft(draftId) else loadInitialData()
    }

    private fun loadExistingDraft(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val draft = characterRepository.getDraftById(id) ?: return@launch
            val refs = fetchReferenceDataLocally()
            runBakeAndValidate(incomingDraft = draft, forcedLevelIndex = (draft.levelStack.size - 1).coerceAtLeast(0), injectedRefs = refs)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val refs = fetchReferenceDataLocally()
            runBakeAndValidate(incomingDraft = _uiState.value.draft, injectedRefs = refs)
        }
    }

    private suspend fun fetchReferenceDataLocally() = coroutineScope {
        val r = async { libraryRepository.getAllParentRaces() }
        val c = async { libraryRepository.getAllClasses() }
        val b = async { libraryRepository.getAllBackgrounds() }
        val a = async { libraryRepository.getAllAlignments() }
        ReferenceDataPackage(r.await(), c.await(), b.await(), a.await())
    }

    private data class ReferenceDataPackage(
        val races: List<Race>, val classes: List<ClassInfo>, val backgrounds: List<Background>,
        val alignments: List<com.dnd.app.data.local.entity.AlignmentEntity>
    )

    private suspend fun buildMetadataRegistry(draft: DraftCharacter, totalFeatures: List<Feature>): Map<String, Feature> {
        val draftFeatIds = mutableSetOf<String>()

        fun scan(selections: Map<String, ChoiceResult>) {
            selections.values.filterIsInstance<ChoiceResult.SelectedOptions>().forEach { result ->
                result.items.forEach { itemId ->
                    if (itemId.isNotBlank() && itemId.any { it.isLetter() }) {
                        draftFeatIds.add(itemId)
                    }
                }
            }
        }

        scan(draft.baseInfo.raceSelections)
        scan(draft.baseInfo.backgroundSelections)
        draft.levelStack.forEach { step -> scan(step.selections) }

        val alreadyKnown = totalFeatures.associateBy { it.index }
        val idsToFetch = draftFeatIds.filter { id -> !alreadyKnown.containsKey(id) }
        Log.d("RALPH", "Creator metadata registry will fetch ${idsToFetch.size} new ids: $idsToFetch")

        val fetched: List<Feature> = idsToFetch.mapNotNull { featId ->
            libraryRepository.getFeatureByIndex(featId)
        }

        val combined = totalFeatures + fetched
        val registry = mutableMapOf<String, Feature>()
        combined.forEach { feature ->
            if (feature.index.isNotBlank()) {
                registry[feature.index] = feature
                registry[feature.index.trim().lowercase()] = feature
            }
        }
        return registry
    }

    private suspend fun runBakeAndValidate(
        incomingDraft: DraftCharacter, forcedLevelIndex: Int? = null, injectedRefs: ReferenceDataPackage? = null
    ) = coroutineScope {
        val bakedDraft = bakeCharacterUseCase(incomingDraft)
        val levelIndex = forcedLevelIndex ?: _uiState.value.editingLevelIndex
        val levelStep = bakedDraft.levelStack.getOrNull(levelIndex)

        val raceFeatsDef = async { if (bakedDraft.baseInfo.raceIndex.isNotBlank()) libraryRepository.getRaceByIndex(bakedDraft.baseInfo.raceIndex)?.let { libraryRepository.getBaseRaceFeatures(it.id) } ?: emptyList() else emptyList() }
        val subFeatsDef = async { bakedDraft.baseInfo.subraceIndex?.let { libraryRepository.getSubraceFeatures(it) } ?: emptyList() }

        val classDataDef = async {
            if (levelStep != null) {
                val classMetadata = bakedDraft.levelStack.map { it.classIndex }.distinct().mapNotNull { idx ->
                    classFeatureRepository.getClassEntity(idx)?.let { idx to it }
                }.toMap()

                val classLevel = bakedDraft.levelStack.take(levelIndex + 1).count { it.classIndex == levelStep.classIndex }
                val abilityMod = calculator.calculateRelevantAbilityModifier(bakedDraft, classMetadata)
                val staticEquipment = if (levelIndex == 0) bakedDraft.baseInfo.staticEquipment else emptyList()

                getClassProgressionDataUseCase(
                    draft = bakedDraft,
                    classIndex = levelStep.classIndex,
                    level = classLevel,
                    subclassIndex = levelStep.subclassIndex,
                    abilityModifier = abilityMod,
                    additionalIndexes = staticEquipment,
                    proficiencyProvider = { bakedDraft.getAllProficienciesWithLevels() },
                    editingLevelIndex = levelIndex
                )
            } else null
        }

        val bgDataDef = async { if (bakedDraft.baseInfo.backgroundIndex.isNotBlank()) getBackgroundDataUseCase(bakedDraft.baseInfo.backgroundIndex) else null }
        val validationDef = async { validateDraftUseCase(bakedDraft) }

        val raceFeats = raceFeatsDef.await(); val subFeats = subFeatsDef.await(); val classData = classDataDef.await(); val bgData = bgDataDef.await(); val validationReport = validationDef.await()
        val totalFeatures = raceFeats + subFeats + (classData?.partitionedFeatures?.classSkillFeatures ?: emptyList()) + (bgData?.background?.features ?: emptyList())
        val registry = buildMetadataRegistry(bakedDraft, totalFeatures)

        _uiState.update { currentState ->
            currentState.copy(
                draft = bakedDraft, isLoading = false, editingLevelIndex = levelIndex,
                availableRaces = injectedRefs?.races ?: currentState.availableRaces,
                availableClasses = injectedRefs?.classes ?: currentState.availableClasses,
                availableBackgrounds = injectedRefs?.backgrounds ?: currentState.availableBackgrounds,
                availableAlignments = injectedRefs?.alignments ?: currentState.availableAlignments,
                baseRaceFeatures = raceFeats, subraceFeatures = subFeats,
                availableSubraces = if (bakedDraft.baseInfo.raceIndex.isNotBlank()) (injectedRefs?.races ?: currentState.availableRaces).find { it.index == bakedDraft.baseInfo.raceIndex }?.let { libraryRepository.getSubracesFromDb(it.id) } ?: emptyList() else emptyList(),
                classStepFeatures = classData?.partitionedFeatures?.classSkillFeatures ?: emptyList(),
                inventoryStepFeatures = if (levelIndex == 0) classData?.partitionedFeatures?.inventoryChoiceFeatures ?: emptyList() else emptyList(),
                subclassChoiceFeature = classData?.partitionedFeatures?.subclassChoiceFeature,
                availableSubclasses = if (levelStep != null) libraryRepository.getSubclassesForClass(levelStep.classIndex) else emptyList(),
                aggregatedSpellFeature = classData?.aggregatedSpellFeature, backgroundFeatures = bgData?.background?.features ?: emptyList(),
                unpackedEquipmentOptions = currentState.unpackedEquipmentOptions + (classData?.unpackedEquipmentOptions ?: emptyMap()) + (bgData?.unpackedEquipment ?: emptyMap()),
                featMetadataRegistry = registry, validationIssues = validationReport.issues,
                tabErrors = validationReport.issues.groupBy { it.tabIndex }.mapValues { it.value.isNotEmpty() },
                proficiencyExclusions = mapOf(1 to bakedDraft.getProficiencyExclusions(1), 2 to bakedDraft.getProficiencyExclusions(2))
            )
        }
    }

    fun selectLevelToEdit(index: Int) {
        if (index in _uiState.value.draft.levelStack.indices) {
            _uiState.update { it.copy(editingLevelIndex = index) }
            viewModelScope.launch { runBakeAndValidate(_uiState.value.draft, forcedLevelIndex = index) }
        }
    }

    fun handleSelection(source: SelectionSource, key: String, res: ChoiceResult) {
        viewModelScope.launch { selectionMutex.withLock {
            val newDraft = handleSelectionUseCase(_uiState.value.draft, source, key, res, if (source == SelectionSource.CLASS) _uiState.value.editingLevelIndex else null)
            runBakeAndValidate(newDraft)
        }}
    }

    fun updateHpIncrease(value: Int) {
        viewModelScope.launch { selectionMutex.withLock {
            val idx = _uiState.value.editingLevelIndex; val stack = _uiState.value.draft.levelStack.toMutableList()
            if (idx in stack.indices && stack[idx].hpIncrease != value) {
                stack[idx] = stack[idx].copy(hpIncrease = value)
                runBakeAndValidate(_uiState.value.draft.copy(levelStack = stack))
            }
        }}
    }

    fun addLevel() {
        viewModelScope.launch { selectionMutex.withLock {
            val lastClass = _uiState.value.draft.levelStack.lastOrNull()?.classIndex ?: return@launch
            val newStack = _uiState.value.draft.levelStack + LevelStep(classIndex = lastClass)
            runBakeAndValidate(_uiState.value.draft.copy(levelStack = newStack), forcedLevelIndex = newStack.lastIndex)
        }}
    }

    fun removeLastLevel() {
        viewModelScope.launch { selectionMutex.withLock {
            if (_uiState.value.draft.levelStack.size <= 1) return@launch
            val newStack = _uiState.value.draft.levelStack.dropLast(1)
            runBakeAndValidate(_uiState.value.draft.copy(levelStack = newStack), forcedLevelIndex = newStack.lastIndex)
        }}
    }

    fun setClassForCurrentLevel(classIdx: String) {
        viewModelScope.launch { selectionMutex.withLock {
            val idx = _uiState.value.editingLevelIndex; val stack = _uiState.value.draft.levelStack.toMutableList()
            if (idx in stack.indices && stack[idx].classIndex != classIdx) {
                stack[idx] = LevelStep(classIndex = classIdx)
                runBakeAndValidate(_uiState.value.draft.copy(levelStack = stack))
            }
        }}
    }

    fun toggleExpandedState(key: String) {
        _uiState.update { state -> state.copy(expandedStates = state.expandedStates.put(key, !(state.expandedStates[key] ?: false))) }
    }

    fun selectRace(idx: String) {
        if (_uiState.value.isEditMode) return
        viewModelScope.launch { selectionMutex.withLock {
            runBakeAndValidate(_uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(raceIndex = idx, subraceIndex = null, raceSelections = emptyMap())))
        }}
    }

    fun selectSubrace(idx: String) {
        if (_uiState.value.isEditMode) return
        viewModelScope.launch { selectionMutex.withLock {
            runBakeAndValidate(_uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(subraceIndex = idx)))
        }}
    }

    fun selectClass(idx: String) {
        if (_uiState.value.isEditMode) return
        viewModelScope.launch { selectionMutex.withLock {
            runBakeAndValidate(_uiState.value.draft.copy(levelStack = listOf(LevelStep(classIndex = idx)), baseInfo = _uiState.value.draft.baseInfo.copy(inventorySelections = emptyMap(), startingClassIndex = idx)))
        }}
    }

    fun selectSubclass(idx: String) { viewModelScope.launch { selectionMutex.withLock {
        val stack = _uiState.value.draft.levelStack.toMutableList(); val i = _uiState.value.editingLevelIndex
        if (i in stack.indices) {
            stack[i] = stack[i].copy(subclassIndex = idx)
            runBakeAndValidate(_uiState.value.draft.copy(levelStack = stack))
        }
    }}}

    fun selectBackground(bg: Background) {
        if (_uiState.value.isEditMode) return
        viewModelScope.launch { selectionMutex.withLock {
            runBakeAndValidate(_uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(backgroundIndex = bg.index, backgroundSelections = emptyMap())))
        }}
    }

    fun onRaceSelectionChange(key: String, res: ChoiceResult) = handleSelection(SelectionSource.RACE, key, res)
    fun onClassSelectionChange(key: String, res: ChoiceResult) = handleSelection(SelectionSource.CLASS, key, res)
    fun onBgSelectionChange(key: String, res: ChoiceResult) = handleSelection(SelectionSource.BACKGROUND, key, res)
    fun onInventorySelectionChange(key: String, res: ChoiceResult) = handleSelection(SelectionSource.INVENTORY, key, res)

    fun updateStat(key: String, diff: Int) {
        viewModelScope.launch { selectionMutex.withLock {
            runBakeAndValidate(updateStatUseCase(_uiState.value.draft, key, diff))
        }}
    }

    fun saveCharacter(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentDraft = _uiState.value.draft
            Log.d(
                TAG,
                "SAVE_CLICK draft id=${currentDraft.id} name='${currentDraft.name}' " +
                    "levels=${currentDraft.levelStack.size} class='${currentDraft.baseInfo.startingClassIndex}' " +
                    "race='${currentDraft.baseInfo.raceIndex}' bg='${currentDraft.baseInfo.backgroundIndex}'"
            )
            val report = validateDraftUseCase(currentDraft)

            if (report.isValid) {
                Log.d(TAG, "SAVE_PIPELINE_V2 before commit valid=true")
                characterRepository.commitFullCharacter(currentDraft)
                    .onSuccess {
                        Log.d(TAG, "SAVE_PIPELINE_V2 after commit success=true")
                        Log.d(TAG, "Character successfully committed.")
                        onSuccess()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "SAVE_PIPELINE_V2 after commit success=false", error)
                        Log.e(TAG, "Failed to commit character", error)
                        _uiState.update { it.copy(interactionError = "Ошибка сохранения", isLoading = false) }
                    }
            } else {
                Log.d(TAG, "SAVE_PIPELINE_V2 before commit valid=false issues=${report.issues.size}")
                _uiState.update { it.copy(interactionError = "Заполните все обязательные поля", isLoading = false) }
            }
        }
    }

    private fun updateBaseInfo(transform: (BaseInfo) -> BaseInfo) = viewModelScope.launch { selectionMutex.withLock {
        val draft = _uiState.value.draft
        runBakeAndValidate(draft.copy(baseInfo = transform(draft.baseInfo)))
    }}

    fun updateName(n: String) = _uiState.update { it.copy(draft = it.draft.copy(name = n)) }
    fun updateGender(value: String) = updateBaseInfo { it.copy(gender = value) }
    fun updatePersonalityTrait(value: String) = _uiState.update {
        it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(personalityTrait = value)))
    }
    fun updateIdeal(value: String) = _uiState.update {
        it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(ideal = value)))
    }
    fun updateBond(value: String) = _uiState.update {
        it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(bond = value)))
    }
    fun updateFlaw(value: String) = _uiState.update {
        it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(flaw = value)))
    }
    fun updateAppearance(value: String) = _uiState.update {
        it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(appearance = value)))
    }
    fun updateBackstory(value: String) = _uiState.update {
        it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(backstory = value)))
    }

    fun selectAlignment(idx: String) = viewModelScope.launch { selectionMutex.withLock {
        runBakeAndValidate(_uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(alignmentIndex = idx)))
    }}

    fun setInventoryMode(mode: InventoryMode) = inventoryHandler.setInventoryMode(mode)
    fun selectShopCategory(category: ShopCategory) = inventoryHandler.selectShopCategory(category)
    fun goBackInShop() = inventoryHandler.goBackInShop()
    fun searchShop(query: String) = inventoryHandler.searchShop(query)
    fun addItemToCart(item: ShopItem) = inventoryHandler.addItemToCart(item)
    fun removeItemFromCart(item: ShopItem) = inventoryHandler.removeItemFromCart(item)
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\CharacterCreatorViewModel.kt

