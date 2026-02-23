// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\level_up\LevelUpViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.level_up

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.rules.TechnicalIdFilter
import com.dnd.app.domain.usecase.*
import com.dnd.app.domain.usecase.level_up.ValidateLevelUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LevelUpViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val characterRepository: CharacterRepository,
    private val bakeCharacterUseCase: BakeCharacterUseCase,
    private val getClassProgressionDataUseCase: GetClassProgressionDataUseCase,
    private val handleSelectionUseCase: HandleSelectionUseCase,
    private val calculator: DndCalculator,
    private val validateLevelUpUseCase: ValidateLevelUpUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private companion object {
        private const val MAX_CHOICE_DEPTH = 5
    }

    private val characterId: Long = savedStateHandle.get<Long>("characterId") ?: 0L
    private val _uiState = MutableStateFlow(LevelUpUiState())
    val uiState = _uiState.asStateFlow()
    private val selectionMutex = Mutex()
    private val cachedFeatures = mutableMapOf<String, Feature>()
    private val TAG = "LevelUpViewModel"

    init {
        loadCharacterForLevelUp()
    }

    private fun loadCharacterForLevelUp() {
        if (characterId == 0L) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val existingDraft = characterRepository.getDraftById(characterId)
            if (existingDraft == null || existingDraft.levelStack.isEmpty()) {
                triggerError("Не удалось загрузить черновик.")
                return@launch
            }

            val lastClassIndex = existingDraft.levelStack.last().classIndex
            val draftForLevelUp = existingDraft.copy(levelStack = existingDraft.levelStack + LevelStep(classIndex = lastClassIndex))

            val baked = bakeCharacterUseCase(draftForLevelUp)
            updateStateWithData(baked)
        }
    }


    private suspend fun buildMetadataRegistry(draft: DraftCharacter, totalFeatures: List<Feature>): Map<String, Feature> = coroutineScope {
        fun normalizeId(value: String): String = value.trim().lowercase()
        fun isFeatureLike(value: String): Boolean {
            val normalized = normalizeId(value)
            if (normalized.isBlank()) return false
            if (normalized.all { it.isDigit() }) return false
            return normalized.any { it.isLetter() }
        }

        val resolved = mutableMapOf<String, Feature>()
        val resolvedIds = mutableSetOf<String>()
        val idsToScan = mutableSetOf<String>()
        val eagerSessionIds = mutableSetOf<String>()
        var maxChoiceDepth = 0
        var maxChoicePath: String? = null

        fun registerCandidate(rawId: String) {
            if (!isFeatureLike(rawId)) return
            val normalized = normalizeId(rawId)
            if (TechnicalIdFilter.shouldSkip(normalized)) {
                Log.d(TAG, "Skipping technical candidate -> $normalized (raw=$rawId)")
                return
            }
            idsToScan.add(normalized)
            Log.d(TAG, "registerCandidate -> $normalized (raw=$rawId)")
        }

        fun registerFeatId(rawId: String) {
            val normalized = normalizeId(rawId)
            if (normalized.isBlank()) return
            if (TechnicalIdFilter.shouldSkip(normalized)) {
                Log.d(TAG, "Skipping technical feat id -> $normalized")
                return
            }
            idsToScan.add(normalized)
            Log.d(TAG, "registerFeatId -> $normalized")
        }

        fun choiceLabel(choice: FeatureChoiceDomain): String {
            val desc = when (choice) {
                is FeatureChoiceDomain.SelectOption -> choice.description
                is FeatureChoiceDomain.SelectStatBonus -> "SelectStatBonus"
                is FeatureChoiceDomain.SelectSpell -> "SelectSpell"
                is FeatureChoiceDomain.SelectSkill -> "SelectSkill"
                is FeatureChoiceDomain.SelectExpertise -> "SelectExpertise"
                is FeatureChoiceDomain.InvalidChoice -> "InvalidChoice"
            }
            return desc?.takeIf { it.isNotBlank() } ?: choice::class.simpleName.orEmpty()
        }

        fun cacheIfNeeded(feature: Feature) {
            val key = feature.index.trim().lowercase()
            if (key.isNotBlank()) {
                cachedFeatures[key] = feature
            }
        }

        fun registerChoiceOptions(choice: FeatureChoiceDomain, depth: Int = 1, parentPath: String = "") {
            val currentLabel = choiceLabel(choice)
            val currentPath = if (parentPath.isBlank()) currentLabel else "$parentPath/$currentLabel"
            if (depth > MAX_CHOICE_DEPTH) {
                Log.d(TAG, "Drop path -> $currentPath at depth=$depth")
                return
            }
            if (depth > maxChoiceDepth) {
                maxChoiceDepth = depth
                maxChoicePath = currentPath
                Log.d(TAG, "buildMetadataRegistry choice depth=$depth path=$currentPath")
            }

            choice.options.forEach { opt ->
                if (opt.kind == ProficiencyKind.FEAT) {
                    registerFeatId(opt.id)
                }
                registerCandidate(opt.id)
                opt.subChoice?.let { registerChoiceOptions(it, depth + 1, "$currentPath/${opt.id}") }
            }
        }

        fun scanDraft(selections: Map<String, ChoiceResult>) {
            selections.values.filterIsInstance<ChoiceResult.SelectedOptions>().forEach { result ->
                result.items.forEach { itemId ->
                    registerCandidate(itemId)
                    if (isFeatureLike(itemId)) {
                        eagerSessionIds.add(normalizeId(itemId))
                    }
                }
            }
        }
        Log.d(TAG, "Recursive metadata resolution started. Initial pool: ${totalFeatures.size}")
        Log.d("RALPH", "LevelUp buildMetadataRegistry: levels=${draft.levelStack.size}, totalFeatures=${totalFeatures.size}")
        draft.levelStack.forEach { scanDraft(it.selections) }
        draft.baseInfo.raceSelections.let { scanDraft(it) }
        draft.baseInfo.backgroundSelections.let { scanDraft(it) }
        draft.baseInfo.inventorySelections.let { scanDraft(it) }

        // Eager preload: scan all options from provided totalFeatures (current step features)
        fun registerFeature(feature: Feature) {
            val normalizedIndex = normalizeId(feature.index)
            if (feature.index.isNotBlank()) {
                resolved[feature.index] = feature
                resolvedIds.add(feature.index)
            }
            if (normalizedIndex.isNotBlank()) {
                resolved[normalizedIndex] = feature
                resolvedIds.add(normalizedIndex)
            }
            cacheIfNeeded(feature)
        }

        totalFeatures.forEach { registerFeature(it) }
        if (eagerSessionIds.isNotEmpty()) {
            val eagerFetches = eagerSessionIds.map { id ->
                async {
                    runCatching {
                        libraryRepository.getFeatureByIndex(id)
                    }.onFailure { e ->
                        Log.e("RALPH", "Failed to eagerly load selected feature '$id'.", e)
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
            if (eagerFetches.isNotEmpty()) {
                Log.d("RALPH", "Eagerly loaded ${eagerFetches.size} selected features: ${eagerSessionIds.joinToString(", ")}")
                eagerFetches.forEach { registerFeature(it) }
            }
        }

        cachedFeatures.values.forEach { cached ->
            registerFeature(cached)
        }

        totalFeatures.forEach { feat ->
            cacheIfNeeded(feat)
            feat.choices.forEach { choice ->
                registerChoiceOptions(choice, 1, feat.index)
            }
            feat.grantedProficiencies.forEach { registerCandidate(it) }
        }

        Log.d(TAG, "Eager preload added ${idsToScan.size} candidate ids: ${idsToScan.take(20)}")

        var iteration = 0

        while (iteration < 10) {

            resolved.values.forEach { feat ->
                feat.choices.forEach { choice ->
                    registerChoiceOptions(choice, 1, feat.index)
                }

                feat.grantedProficiencies.forEach { registerCandidate(it) }
            }


            val missing = idsToScan.filter { it !in resolvedIds }
            if (missing.isEmpty()) break
            Log.d(TAG, "Iteration $iteration: Fetching ${missing.size} missing features (sample: ${missing.take(5)})")
            Log.d("RALPH", "LevelUp metadata iteration $iteration fetching ${missing.size} ids")

            val (fromCache, toFetch) = missing.partition { cachedFeatures.containsKey(it) }
            fromCache.forEach { cachedId ->
                registerFeature(cachedFeatures[cachedId]!!)
            }
            if (toFetch.isEmpty()) break

            val fetched = toFetch.map { id ->
                async {
                    runCatching {
                        libraryRepository.getFeatureByIndex(id)
                    }.onFailure { e ->
                        Log.e(TAG, "Failed to resolve feature '$id' in recursion.", e)
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()

            if (fetched.isEmpty()) break

            fetched.forEach { feat ->
                registerFeature(feat)
            }
            iteration++
        }

        Log.d(TAG, "buildMetadataRegistry deepest choice depth: $maxChoiceDepth path=${maxChoicePath ?: "unknown"}")
        resolved
    }

    private fun isSelectedInDraft(draft: DraftCharacter, id: String): Boolean {
        return draft.levelStack.any { step ->
            step.selections.values.any { res ->
                res is ChoiceResult.SelectedOptions && res.items.contains(id)
            }
        }
    }


    private suspend fun updateStateWithData(draft: DraftCharacter) = coroutineScope {
        val levelIndex = draft.levelStack.lastIndex
        val levelStep = draft.levelStack[levelIndex]
        val classLevel = draft.levelStack.count { it.classIndex == levelStep.classIndex }

        val uniqueClassIndices = draft.levelStack.map { it.classIndex }.distinct()
        val classMetadataMap = uniqueClassIndices.mapNotNull { idx ->
            libraryRepository.getClassEntityByIndex(idx)?.let { idx to it }
        }.toMap()

        val classDataDef = async {
            val abilityMod = calculator.calculateRelevantAbilityModifier(draft, classMetadataMap)
            getClassProgressionDataUseCase(
                draft = draft,
                classIndex = levelStep.classIndex,
                level = classLevel,
                subclassIndex = levelStep.subclassIndex,
                abilityModifier = abilityMod,
                proficiencyProvider = { draft.getAllProficienciesWithLevels() },
                editingLevelIndex = levelIndex
            )
        }

        val validationDef = async { validateLevelUpUseCase(draft, levelIndex) }
        val allAvailableClassesDef = async { libraryRepository.getAllClasses() }

        val classData = classDataDef.await()
        val validationReport = validationDef.await()
        val allAvailableClasses = allAvailableClassesDef.await()


        val allFeaturesFromProgression = mutableListOf<Feature>()
        allFeaturesFromProgression.addAll(classData.partitionedFeatures.classSkillFeatures)
        classData.partitionedFeatures.subclassChoiceFeature?.let { allFeaturesFromProgression.add(it) }
        classData.partitionedFeatures.inventoryChoiceFeatures.forEach { allFeaturesFromProgression.add(it) }
        classData.aggregatedSpellFeature?.let { allFeaturesFromProgression.add(it) }

        val registry = buildMetadataRegistry(draft, allFeaturesFromProgression)
        val sampleRegistryKeys = registry.keys.take(50).joinToString(",")
        Log.d(TAG, "featMetadataRegistry: size=${registry.size}; keys=$sampleRegistryKeys")
        libraryRepository.publishFeatMetadataRegistry(registry)

        _uiState.update {
            it.copy(
                draft = draft,
                isLoading = false,
                currentClassInfo = allAvailableClasses.find { c -> c.index == levelStep.classIndex },
                availableClasses = allAvailableClasses,
                classStepFeatures = classData.partitionedFeatures.classSkillFeatures,
                availableSubclasses = libraryRepository.getSubclassesForClass(levelStep.classIndex),
                subclassChoiceFeature = classData.partitionedFeatures.subclassChoiceFeature,
                aggregatedSpellFeature = classData.aggregatedSpellFeature,
                validationIssues = validationReport.issues,
                featMetadataRegistry = registry,
                proficiencyExclusions = mapOf(
                    1 to draft.getProficiencyExclusions(1, null),
                    2 to draft.getProficiencyExclusions(2, null)
                )
            )
        }
    }

    fun handleSelection(key: String, result: ChoiceResult) {
        viewModelScope.launch {
            selectionMutex.withLock {
                val currentDraft = _uiState.value.draft
                val newDraft = handleSelectionUseCase(
                    currentDraft,
                    SelectionSource.CLASS,
                    key,
                    result,
                    currentDraft.levelStack.lastIndex
                )
                updateStateWithData(newDraft)
            }
        }
    }

    fun updateHpIncrease(value: Int) {
        viewModelScope.launch {
            selectionMutex.withLock {
                val draft = _uiState.value.draft
                val stack = draft.levelStack.toMutableList()
                val idx = stack.lastIndex
                if (stack[idx].hpIncrease != value) {
                    stack[idx] = stack[idx].copy(hpIncrease = value)
                    updateStateWithData(draft.copy(levelStack = stack))
                }
            }
        }
    }

    fun selectSubclass(subclassIndex: String) {
        viewModelScope.launch {
            selectionMutex.withLock {
                val draft = _uiState.value.draft
                val stack = draft.levelStack.toMutableList()
                val idx = stack.lastIndex
                stack[idx] = stack[idx].copy(subclassIndex = subclassIndex)
                cachedFeatures.clear()
                updateStateWithData(draft.copy(levelStack = stack))
            }
        }
    }

    fun setClassForCurrentLevel(classIdx: String) {
        viewModelScope.launch {
            selectionMutex.withLock {
                val draft = _uiState.value.draft
                val stack = draft.levelStack.toMutableList()
                val idx = stack.lastIndex
                if (idx in stack.indices && stack[idx].classIndex != classIdx) {
                    stack[idx] = LevelStep(classIndex = classIdx)
                    cachedFeatures.clear()
                    updateStateWithData(draft.copy(levelStack = stack))
                }
            }
        }
    }

    fun toggleExpandedState(key: String) {
        _uiState.update { it.copy(expandedStates = it.expandedStates.put(key, !(it.expandedStates[key] ?: false))) }
    }

    fun commitLevel(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val draftToCommit = bakeCharacterUseCase(_uiState.value.draft)
            val report = validateLevelUpUseCase(draftToCommit, draftToCommit.levelStack.lastIndex)

            if (report.isValid) {
                withContext(NonCancellable) {
                    characterRepository.commitFullCharacter(draftToCommit)
                        .onSuccess { withContext(Dispatchers.Main) { onSuccess() } }
                        .onFailure { error ->
                            triggerError("Ошибка БД: ${error.message}")
                            _uiState.update { it.copy(isLoading = false) }
                        }
                }
            } else {
                triggerError("Заполните обязательные поля текущего уровня.")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun triggerError(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(interactionError = message) }
            delay(3000)
            _uiState.update { it.copy(interactionError = null) }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\level_up\LevelUpViewModel.kt
