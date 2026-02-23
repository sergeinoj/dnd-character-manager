package com.dnd.app.ui.screens.shape

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.model.MonsterFilter
import com.dnd.app.domain.model.MonsterRecord
import com.dnd.app.domain.model.snapshot.ResourcePoolSnapshot
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.usecase.GetAvailableShapesUseCase
import com.dnd.app.domain.usecase.WildShapeContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dnd.app.util.DndLocalization
import javax.inject.Inject

@HiltViewModel
class ShapeSelectorViewModel @Inject constructor(
    private val getAvailableShapes: GetAvailableShapesUseCase,
    private val repository: CharacterRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShapeSelectorUiState())
    val uiState: StateFlow<ShapeSelectorUiState> = _uiState.asStateFlow()
    private val characterId: Long = savedStateHandle.get<Long>("characterId") ?: 0L

    init {
        loadShapes(CrRange.ANY)
    }

    fun onFilterSelected(range: CrRange) {
        loadShapes(range)
    }

    private fun loadShapes(range: CrRange) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedRange = range, error = null) }
            val result = runCatching {
                val context = resolveWildShapeContext()
                    ?: throw IllegalStateException("Не удалось определить контекст Дикого облика для текущего персонажа.")
                getAvailableShapes(range.toFilter(), context)
            }
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(isLoading = false, monsters = list, error = null)
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.message ?: "Не удалось загрузить зверей")
                    }
                }
            )
        }
    }

    private suspend fun resolveWildShapeContext(): WildShapeContext? {
        val draft = repository.getDraftById(characterId) ?: return null
        val druidSteps = draft.levelStack.filter { it.classIndex.equals("druid", ignoreCase = true) }
        if (druidSteps.isEmpty()) return null
        val druidLevel = druidSteps.size
        val subclass = druidSteps.lastOrNull { !it.subclassIndex.isNullOrBlank() }?.subclassIndex
        return WildShapeContext(
            classIndex = "druid",
            subclassIndex = subclass,
            level = druidLevel
        )
    }

    fun onShapeSelected(monsterId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val current = _uiState.value
            val monster = current.monsters.firstOrNull { it.index == monsterId }
            val wildShapeLabel = DndLocalization.translateProficiency("Wild Shape Uses").lowercase()
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.performAtomicMutation(characterId) { snap, live, _ ->
                val pool = findWildShapePool(snap.resourcePools, wildShapeLabel)
                    ?: return@performAtomicMutation Result.failure(Exception("Ресурс Дикий облик не найден."))
                val currentSpent = live.featureCharges[pool.id] ?: 0
                if (currentSpent >= pool.max) {
                    return@performAtomicMutation Result.failure(Exception("Заряды Дикого облика исчерпаны."))
                }
                val nextCharges = live.featureCharges.toMutableMap().apply { put(pool.id, currentSpent + 1) }
                val hp = monster?.hitPoints ?: 0
                val name = monster?.name ?: monsterId
                val entry = "Персонаж принял облик $name, получено $hp временных хитов"
                val nextLogs = (live.systemLogs + entry).takeLast(10)
                val nextLive = live.copy(
                    featureCharges = nextCharges,
                    transformationId = monsterId,
                    transformationHp = hp,
                    systemLogs = nextLogs
                )
                Result.success(nextLive to Unit)
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.message ?: "Не удалось выбрать форму")
                    }
                }
            )
        }
    }

    private fun findWildShapePool(
        pools: List<ResourcePoolSnapshot>,
        wildShapeLabel: String
    ): ResourcePoolSnapshot? {
        return pools.firstOrNull { pool ->
            val lowerName = pool.name.lowercase()
            val lowerId = pool.id.lowercase()
            val matchesLabel = wildShapeLabel.isNotBlank() && lowerName.contains(wildShapeLabel)
            matchesLabel ||
                (lowerName.contains("wild") && lowerName.contains("shape")) ||
                (lowerName.contains("дик") && lowerName.contains("облик")) ||
                (lowerId.contains("wild") && lowerId.contains("shape"))
        }
    }


}

data class ShapeSelectorUiState(
    val isLoading: Boolean = true,
    val monsters: List<MonsterRecord> = emptyList(),
    val selectedRange: CrRange = CrRange.ANY,
    val error: String? = null
)

enum class CrRange(val label: String, val min: Double?, val max: Double?) {
    ANY("Все CR", null, null),
    UNDER_ONE("CR < 1", null, 0.99),
    ONE_TO_FOUR("CR 1-4", 1.0, 4.0),
    FIVE_TO_NINE("CR 5-9", 5.0, 9.0),
    TEN_PLUS("CR ≥ 10", 10.0, null);

    fun toFilter(): MonsterFilter? {
        return if (min == null && max == null) null else MonsterFilter(min, max)
    }
}
