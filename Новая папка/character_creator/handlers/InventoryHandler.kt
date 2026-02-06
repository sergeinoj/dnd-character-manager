// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/handlers/InventoryHandler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.handlers

import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.Money
import com.dnd.app.domain.model.ShopCategory
import com.dnd.app.domain.model.ShopItem
import com.dnd.app.domain.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import kotlin.random.Random

/**
 * [НОВЫЙ HANDLER]
 * Изолированная, stateful-логика для управления состоянием магазина.
 * ViewModel создает экземпляр этого класса и просто вызывает его методы.
 */
class InventoryHandler(
    private val scope: CoroutineScope,
    private val repository: LibraryRepository,
    private val onCartUpdate: (List<ShopItem>) -> Unit
) {
    data class ShopState(
        val view: com.dnd.app.domain.model.ShopView = com.dnd.app.domain.model.ShopView.CATEGORIES,
        val categories: List<ShopCategory> = emptyList(),
        val items: List<ShopItem> = emptyList(),
        val cart: List<ShopItem> = emptyList(),
        val remainingGold: Money = Money(),
        val initialGold: Money = Money(),
        val currentTitle: String = "Магазин"
    )

    private val _state = MutableStateFlow(ShopState())
    val state = _state.asStateFlow()

    private val categoryStack = ArrayDeque<ShopCategory>()
    private var searchJob: Job? = null

    fun initializeForGoldBuy(classIndex: String) {
        scope.launch {
            val startingGold = getStartingGoldForClass(classIndex)
            categoryStack.clear()
            val rootCategories = repository.getRootShopCategories()
            _state.update {
                ShopState(
                    initialGold = startingGold,
                    remainingGold = startingGold,
                    categories = rootCategories
                )
            }
        }
    }

    fun selectCategory(category: ShopCategory) {
        scope.launch {
            val children = repository.getChildShopCategories(category.index)
            categoryStack.push(category)
            if (children.isNotEmpty()) {
                _state.update { it.copy(
                    categories = children,
                    view = com.dnd.app.domain.model.ShopView.CATEGORIES,
                    currentTitle = category.name
                )}
            } else {
                val items = repository.getItemsForCategory(category.index)
                _state.update { it.copy(
                    items = items,
                    view = com.dnd.app.domain.model.ShopView.ITEMS,
                    currentTitle = category.name
                )}
            }
        }
    }

    fun goBack() {
        scope.launch {
            if (categoryStack.isNotEmpty()) categoryStack.pop()
            val parent = categoryStack.peek()
            if (parent == null) {
                _state.update { it.copy(
                    categories = repository.getRootShopCategories(),
                    view = com.dnd.app.domain.model.ShopView.CATEGORIES,
                    currentTitle = "Магазин"
                )}
            } else {
                _state.update { it.copy(
                    categories = repository.getChildShopCategories(parent.index),
                    view = com.dnd.app.domain.model.ShopView.CATEGORIES,
                    currentTitle = parent.name
                )}
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            if (query.isBlank()) {
                goBack()
                return@launch
            }
            _state.update { it.copy(
                items = repository.searchAllItems(query),
                view = com.dnd.app.domain.model.ShopView.ITEMS,
                currentTitle = "Результаты поиска"
            )}
        }
    }

    fun addToCart(item: ShopItem) {
        if (_state.value.remainingGold >= item.cost) {
            val newCart = _state.value.cart + item
            _state.update { it.copy(
                cart = newCart,
                remainingGold = it.remainingGold - item.cost
            )}
            onCartUpdate(newCart)
        }
    }

    fun removeFromCart(item: ShopItem) {
        val newCart = _state.value.cart.toMutableList()
        if (newCart.remove(item)) {
            _state.update { it.copy(
                cart = newCart,
                remainingGold = it.remainingGold + item.cost
            )}
            onCartUpdate(newCart)
        }
    }

    private fun getStartingGoldForClass(classIndex: String): Money {
        return when (classIndex) {
            "barbarian" -> Money(gp = Random.nextInt(2, 9) * 10); "bard" -> Money(gp = Random.nextInt(5, 21) * 10); "cleric" -> Money(gp = Random.nextInt(5, 21) * 10); "druid" -> Money(gp = Random.nextInt(2, 9) * 10); "fighter" -> Money(gp = Random.nextInt(5, 21) * 10); "monk" -> Money(gp = Random.nextInt(5, 21)); "paladin" -> Money(gp = Random.nextInt(5, 21) * 10); "ranger" -> Money(gp = Random.nextInt(5, 21) * 10); "rogue" -> Money(gp = Random.nextInt(4, 17) * 10); "sorcerer" -> Money(gp = Random.nextInt(3, 13) * 10); "warlock" -> Money(gp = Random.nextInt(4, 17) * 10); "wizard" -> Money(gp = Random.nextInt(4, 17) * 10); else -> Money(gp = 100)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/handlers/InventoryHandler.kt