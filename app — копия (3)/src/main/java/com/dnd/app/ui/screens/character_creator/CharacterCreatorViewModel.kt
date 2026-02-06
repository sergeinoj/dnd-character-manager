// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.data.model.ReferenceJson
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.rules.DndRules
import com.dnd.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import kotlin.random.Random

data class CreatorUiState(
    val draft: DraftCharacter = DraftCharacter(),
    val availableRaces: List<Race> = emptyList(),
    val availableClasses: List<ClassInfo> = emptyList(),
    val availableBackgrounds: List<Background> = emptyList(),
    val availableAlignments: List<AlignmentEntity> = emptyList(),
    val baseRaceFeatures: List<Feature> = emptyList(),
    val subraceFeatures: List<Feature> = emptyList(),
    val backgroundFeatures: List<Feature> = emptyList(),
    val globalExclusions: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val selectedFeatDetails: Feature? = null,
    val aggregatedSpellFeature: Feature? = null,
    val availableSubraces: List<Race> = emptyList(),
    val availableSubclasses: List<SubclassInfo> = emptyList(),
    val classStepFeatures: List<Feature> = emptyList(),
    val inventoryStepFeatures: List<Feature> = emptyList(),
    val subclassChoiceFeature: Feature? = null,

    // --- Новые поля для магазина ---
    val inventoryMode: InventoryMode = InventoryMode.STANDARD_PACKS,
    val shopView: ShopView = ShopView.CATEGORIES,
    val shopCategories: List<ShopCategory> = emptyList(),
    val selectedShopCategory: ShopCategory? = null,
    val shopItems: List<ShopItem> = emptyList(),
    val shoppingCart: List<ShopItem> = emptyList(),
    val remainingGold: Money = Money(),
    val initialGold: Money = Money()
)

@HiltViewModel
class CharacterCreatorViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val characterRepository: CharacterRepository,
    private val assembler: CharacterAssembler,
    private val draftStatsUseCase: DraftStatsUseCase,
    private val spellChoiceAggregatorUseCase: SpellChoiceAggregatorUseCase,
    private val partitionClassFeaturesUseCase: PartitionClassFeaturesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorUiState())
    val uiState = _uiState.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }
    private var searchJob: Job? = null
    private val TAG = "DND_LOG_CC_VM"

    init { loadInitialData() }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val races = libraryRepository.getAllParentRaces()
            val classes = libraryRepository.getAllClasses()
            val backgrounds = libraryRepository.getAllBackgrounds()
            val alignments = libraryRepository.getAllAlignments()
            _uiState.update { it.copy(
                availableRaces = races, availableClasses = classes,
                availableBackgrounds = backgrounds, availableAlignments = alignments,
                isLoading = false
            )}
        }
    }

    fun selectRace(idx: String) {
        viewModelScope.launch {
            val r = _uiState.value.availableRaces.find { it.index == idx } ?: return@launch
            val features = libraryRepository.getBaseRaceFeatures(r.id)
            val subraces = libraryRepository.getSubracesFromDb(r.id)
            val baseProfsFromRace = r.baseProficiencies
            val profsFromFeatures = features.flatMap { it.grantedProficiencies }
            val allStaticProfs = (baseProfsFromRace + profsFromFeatures).distinct()

            val intermediateDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(
                raceIndex = idx, subraceIndex = null, raceSelections = emptyMap(),
                staticProficiencies = allStaticProfs
            ))
            val finalDraft = draftStatsUseCase(pruneInvalidSelections(intermediateDraft))

            _uiState.update { it.copy(
                draft = finalDraft, baseRaceFeatures = features, availableSubraces = subraces,
                subraceFeatures = emptyList(), selectedFeatDetails = null,
                globalExclusions = finalDraft.getProficiencyExclusions()
            )}
        }
    }

    private fun pruneInvalidSelections(draft: DraftCharacter): DraftCharacter {
        val hardExclusions = draft.getHardExclusions()
        if (hardExclusions.isEmpty()) return draft
        fun ChoiceResult.isStillValid(): Boolean = when (this) {
            is ChoiceResult.Skills -> this.skillIndexes.none { it in hardExclusions }
            is ChoiceResult.StatBonus -> this.bonuses.keys.none { it in hardExclusions }
            is ChoiceResult.SelectedOptions -> this.items.none { it in hardExclusions }
            else -> true
        }
        val cleanedRaceSelections = draft.baseInfo.raceSelections.filterValues { it.isStillValid() }
        val cleanedClassSelections = draft.levelStack.map { levelStep ->
            levelStep.copy(selections = levelStep.selections.filterValues { it.isStillValid() })
        }
        val cleanedBgSelections = draft.baseInfo.backgroundSelections.filterValues { it.isStillValid() }
        return draft.copy(
            baseInfo = draft.baseInfo.copy(
                raceSelections = cleanedRaceSelections,
                backgroundSelections = cleanedBgSelections
            ),
            levelStack = cleanedClassSelections
        )
    }

    fun selectSubrace(idx: String) {
        viewModelScope.launch {
            val features = libraryRepository.getSubraceFeatures(idx)
            val intermediateDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(subraceIndex = idx))
            val finalDraft = draftStatsUseCase(pruneInvalidSelections(intermediateDraft))
            _uiState.update { it.copy(
                draft = finalDraft, subraceFeatures = features,
                globalExclusions = finalDraft.getProficiencyExclusions()
            )}
        }
    }

    fun onRaceSelectionChange(key: String, res: ChoiceResult) {
        viewModelScope.launch {
            val currentSelections = _uiState.value.draft.baseInfo.raceSelections.toMutableMap()
            currentSelections[key] = res
            val isPrimaryFeatSelection = !key.contains("_") && res is ChoiceResult.SelectedOptions && res.items.firstOrNull()?.startsWith("feat-") == true

            if (isPrimaryFeatSelection) {
                val newFeatIndex = (res as? ChoiceResult.SelectedOptions)?.items?.firstOrNull()
                val oldFeatDetails = _uiState.value.selectedFeatDetails
                if (newFeatIndex != oldFeatDetails?.index) {
                    if (oldFeatDetails != null) currentSelections.keys.removeIf { it.startsWith("${key}_${oldFeatDetails.index}") }
                    val newFeatDetails = if (newFeatIndex != null) libraryRepository.getFeatureByIndex(newFeatIndex) else null
                    _uiState.update { it.copy(selectedFeatDetails = newFeatDetails) }
                }
            }
            val intermediateDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(raceSelections = currentSelections))
            val finalDraft = draftStatsUseCase(intermediateDraft)
            _uiState.update { it.copy(draft = finalDraft, globalExclusions = finalDraft.getProficiencyExclusions()) }
        }
    }

    fun selectClass(idx: String) {
        viewModelScope.launch {
            val classEntity = libraryRepository.getClassEntityByIndex(idx) ?: return@launch
            val currentBaseInfo = _uiState.value.draft.baseInfo
            val raceProfs = libraryRepository.getRaceByIndex(currentBaseInfo.raceIndex)?.baseProficiencies ?: emptyList()
            val classProfs = classEntity.proficienciesJson?.let { runCatching { json.decodeFromString<List<ReferenceJson>>(it).map { p -> p.index } }.getOrElse { emptyList() } } ?: emptyList()
            val savingThrows = classEntity.savingThrowsJson?.let { runCatching { json.decodeFromString<List<ReferenceJson>>(it).map { s -> "saving-throw-${s.index.lowercase()}" } }.getOrElse { emptyList() } } ?: emptyList()
            val allStaticProfs = (raceProfs + classProfs + savingThrows).distinct()

            val bgEquipment = _uiState.value.availableBackgrounds.find { it.name == currentBaseInfo.backgroundIndex }?.staticEquipment ?: emptyList()
            val staticEquipment = classEntity.startingEquipmentJson?.let { runCatching { json.decodeFromString<List<ReferenceJson>>(it).map { e -> e.index } }.getOrElse { emptyList() } } ?: emptyList()

            val newBaseInfo = currentBaseInfo.copy(
                staticProficiencies = allStaticProfs,
                staticEquipment = (bgEquipment + staticEquipment).distinct(),
                inventorySelections = emptyMap()
            )
            val intermediateDraft = _uiState.value.draft.copy(levelStack = listOf(LevelStep(classIndex = idx)), baseInfo = newBaseInfo)
            val finalDraft = draftStatsUseCase(pruneInvalidSelections(intermediateDraft))

            val featuresResult = libraryRepository.getClassFeaturesForLevel(idx, 1, null)
            val partitioned = partitionClassFeaturesUseCase(featuresResult)
            val aggregatedSpellFeature = spellChoiceAggregatorUseCase(partitioned.classSkillFeatures)
            val availableSubclasses = libraryRepository.getSubclassesForClass(idx)

            _uiState.update {
                it.copy(
                    draft = finalDraft,
                    classStepFeatures = partitioned.classSkillFeatures,
                    inventoryStepFeatures = partitioned.inventoryChoiceFeatures,
                    subclassChoiceFeature = partitioned.subclassChoiceFeature,
                    aggregatedSpellFeature = aggregatedSpellFeature,
                    availableSubclasses = availableSubclasses,
                    globalExclusions = finalDraft.getProficiencyExclusions(),
                    inventoryMode = InventoryMode.STANDARD_PACKS, shoppingCart = emptyList() // Сброс магазина
                )
            }
        }
    }

    fun selectSubclass(idx: String) {
        val curClass = _uiState.value.draft.levelStack.firstOrNull()?.classIndex ?: return
        viewModelScope.launch {
            val stack = _uiState.value.draft.levelStack.toMutableList()
            if(stack.isEmpty()) return@launch
            stack[0] = stack[0].copy(subclassIndex = idx)
            val intermediateDraft = _uiState.value.draft.copy(levelStack = stack)
            val finalDraft = draftStatsUseCase(pruneInvalidSelections(intermediateDraft))
            val featuresResult = libraryRepository.getClassFeaturesForLevel(curClass, 1, idx)
            val partitioned = partitionClassFeaturesUseCase(featuresResult)
            val aggregatedSpellFeature = spellChoiceAggregatorUseCase(partitioned.classSkillFeatures)
            _uiState.update { it.copy(
                draft = finalDraft, classStepFeatures = partitioned.classSkillFeatures,
                inventoryStepFeatures = partitioned.inventoryChoiceFeatures, subclassChoiceFeature = partitioned.subclassChoiceFeature,
                aggregatedSpellFeature = aggregatedSpellFeature, globalExclusions = finalDraft.getProficiencyExclusions()
            )}
        }
    }

    fun onClassSelectionChange(key: String, res: ChoiceResult) = onSelectionChange(SelectionSource.CLASS, key, res)
    fun onBgSelectionChange(key: String, res: ChoiceResult) = onSelectionChange(SelectionSource.BACKGROUND, key, res)
    fun onInventorySelectionChange(key: String, res: ChoiceResult) = onSelectionChange(SelectionSource.INVENTORY, key, res)

    fun selectBackground(bg: Background) {
        viewModelScope.launch {
            val classIdx = _uiState.value.draft.levelStack.firstOrNull()?.classIndex
            val classEquipment = if (classIdx != null) libraryRepository.getClassEntityByIndex(classIdx)?.startingEquipmentJson?.let {
                runCatching { json.decodeFromString<List<ReferenceJson>>(it).map { e -> e.index } }.getOrElse { emptyList() }
            } ?: emptyList() else emptyList()

            val newStaticEquipment = (classEquipment + bg.staticEquipment).distinct()
            val intermediateDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(
                backgroundIndex = bg.name, backgroundSelections = emptyMap(), staticEquipment = newStaticEquipment
            ))
            val finalDraft = draftStatsUseCase(pruneInvalidSelections(intermediateDraft))
            _uiState.update { it.copy(
                draft = finalDraft, backgroundFeatures = bg.features,
                globalExclusions = finalDraft.getProficiencyExclusions()
            )}
        }
    }

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
        val currentScores = _uiState.value.draft.baseInfo.baseAbilityScores.toMutableMap()
        val currentScore = currentScores[key] ?: 8
        val newScore = currentScore + diff
        if (newScore < DndRules.MIN_SCORE || newScore > DndRules.MAX_SCORE) return

        val totalSpent = _uiState.value.draft.baseInfo.baseAbilityScores.values.sumOf { DndRules.getPointCost(it) }
        val costChange = DndRules.getPointCost(newScore) - DndRules.getPointCost(currentScore)

        if (totalSpent + costChange <= DndRules.MAX_POINTS) {
            currentScores[key] = newScore
            viewModelScope.launch {
                val finalDraft = draftStatsUseCase(_uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(baseAbilityScores = currentScores)))
                _uiState.update { it.copy(draft = finalDraft) }
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

    // --- Магазин ---
    fun setInventoryMode(mode: InventoryMode) {
        viewModelScope.launch {
            val classIdx = _uiState.value.draft.levelStack.firstOrNull()?.classIndex
            if (classIdx.isNullOrBlank()) { /* TODO: show error message */ return@launch }

            if (mode == InventoryMode.BUY_WITH_GOLD) {
                val startingGold = getStartingGoldForClass(classIdx)
                val categories = libraryRepository.getAllEquipmentCategories().map { ShopCategory(it.indexName, it.name ?: "Без названия") }
                val bgEquipment = _uiState.value.availableBackgrounds.find { it.name == _uiState.value.draft.baseInfo.backgroundIndex }?.staticEquipment ?: emptyList()
                val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(
                    inventorySelections = emptyMap(), staticEquipment = bgEquipment
                ))
                _uiState.update { it.copy(
                    inventoryMode = mode, shopCategories = categories,
                    initialGold = startingGold, remainingGold = startingGold,
                    shoppingCart = emptyList(), shopView = ShopView.CATEGORIES,
                    selectedShopCategory = null, shopItems = emptyList(), draft = newDraft
                )}
            } else { // STANDARD_PACKS
                val classEquipment = libraryRepository.getClassEntityByIndex(classIdx)?.startingEquipmentJson?.let {
                    runCatching { json.decodeFromString<List<ReferenceJson>>(it).map { e -> e.index } }.getOrElse { emptyList() }
                } ?: emptyList()
                val bgEquipment = _uiState.value.availableBackgrounds.find { it.name == _uiState.value.draft.baseInfo.backgroundIndex }?.staticEquipment ?: emptyList()
                val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(
                    inventorySelections = emptyMap(), staticEquipment = (bgEquipment + classEquipment).distinct()
                ))
                _uiState.update { it.copy(inventoryMode = mode, draft = newDraft) }
            }
        }
    }

    fun selectShopCategory(category: ShopCategory) {
        viewModelScope.launch {
            val items = libraryRepository.getEquipmentByCategory(category.index).map { entity -> mapToShopItem(entity) }
            _uiState.update { it.copy(shopItems = items, selectedShopCategory = category, shopView = ShopView.ITEMS) }
        }
    }

    fun goBackToCategories() { _uiState.update { it.copy(shopView = ShopView.CATEGORIES, selectedShopCategory = null, shopItems = emptyList()) } }

    fun searchShop(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            if (query.isBlank()) {
                if (_uiState.value.shopView == ShopView.ITEMS && _uiState.value.selectedShopCategory != null) {
                    selectShopCategory(_uiState.value.selectedShopCategory!!)
                } else {
                    _uiState.update { it.copy(shopItems = emptyList()) }
                }
                return@launch
            }
            val results = libraryRepository.searchEquipment(query).map { mapToShopItem(it) }
            _uiState.update { it.copy(shopItems = results, shopView = ShopView.ITEMS) }
        }
    }

    fun addItemToCart(item: ShopItem) {
        if (_uiState.value.remainingGold >= item.cost) {
            val newCart = _uiState.value.shoppingCart + item
            val newGold = _uiState.value.remainingGold - item.cost
            val newSelections = mapOf("shop_cart" to ChoiceResult.SelectedOptions(newCart.map { it.index }))
            val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(inventorySelections = newSelections))
            _uiState.update { it.copy(shoppingCart = newCart, remainingGold = newGold, draft = newDraft) }
        }
    }

    fun removeItemFromCart(item: ShopItem) {
        val newCart = _uiState.value.shoppingCart.toMutableList()
        if (newCart.remove(item)) {
            val newGold = _uiState.value.remainingGold + item.cost
            val newSelections = mapOf("shop_cart" to ChoiceResult.SelectedOptions(newCart.map { it.index }))
            val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(inventorySelections = newSelections))
            _uiState.update { it.copy(shoppingCart = newCart, remainingGold = newGold, draft = newDraft) }
        }
    }

    private fun getStartingGoldForClass(classIndex: String): Money {
        return when (classIndex) {
            "barbarian", "fighter", "paladin", "ranger" -> Money(gp = Random.nextInt(5, 21) * 10)
            "bard", "cleric", "rogue" -> Money(gp = Random.nextInt(5, 21) * 10)
            "druid" -> Money(gp = Random.nextInt(2, 9) * 10)
            "sorcerer", "warlock", "wizard" -> Money(gp = Random.nextInt(4, 17) * 10)
            "monk" -> Money(gp = Random.nextInt(5, 21))
            else -> Money(gp = 100)
        }
    }

    private fun mapToShopItem(entity: com.dnd.app.data.local.entity.EquipmentEntity): ShopItem {
        return ShopItem(
            index = entity.indexName, name = entity.name,
            cost = parseCostJsonToMoney(entity.costJson),
            weight = entity.weight, description = entity.description
        )
    }

    private fun parseCostJsonToMoney(costJson: String?): Money {
        if (costJson.isNullOrBlank()) return Money()
        return try {
            val obj = json.parseToJsonElement(costJson).jsonObject
            // [ИСПРАВЛЕНО] Заменено на .content.toIntOrNull()
            val quantity = obj["quantity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            when (obj["unit"]?.jsonPrimitive?.content) {
                "gp" -> Money(gp = quantity)
                "sp" -> Money(sp = quantity)
                "cp" -> Money(cp = quantity)
                else -> Money()
            }
        } catch (e: Exception) { Money() }
    }

    private enum class SelectionSource { CLASS, BACKGROUND, INVENTORY }
    private fun onSelectionChange(source: SelectionSource, key: String, res: ChoiceResult) {
        viewModelScope.launch {
            val currentDraft = _uiState.value.draft
            val newDraft = when (source) {
                SelectionSource.CLASS -> {
                    val stack = currentDraft.levelStack.toMutableList()
                    if (stack.isNotEmpty()) {
                        val sel = stack[0].selections.toMutableMap().apply { put(key, res) }
                        stack[0] = stack[0].copy(selections = sel)
                    }
                    currentDraft.copy(levelStack = stack)
                }
                SelectionSource.BACKGROUND -> {
                    val sel = currentDraft.baseInfo.backgroundSelections.toMutableMap().apply { put(key, res) }
                    currentDraft.copy(baseInfo = currentDraft.baseInfo.copy(backgroundSelections = sel))
                }
                SelectionSource.INVENTORY -> {
                    val sel = currentDraft.baseInfo.inventorySelections.toMutableMap().apply { put(key, res) }
                    currentDraft.copy(baseInfo = currentDraft.baseInfo.copy(inventorySelections = sel))
                }
            }
            val finalDraft = draftStatsUseCase(newDraft)
            _uiState.update { it.copy(draft = finalDraft, globalExclusions = finalDraft.getProficiencyExclusions()) }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt