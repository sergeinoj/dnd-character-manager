// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/InventoryHandler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.LibraryRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import javax.inject.Inject
import kotlin.random.Random

/**
 * [НОВЫЙ КЛАСС - ЭТАП 5]
 * Инкапсулирует всю бизнес-логику, связанную с инвентарем и магазином,
 * убирая ее из CharacterCreatorViewModel.
 * Управляет состоянием через переданный MutableStateFlow.
 */
@ViewModelScoped
class InventoryHandler @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {
    private lateinit var scope: CoroutineScope
    private lateinit var _uiState: MutableStateFlow<CreatorUiState>

    private var searchJob: Job? = null
    private val categoryStack = ArrayDeque<ShopCategory>()

    fun initialize(scope: CoroutineScope, uiState: MutableStateFlow<CreatorUiState>) {
        this.scope = scope
        this._uiState = uiState
    }

    fun setInventoryMode(mode: InventoryMode) {
        scope.launch {
            val baseDraft = _uiState.value.draft.copy(baseInfo = _uiState.value.draft.baseInfo.copy(inventorySelections = emptyMap()))
            // Для переключения режима не нужен полный bake, достаточно сбросить инвентарь
            // val finalDraft = bakeCharacterUseCase(baseDraft)

            if (mode == InventoryMode.BUY_WITH_GOLD) {
                val classIdx = baseDraft.levelStack.firstOrNull()?.classIndex ?: return@launch
                val startingGold = getStartingGoldForClass(classIdx)
                _uiState.update { it.copy(
                    draft = baseDraft,
                    inventoryMode = mode, initialGold = startingGold, remainingGold = startingGold,
                    shoppingCart = emptyList(), shopView = ShopView.CATEGORIES, shopItems = emptyList(),
                    currentShopTitle = "Магазин"
                )}
                loadRootShopCategories()
            } else {
                _uiState.update { it.copy(
                    draft = baseDraft,
                    inventoryMode = mode
                )}
            }
        }
    }

    fun selectShopCategory(category: ShopCategory) {
        scope.launch {
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
        scope.launch {
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
        searchJob = scope.launch {
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

    private fun loadRootShopCategories() {
        scope.launch {
            categoryStack.clear()
            val categories = libraryRepository.getRootShopCategories()
            _uiState.update { it.copy(
                shopCategories = categories, shopItems = emptyList(),
                shopView = ShopView.CATEGORIES, currentShopTitle = "Магазин"
            )}
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
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/InventoryHandler.kt