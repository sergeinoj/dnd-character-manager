// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.data.model.ReferenceJson
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.rules.DndRules
import com.dnd.app.domain.usecase.*
import com.dnd.app.util.capitalizeFirst
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

data class EquipmentOptionDetails(val name: String, val contents: List<String>)

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

    // --- Новые поля для магазина (v1.25) ---
    val inventoryMode: InventoryMode = InventoryMode.STANDARD_PACKS,
    val shopView: ShopView = ShopView.CATEGORIES,
    val shopCategories: List<ShopCategory> = emptyList(),
    val shopItems: List<ShopItem> = emptyList(),
    val shoppingCart: List<ShopItem> = emptyList(),
    val remainingGold: Money = Money(),
    val initialGold: Money = Money(),
    val currentShopTitle: String = "Магазин",

    // --- Новое поле для распаковки снаряжения (v1.26) ---
    val unpackedEquipmentOptions: Map<String, EquipmentOptionDetails> = emptyMap()
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
    private val categoryStack = ArrayDeque<ShopCategory>()
    private val calculator = DndCalculator()

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

            val bg = _uiState.value.availableBackgrounds.find { it.name == currentBaseInfo.backgroundIndex }
            val bgEquipment = bg?.staticEquipment ?: emptyList()
            val staticEquipment = classEntity.startingEquipmentJson?.let { runCatching { json.decodeFromString<List<ReferenceJson>>(it).map { e -> e.index } }.getOrElse { emptyList() } } ?: emptyList()

            val newBaseInfo = currentBaseInfo.copy(
                staticProficiencies = allStaticProfs,
                staticEquipment = (bgEquipment + staticEquipment).distinct(),
                inventorySelections = emptyMap()
            )
            val intermediateDraft = _uiState.value.draft.copy(levelStack = listOf(LevelStep(classIndex = idx)), baseInfo = newBaseInfo)
            val finalDraft = draftStatsUseCase(pruneInvalidSelections(intermediateDraft))

            val abilityModifier = getRelevantAbilityModifier(finalDraft)
            val featuresResult = libraryRepository.getClassFeaturesForLevel(idx, 1, null, abilityModifier)
            val partitioned = partitionClassFeaturesUseCase(featuresResult)
            val aggregatedSpellFeature = spellChoiceAggregatorUseCase(partitioned.classSkillFeatures)
            val availableSubclasses = libraryRepository.getSubclassesForClass(idx)
            val unpackedEquipment = unpackAllEquipmentOptions(partitioned.inventoryChoiceFeatures)

            _uiState.update {
                it.copy(
                    draft = finalDraft,
                    classStepFeatures = partitioned.classSkillFeatures,
                    inventoryStepFeatures = partitioned.inventoryChoiceFeatures,
                    subclassChoiceFeature = partitioned.subclassChoiceFeature,
                    aggregatedSpellFeature = aggregatedSpellFeature,
                    availableSubclasses = availableSubclasses,
                    globalExclusions = finalDraft.getProficiencyExclusions(),
                    inventoryMode = InventoryMode.STANDARD_PACKS, shoppingCart = emptyList(),
                    unpackedEquipmentOptions = unpackedEquipment
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
            val abilityModifier = getRelevantAbilityModifier(finalDraft)
            val featuresResult = libraryRepository.getClassFeaturesForLevel(curClass, 1, idx, abilityModifier)
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
                backgroundIndex = bg.name,
                backgroundSelections = emptyMap(),
                staticEquipment = newStaticEquipment
            ))
            val finalDraft = draftStatsUseCase(pruneInvalidSelections(intermediateDraft))

            _uiState.update { it.copy(
                draft = finalDraft,
                backgroundFeatures = bg.features,
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

                val currentClassIndex = _uiState.value.draft.levelStack.firstOrNull()?.classIndex
                if (currentClassIndex != null) {
                    val primaryStat = getPrimaryCastingStat(currentClassIndex)
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

    // --- Магазин v1.25 ---
    fun setInventoryMode(mode: InventoryMode) {
        viewModelScope.launch {
            val classIdx = _uiState.value.draft.levelStack.firstOrNull()?.classIndex
            if (classIdx.isNullOrBlank()) { return@launch }

            val bgEquipment = _uiState.value.availableBackgrounds.find { it.name == _uiState.value.draft.baseInfo.backgroundIndex }?.staticEquipment ?: emptyList()
            val baseDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(inventorySelections = emptyMap()))

            if (mode == InventoryMode.BUY_WITH_GOLD) {
                val startingGold = getStartingGoldForClass(classIdx)
                val newDraft = baseDraft.copy(baseInfo = baseDraft.baseInfo.copy(staticEquipment = bgEquipment))
                _uiState.update { it.copy(
                    inventoryMode = mode, initialGold = startingGold, remainingGold = startingGold,
                    shoppingCart = emptyList(), shopView = ShopView.CATEGORIES, shopItems = emptyList(),
                    currentShopTitle = "Магазин", draft = newDraft
                )}
                loadRootShopCategories()
            } else {
                val classEquipment = libraryRepository.getClassEntityByIndex(classIdx)?.startingEquipmentJson?.let {
                    runCatching { json.decodeFromString<List<ReferenceJson>>(it).map { e -> e.index } }.getOrElse { emptyList() }
                } ?: emptyList()
                val newDraft = baseDraft.copy(baseInfo = baseDraft.baseInfo.copy(staticEquipment = (bgEquipment + classEquipment).distinct()))
                _uiState.update { it.copy(inventoryMode = mode, draft = newDraft) }
            }
        }
    }

    private suspend fun unpackAllEquipmentOptions(features: List<Feature>): Map<String, EquipmentOptionDetails> {
        val allOptionIndexes = features
            .flatMap { it.choices }
            .filterIsInstance<FeatureChoiceDomain.SelectOption>()
            .flatMap { it.options }
            .map { it.id }
            .distinct()

        if (allOptionIndexes.isEmpty()) return emptyMap()

        val equipmentEntities = libraryRepository.getEquipmentByIndexes(allOptionIndexes).associateBy { it.indexName }
        val unpackedDetails = mutableMapOf<String, EquipmentOptionDetails>()

        val allContentIndexes = mutableSetOf<String>()
        equipmentEntities.values.forEach { entity ->
            entity.contentsJson?.let { rawJson ->
                try {
                    val contents = json.decodeFromString<List<JsonObject>>(rawJson)
                    contents.forEach { item ->
                        item["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content?.let {
                            allContentIndexes.add(it)
                        }
                    }
                } catch (e: Exception) { Log.w(TAG, "Malformed contents_json for ${entity.indexName}") }
            }
        }

        val contentEntities = if (allContentIndexes.isNotEmpty()) {
            libraryRepository.getEquipmentByIndexes(allContentIndexes.toList()).associateBy { it.indexName }
        } else {
            emptyMap()
        }

        for (index in allOptionIndexes) {
            val entity = equipmentEntities[index] ?: continue
            val contentNames = mutableListOf<String>()
            entity.contentsJson?.let { rawJson ->
                try {
                    val contents = json.decodeFromString<List<JsonObject>>(rawJson)
                    for (item in contents) {
                        val itemIndex = item["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                        val itemName = contentEntities[itemIndex]?.name ?: itemIndex?.capitalizeFirst() ?: "Неизвестный предмет"
                        val quantity = item["quantity"]?.jsonPrimitive?.int ?: 1

                        val formattedName = if(quantity > 1) "${itemName} x${quantity}" else itemName
                        contentNames.add(formattedName)
                    }
                } catch (e: Exception) { Log.w(TAG, "Second pass malformed contents_json for ${entity.indexName}") }
            }
            unpackedDetails[index] = EquipmentOptionDetails(entity.name, contentNames)
        }
        return unpackedDetails
    }

    private fun loadRootShopCategories() {
        viewModelScope.launch {
            categoryStack.clear()
            val categories = libraryRepository.getRootShopCategories()
            _uiState.update { it.copy(
                shopCategories = categories, shopItems = emptyList(),
                shopView = ShopView.CATEGORIES, currentShopTitle = "Магазин"
            )}
        }
    }

    fun selectShopCategory(category: ShopCategory) {
        viewModelScope.launch {
            val children = libraryRepository.getChildShopCategories(category.index)
            if (children.isNotEmpty()) {
                categoryStack.push(category)
                _uiState.update { it.copy(
                    shopCategories = children, shopView = ShopView.CATEGORIES,
                    currentShopTitle = category.name
                )}
            } else {
                val items = libraryRepository.getItemsForCategory(category.index)
                categoryStack.push(category)
                _uiState.update { it.copy(
                    shopItems = items, shopView = ShopView.ITEMS,
                    currentShopTitle = category.name
                )}
            }
        }
    }

    fun goBackInShop() {
        viewModelScope.launch {
            if (categoryStack.isNotEmpty()) {
                categoryStack.pop()
            }

            if (categoryStack.isEmpty()) {
                loadRootShopCategories()
            } else {
                val parent = categoryStack.peek()
                _uiState.update { state -> state.copy(
                    shopCategories = if (parent != null) libraryRepository.getChildShopCategories(parent.index) else emptyList(),
                    shopView = ShopView.CATEGORIES,
                    currentShopTitle = parent?.name ?: "Магазин"
                )}
            }
        }
    }

    fun searchShop(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (query.isBlank()) {
                if (_uiState.value.shopView == ShopView.ITEMS && categoryStack.isNotEmpty()) {
                    val currentCategory = categoryStack.peek()
                    if (currentCategory != null) {
                        val items = libraryRepository.getItemsForCategory(currentCategory.index)
                        _uiState.update { it.copy(shopItems = items, shopView = ShopView.ITEMS, currentShopTitle = currentCategory.name)}
                    } else {
                        goBackInShop()
                    }
                } else {
                    goBackInShop()
                }
                return@launch
            }
            val results = libraryRepository.searchAllItems(query)
            _uiState.update { it.copy(shopItems = results, shopView = ShopView.ITEMS, currentShopTitle = "Результаты поиска") }
        }
    }

    fun addItemToCart(item: ShopItem) {
        if (_uiState.value.remainingGold >= item.cost) {
            val newCart = _uiState.value.shoppingCart + item
            val newGold = _uiState.value.remainingGold - item.cost
            updateCartInDraft(newCart)
            _uiState.update { it.copy(shoppingCart = newCart, remainingGold = newGold) }
        }
    }

    fun removeItemFromCart(item: ShopItem) {
        val newCart = _uiState.value.shoppingCart.toMutableList()
        if (newCart.remove(item)) {
            val newGold = _uiState.value.remainingGold + item.cost
            updateCartInDraft(newCart)
            _uiState.update { it.copy(shoppingCart = newCart, remainingGold = newGold) }
        }
    }

    private fun updateCartInDraft(cart: List<ShopItem>) {
        val newSelections = mapOf("shop_cart" to ChoiceResult.SelectedOptions(cart.map { it.index }))
        val newDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(inventorySelections = newSelections))
        _uiState.update { it.copy(draft = newDraft) }
    }

    private fun getStartingGoldForClass(classIndex: String): Money {
        return when (classIndex) {
            "barbarian" -> Money(gp = Random.nextInt(2, 9) * 10)
            "bard" -> Money(gp = Random.nextInt(5, 21) * 10)
            "cleric" -> Money(gp = Random.nextInt(5, 21) * 10)
            "druid" -> Money(gp = Random.nextInt(2, 9) * 10)
            "fighter" -> Money(gp = Random.nextInt(5, 21) * 10)
            "monk" -> Money(gp = Random.nextInt(5, 21))
            "paladin" -> Money(gp = Random.nextInt(5, 21) * 10)
            "ranger" -> Money(gp = Random.nextInt(5, 21) * 10)
            "rogue" -> Money(gp = Random.nextInt(4, 17) * 10)
            "sorcerer" -> Money(gp = Random.nextInt(3, 13) * 10)
            "warlock" -> Money(gp = Random.nextInt(4, 17) * 10)
            "wizard" -> Money(gp = Random.nextInt(4, 17) * 10)
            else -> Money(gp = 100)
        }
    }

    private fun getPrimaryCastingStat(classIndex: String): String? {
        return when (classIndex) {
            "cleric", "druid", "ranger" -> "WIS"
            "wizard" -> "INT"
            "paladin", "bard", "sorcerer", "warlock" -> "CHA"
            else -> null
        }
    }

    private fun getRelevantAbilityModifier(draft: DraftCharacter = _uiState.value.draft): Int {
        val classIndex = draft.levelStack.firstOrNull()?.classIndex ?: return 0
        val primaryStat = getPrimaryCastingStat(classIndex) ?: return 0
        val baseScore = draft.baseInfo.baseAbilityScores[primaryStat] ?: 10
        val bonus = draft.baseInfo.aggregateStatBonuses[primaryStat] ?: 0
        val totalScore = baseScore + bonus
        return calculator.calculateModifier(totalScore)
    }

    private fun refreshClassFeatures() {
        viewModelScope.launch {
            val draft = _uiState.value.draft
            val classIndex = draft.levelStack.firstOrNull()?.classIndex ?: return@launch
            val subclassIndex = draft.levelStack.firstOrNull()?.subclassIndex
            val abilityModifier = getRelevantAbilityModifier()

            val featuresResult = libraryRepository.getClassFeaturesForLevel(classIndex, 1, subclassIndex, abilityModifier)
            val partitioned = partitionClassFeaturesUseCase(featuresResult)
            val aggregatedSpellFeature = spellChoiceAggregatorUseCase(partitioned.classSkillFeatures)

            val preparedSpellsFeature = partitioned.classSkillFeatures.find { it.index == "virtual-prepared-spells" }
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
                    classStepFeatures = partitioned.classSkillFeatures,
                    subclassChoiceFeature = partitioned.subclassChoiceFeature,
                    aggregatedSpellFeature = aggregatedSpellFeature
                )
            }
        }
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