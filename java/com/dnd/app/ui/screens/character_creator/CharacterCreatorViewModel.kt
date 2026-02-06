// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.rules.DndRules
import com.dnd.app.domain.usecase.*
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.capitalizeFirst
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.ArrayDeque
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CharacterCreatorViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val characterRepository: CharacterRepository,
    private val assembler: CharacterAssembler,
    private val updateStatUseCase: UpdateStatUseCase,
    private val bakeCharacterUseCase: BakeCharacterUseCase,
    private val getClassProgressionDataUseCase: GetClassProgressionDataUseCase,
    private val handleSelectionUseCase: HandleSelectionUseCase, // [НОВЫЙ USE CASE - ЭТАП 6]
    private val inventoryHandler: InventoryHandler,
    private val calculator: DndCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorUiState())
    val uiState = _uiState.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }
    private val TAG = "DND_LOG_CC_VM"


    init {
        inventoryHandler.initialize(viewModelScope, _uiState)
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val racesDef = async { libraryRepository.getAllParentRaces() }
            val classesDef = async { libraryRepository.getAllClasses() }
            val backgroundsDef = async { libraryRepository.getAllBackgrounds() }
            val alignmentsDef = async { libraryRepository.getAllAlignments() }

            _uiState.update { it.copy(
                availableRaces = racesDef.await(),
                availableClasses = classesDef.await(),
                availableBackgrounds = backgroundsDef.await(),
                availableAlignments = alignmentsDef.await(),
                isLoading = false
            )}
        }
    }

    fun selectRace(idx: String) {
        viewModelScope.launch {
            val r = _uiState.value.availableRaces.find { it.index == idx } ?: return@launch
            val features = libraryRepository.getBaseRaceFeatures(r.id)
            val subraces = libraryRepository.getSubracesFromDb(r.id)

            val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(
                raceIndex = idx, subraceIndex = null, raceSelections = emptyMap()
            ))

            val finalDraft = bakeCharacterUseCase(newDraft)
            _uiState.update { it.copy(
                draft = finalDraft,
                globalExclusions = finalDraft.getProficiencyExclusions(),
                baseRaceFeatures = features, availableSubraces = subraces,
                subraceFeatures = emptyList(), selectedFeatDetails = null
            )}
        }
    }

    fun selectSubrace(idx: String) {
        viewModelScope.launch {
            val features = libraryRepository.getSubraceFeatures(idx)
            val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(subraceIndex = idx))

            val finalDraft = bakeCharacterUseCase(newDraft)
            _uiState.update { it.copy(
                draft = finalDraft,
                globalExclusions = finalDraft.getProficiencyExclusions(),
                subraceFeatures = features
            )}
        }
    }

    fun selectClass(idx: String) {
        viewModelScope.launch {
            val newDraft = _uiState.value.draft.copy(
                levelStack = listOf(LevelStep(classIndex = idx)),
                baseInfo = _uiState.value.draft.baseInfo.copy(
                    inventorySelections = emptyMap() // Сбрасываем выбор инвентаря при смене класса
                )
            )
            val finalDraft = bakeCharacterUseCase(newDraft)

            val abilityModifier = calculator.calculateRelevantAbilityModifier(finalDraft)
            val progressionData = getClassProgressionDataUseCase(idx, 1, null, abilityModifier)
            val availableSubclasses = libraryRepository.getSubclassesForClass(idx)

            _uiState.update {
                it.copy(
                    draft = finalDraft,
                    globalExclusions = finalDraft.getProficiencyExclusions(),
                    classStepFeatures = progressionData.partitionedFeatures.classSkillFeatures,
                    inventoryStepFeatures = progressionData.partitionedFeatures.inventoryChoiceFeatures,
                    subclassChoiceFeature = progressionData.partitionedFeatures.subclassChoiceFeature,
                    aggregatedSpellFeature = progressionData.aggregatedSpellFeature,
                    availableSubclasses = availableSubclasses,
                    inventoryMode = InventoryMode.STANDARD_PACKS,
                    shoppingCart = emptyList(),
                    unpackedEquipmentOptions = progressionData.unpackedEquipmentOptions
                )
            }
        }
    }

    fun selectSubclass(idx: String) {
        viewModelScope.launch {
            val stack = _uiState.value.draft.levelStack.toMutableList()
            if(stack.isEmpty()) return@launch
            stack[0] = stack[0].copy(subclassIndex = idx)
            val newDraft = _uiState.value.draft.copy(levelStack = stack)
            val finalDraft = bakeCharacterUseCase(newDraft)

            val curClass = finalDraft.levelStack.first().classIndex
            val abilityModifier = calculator.calculateRelevantAbilityModifier(finalDraft)
            val progressionData = getClassProgressionDataUseCase(curClass, 1, idx, abilityModifier)

            _uiState.update { it.copy(
                draft = finalDraft,
                globalExclusions = finalDraft.getProficiencyExclusions(),
                classStepFeatures = progressionData.partitionedFeatures.classSkillFeatures,
                inventoryStepFeatures = progressionData.partitionedFeatures.inventoryChoiceFeatures,
                subclassChoiceFeature = progressionData.partitionedFeatures.subclassChoiceFeature,
                aggregatedSpellFeature = progressionData.aggregatedSpellFeature
            )}
        }
    }

    fun selectBackground(bg: Background) {
        viewModelScope.launch {
            val newDraft = _uiState.value.draft.copy(
                baseInfo = _uiState.value.draft.baseInfo.copy(
                    backgroundIndex = bg.name,
                    backgroundSelections = emptyMap()
                )
            )

            val finalDraft = bakeCharacterUseCase(newDraft)
            _uiState.update { it.copy(
                draft = finalDraft,
                globalExclusions = finalDraft.getProficiencyExclusions(),
                backgroundFeatures = bg.features
            )}
        }
    }

    // --- [НАЧАЛО БЛОКА ИЗМЕНЕНИЙ - ЭТАП 6] ---

    // Публичные методы теперь просто делегируют вызов приватному обработчику
    fun onRaceSelectionChange(key: String, res: ChoiceResult) = handleSelection(HandleSelectionUseCase.SelectionSource.RACE, key, res)
    fun onClassSelectionChange(key: String, res: ChoiceResult) = handleSelection(HandleSelectionUseCase.SelectionSource.CLASS, key, res)
    fun onBgSelectionChange(key: String, res: ChoiceResult) = handleSelection(HandleSelectionUseCase.SelectionSource.BACKGROUND, key, res)
    fun onInventorySelectionChange(key: String, res: ChoiceResult) = handleSelection(HandleSelectionUseCase.SelectionSource.INVENTORY, key, res)

    /**
     * Единый обработчик для всех типов выборов.
     * 1. Вызывает HandleSelectionUseCase для корректной обработки зависимостей (например, очистки под-выборов).
     * 2. Обновляет состояние UI, если был выбран новый основной фит (только для расы).
     * 3. Вызывает BakeCharacterUseCase для пересчета всего черновика.
     * 4. Обновляет финальное состояние UI.
     */
    private fun handleSelection(source: HandleSelectionUseCase.SelectionSource, key: String, res: ChoiceResult) {
        viewModelScope.launch {
            // Шаг 1: Делегируем сложную логику обработки выбора UseCase'у
            val intermediateDraft = handleSelectionUseCase(_uiState.value.draft, source, key, res)

            // Шаг 2: Обновляем детали выбранной черты (логика UI, остается в VM)
            if (source == HandleSelectionUseCase.SelectionSource.RACE) {
                val isPrimaryFeatSelection = !key.contains("_") && res is ChoiceResult.SelectedOptions && res.items.firstOrNull()?.startsWith("feat-") == true
                if (isPrimaryFeatSelection) {
                    val newFeatIndex = (res as? ChoiceResult.SelectedOptions)?.items?.firstOrNull()
                    val oldFeatDetails = _uiState.value.selectedFeatDetails
                    if (newFeatIndex != oldFeatDetails?.index) {
                        val newFeatDetails = if (newFeatIndex != null) libraryRepository.getFeatureByIndex(newFeatIndex) else null
                        _uiState.update { it.copy(selectedFeatDetails = newFeatDetails) }
                    }
                }
            }

            // Шаг 3: "Запекаем" черновик после всех изменений
            val finalDraft = bakeCharacterUseCase(intermediateDraft)

            // Шаг 4: Обновляем UI
            _uiState.update { it.copy(
                draft = finalDraft,
                globalExclusions = finalDraft.getProficiencyExclusions()
            )}
        }
    }

    // --- [КОНЕЦ БЛОКА ИЗМЕНЕНИЙ - ЭТАП 6] ---


    // --- ПРОЧИЕ МЕТОДЫ ---
    fun rollCharacterTrait(type: String) {
        val bgName = _uiState.value.draft.baseInfo.backgroundIndex
        val bg = _uiState.value.availableBackgrounds.find { it.name == bgName } ?: return
        val list = when(type) { "personality" -> bg.personalityTraits; "ideal" -> bg.ideals; "bond" -> bg.bonds; "flaw" -> bg.flaws; else -> emptyList() }
        if (list.isNotEmpty()) updateBioField(type, list.random())
    }
    fun updateBioField(type: String, v: String) {
        val b = _uiState.value.draft.baseInfo
        val nb = when(type) { "personality" -> b.copy(personalityTrait = v); "ideal" -> b.copy(ideal = v); "bond" -> b.copy(bond = v); "flaw" -> b.copy(flaw = v); else -> b }
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = nb)) }
    }
    fun updateName(n: String) { _uiState.update { it.copy(draft = it.draft.copy(name = n)) } }
    fun selectAlignment(idx: String) { _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(alignmentIndex = idx))) } }

    fun updateStat(key: String, diff: Int) {
        viewModelScope.launch {
            val currentDraft = _uiState.value.draft
            val newDraft = updateStatUseCase(currentDraft, key, diff)

            if (newDraft != currentDraft) {
                val finalDraft = bakeCharacterUseCase(newDraft)
                _uiState.update { it.copy(
                    draft = finalDraft,
                    globalExclusions = finalDraft.getProficiencyExclusions()
                )}

                val currentClassIndex = finalDraft.levelStack.firstOrNull()?.classIndex
                if (currentClassIndex != null) {
                    val primaryStat = calculator.getPrimaryCastingStat(currentClassIndex)
                    if (key == primaryStat) {
                        refreshClassFeatures()
                    }
                }
            }
        }
    }

    fun saveCharacter(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val domain = assembler.assemble(_uiState.value.draft)
            (characterRepository as com.dnd.app.data.repository.CharacterRepositoryImpl).saveFullCharacter(domain, _uiState.value.draft)
            onSuccess()
        }
    }

    // --- [НАЧАЛО БЛОКА ДЕЛЕГИРОВАНИЯ - ЭТАП 5] ---
    fun setInventoryMode(mode: InventoryMode) = inventoryHandler.setInventoryMode(mode)
    fun selectShopCategory(category: ShopCategory) = inventoryHandler.selectShopCategory(category)
    fun goBackInShop() = inventoryHandler.goBackInShop()
    fun searchShop(query: String) = inventoryHandler.searchShop(query)
    fun addItemToCart(item: ShopItem) = inventoryHandler.addItemToCart(item)
    fun removeItemFromCart(item: ShopItem) = inventoryHandler.removeItemFromCart(item)
    // --- [КОНЕЦ БЛОКА ДЕЛЕГИРОВАНИЯ] ---

    private fun refreshClassFeatures() {
        viewModelScope.launch {
            val draft = _uiState.value.draft
            val classIndex = draft.levelStack.firstOrNull()?.classIndex ?: return@launch
            val subclassIndex = draft.levelStack.firstOrNull()?.subclassIndex
            val abilityModifier = calculator.calculateRelevantAbilityModifier(draft)

            val progressionData = getClassProgressionDataUseCase(classIndex, 1, subclassIndex, abilityModifier)

            val preparedSpellsFeature = progressionData.partitionedFeatures.classSkillFeatures.find { it.index == "virtual-prepared-spells" }
            val newLimit = (preparedSpellsFeature?.choices?.firstOrNull() as? FeatureChoiceDomain.SelectSpell)?.count ?: 0
            val currentSelections = draft.levelStack.firstOrNull()?.selections.orEmpty().toMutableMap()
            val currentPreparedSpells = (currentSelections["virtual-prepared-spells"] as? ChoiceResult.Spells)?.spellIndexes.orEmpty()

            if (currentPreparedSpells.size > newLimit) {
                currentSelections["virtual-prepared-spells"] = ChoiceResult.Spells(currentPreparedSpells.take(newLimit))
            }

            val newLevelStack = draft.levelStack.toMutableList()
            if (newLevelStack.isNotEmpty()) {
                newLevelStack[0] = newLevelStack[0].copy(selections = currentSelections)
            }
            val newDraft = draft.copy(levelStack = newLevelStack)

            _uiState.update {
                it.copy(
                    draft = newDraft,
                    classStepFeatures = progressionData.partitionedFeatures.classSkillFeatures,
                    subclassChoiceFeature = progressionData.partitionedFeatures.subclassChoiceFeature,
                    aggregatedSpellFeature = progressionData.aggregatedSpellFeature
                )
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt