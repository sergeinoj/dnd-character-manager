// Имя файла: ui/screens/character_sheet/CharacterSheetViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.model.Spell
import com.dnd.app.domain.model.Stats
import com.dnd.app.domain.model.Weapon
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.CharacterUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CharacterSheetState(
    val character: CharacterDomain? = null,
    val weapons: List<Weapon> = emptyList(),
    val spells: List<Spell> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CharacterSheetViewModel @Inject constructor(
    private val useCases: CharacterUseCases,
    private val libraryRepository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
    val calculator: DndCalculator
) : ViewModel() {

    private val characterId: Long = checkNotNull(savedStateHandle["characterId"])

    private val _state = MutableStateFlow(CharacterSheetState())
    val state = _state.asStateFlow()

    init {
        loadCharacter()
    }

    private fun loadCharacter() {
        viewModelScope.launch {
            val char = useCases.getCharacter(characterId)
            if (char != null) {
                val loadedWeapons = libraryRepository.getWeaponsByIds(char.inventoryIds)
                val loadedSpells = libraryRepository.getSpellsByIds(char.spellsKnownIds)

                _state.value = _state.value.copy(
                    character = char,
                    weapons = loadedWeapons,
                    spells = loadedSpells,
                    isLoading = false
                )
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun updateHp(change: Int) {
        val currentChar = _state.value.character ?: return
        val newHp = (currentChar.hpCurrent + change).coerceIn(0, currentChar.hpMax)

        saveCharacter(currentChar.copy(hpCurrent = newHp))
    }

    fun updateMoney(type: String, delta: Int) {
        val char = _state.value.character ?: return
        var s = char.stats

        // Логика математики монет
        if (delta > 0) {
            // Простое добавление
            s = when(type) {
                "CP" -> s.copy(copper = s.copper + delta)
                "SP" -> s.copy(silver = s.silver + delta)
                "GP" -> s.copy(gold = s.gold + delta)
                else -> s
            }
        } else {
            // Вычитание с разменом
            s = subtractMoneyRecursive(s, type, -delta)
        }

        saveCharacter(char.copy(stats = s))
    }

    private fun subtractMoneyRecursive(stats: Stats, type: String, amount: Int): Stats {
        var currentStats = stats

        when (type) {
            "CP" -> {
                if (currentStats.copper >= amount) {
                    currentStats = currentStats.copy(copper = currentStats.copper - amount)
                } else {
                    // Не хватает меди, пробуем взять у серебра
                    val needed = amount - currentStats.copper
                    if (currentStats.silver > 0) {
                        // Размен 1 СМ -> 10 ММ
                        currentStats = currentStats.copy(silver = currentStats.silver - 1, copper = currentStats.copper + 10)
                        // Рекурсивная попытка снова снять
                        return subtractMoneyRecursive(currentStats, "CP", amount)
                    } else if (currentStats.gold > 0) {
                        // Размен 1 ЗМ -> 10 СМ (а потом следующий шаг разменяет СМ на ММ)
                        currentStats = currentStats.copy(gold = currentStats.gold - 1, silver = currentStats.silver + 10)
                        return subtractMoneyRecursive(currentStats, "CP", amount)
                    }
                }
            }
            "SP" -> {
                if (currentStats.silver >= amount) {
                    currentStats = currentStats.copy(silver = currentStats.silver - amount)
                } else {
                    if (currentStats.gold > 0) {
                        // Размен 1 ЗМ -> 10 СМ
                        currentStats = currentStats.copy(gold = currentStats.gold - 1, silver = currentStats.silver + 10)
                        return subtractMoneyRecursive(currentStats, "SP", amount)
                    }
                }
            }
            "GP" -> {
                if (currentStats.gold >= amount) {
                    currentStats = currentStats.copy(gold = currentStats.gold - amount)
                }
            }
        }
        return currentStats
    }

    private fun saveCharacter(character: CharacterDomain) {
        _state.value = _state.value.copy(character = character)
        viewModelScope.launch {
            useCases.saveCharacter(character)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/CharacterSheetViewModel.kt