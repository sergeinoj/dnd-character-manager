// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\MerchantManager.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque
import javax.inject.Inject

data class MerchantUiState(
    val categories: List<ShopCategory> = emptyList(),
    val items: List<ShopItem> = emptyList(),
    val breadcrumbs: List<ShopCategory> = emptyList(),
    val isSearching: Boolean = false,
    val currentTitle: String = "Магазин"
)

class MerchantManager @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    private val _state = MutableStateFlow(MerchantUiState())
    val state = _state.asStateFlow()

    private val categoryStack = ArrayDeque<ShopCategory>()
    private var searchJob: Job? = null
    private lateinit var scope: CoroutineScope
    private val navigationMutex = Mutex()

    fun init(scope: CoroutineScope) {
        if (this::scope.isInitialized) return
        this.scope = scope
        loadRoot()
    }

    fun loadRoot() {
        scope.launch {
            navigationMutex.withLock {
                val roots = libraryRepository.getRootShopCategories()
                categoryStack.clear()
                _state.update {
                    it.copy(
                        categories = roots,
                        items = emptyList(),
                        breadcrumbs = emptyList(),
                        currentTitle = "Магазин",
                        isSearching = false
                    )
                }
            }
        }
    }

    fun selectCategory(category: ShopCategory) {
        scope.launch {
            navigationMutex.withLock {
                val children = libraryRepository.getChildShopCategories(category.index)
                val items = if (children.isEmpty()) libraryRepository.getItemsForCategory(category.index) else emptyList()
                categoryStack.push(category)


                val currentBreadcrumbs = categoryStack.toList().reversed()

                _state.update {
                    it.copy(
                        categories = children,
                        items = items,
                        breadcrumbs = currentBreadcrumbs,
                        currentTitle = category.name,
                        isSearching = false
                    )
                }
            }
        }
    }

    fun goBack() {
        scope.launch {
            navigationMutex.withLock {
                if (state.value.isSearching) {
                    val roots = libraryRepository.getRootShopCategories()
                    categoryStack.clear()
                    _state.update {
                        it.copy(categories = roots, items = emptyList(), breadcrumbs = emptyList(), currentTitle = "Магазин", isSearching = false)
                    }
                    return@withLock
                }

                if (categoryStack.isNotEmpty()) {
                    categoryStack.pop()
                }

                val currentBreadcrumbs = categoryStack.toList().reversed()

                if (categoryStack.isEmpty()) {
                    val roots = libraryRepository.getRootShopCategories()
                    _state.update {
                        it.copy(categories = roots, items = emptyList(), breadcrumbs = currentBreadcrumbs, currentTitle = "Магазин")
                    }
                } else {
                    val parent = categoryStack.peek()!!
                    val children = libraryRepository.getChildShopCategories(parent.index)
                    _state.update {
                        it.copy(
                            categories = children,
                            items = emptyList(),
                            breadcrumbs = currentBreadcrumbs,
                            currentTitle = parent.name
                        )
                    }
                }
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            goBack()
            return
        }

        searchJob = scope.launch {
            delay(300)
            navigationMutex.withLock {
                val results = libraryRepository.searchAllItems(query)
                _state.update {
                    it.copy(
                        categories = emptyList(),
                        items = results,
                        breadcrumbs = emptyList(),
                        currentTitle = "Поиск: '$query'",
                        isSearching = true
                    )
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\MerchantManager.kt