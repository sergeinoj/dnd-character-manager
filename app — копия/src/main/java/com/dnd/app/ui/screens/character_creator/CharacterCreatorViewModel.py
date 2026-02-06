// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.model.ClassInfo
import com.dnd.app.domain.model.Race
import com.dnd.app.domain.model.Stats
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.rules.DndRules
import com.dnd.app.domain.usecase.CharacterUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WizardState(
    val draft: DraftCharacter = DraftCharacter(),
    val availableRaces: List<Race> = emptyList(),
    val availableClasses: List<ClassInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isSaveEnabled: Boolean = false
)

@HiltViewModel
class CharacterCreatorViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val useCases: CharacterUseCases
) : ViewModel() {
    private val _state = MutableStateFlow(WizardState())
    val state = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val races = libraryRepository.getAllRaces()
                val classes = libraryRepository.getAllClasses()
                _state.value = _state.value.copy(
                    availableRaces = races,
                    availableClasses = classes,
                    isLoading = false
                )
                updateDraft(_state.value.draft)
            } catch (e: Exception) {
                // Логируем ошибку, если БД все же не загрузилась
                Log.e("CreatorVM", "Failed to load initial data", e)
                _state.value = _state.value.copy(isLoading = false) // Убираем вечный лоадер
            }
        }
    }

    private fun updateDraft(newDraft: DraftCharacter) {
        val isValid = newDraft.name.isNotBlank() && newDraft.raceId != null && newDraft.levels.isNotEmpty()
        _state.value = _state.value.copy(draft = newDraft, isSaveEnabled = isValid)
    }

    fun updateName(newName: String) = updateDraft(_state.value.draft.copy(name = newName))

    fun updateBaseStat(statCode: String, delta: Int) {
        val draft = _state.value.draft
        val currentVal = when (statCode) {
            "STR" -> draft.baseStr
            "DEX" -> draft.baseDex
            "CON" -> draft.baseCon
            "INT" -> draft.baseInt
            "WIS" -> draft.baseWis
            "CHA" -> draft.baseCha
            else -> return
        }
        val newVal = (currentVal + delta).coerceIn(DndRules.MIN_SCORE, DndRules.MAX_SCORE)
        if (newVal == currentVal) return
        val costDiff = DndRules.getPointCost(newVal) - DndRules.getPointCost(currentVal)
        if (delta > 0 && costDiff > draft.pointsRemaining) return
        val newDraft = when (statCode) {
            "STR" -> draft.copy(baseStr = newVal)
            "DEX" -> draft.copy(baseDex = newVal)
            "CON" -> draft.copy(baseCon = newVal)
            "INT" -> draft.copy(baseInt = newVal)
            "WIS" -> draft.copy(baseWis = newVal)
            "CHA" -> draft.copy(baseCha = newVal)
            else -> draft
        }
        updateDraft(newDraft)
    }

    fun selectRace(raceIndex: Int) {
        val race = _state.value.availableRaces.getOrNull(raceIndex) ?: return
        updateDraft(
            _state.value.draft.copy(
                raceId = race.id,
                raceName = race.name,
                raceStats = race.statBonuses
            )
        )
    }

    fun selectFirstClass(classIndex: Int) {
        val classInfo = _state.value.availableClasses.getOrNull(classIndex) ?: return
        updateDraft(
            _state.value.draft.copy(
                levels = listOf(
                    ClassLevel(
                        classId = classInfo.id,
                        className = classInfo.name,
                        classLevelIndex = 1,
                        hitDie = classInfo.hitDie
                    )
                )
            )
        )
    }

    fun saveCharacter(onSuccess: () -> Unit) {
        val d = _state.value.draft
        if (!state.value.isSaveEnabled || d.pointsRemaining != 0) return
        viewModelScope.launch {
            val conMod = (d.finalCon - 10) / 2
            val hp = maxOf(1, d.levels.first().hitDie + conMod)
            val domainChar = CharacterDomain(
                name = d.name.trim(), raceId = d.raceId!!, classId = d.levels.first().classId, level = 1, hpCurrent = hp, hpMax = hp,
                stats = Stats(strength = d.finalStr, dexterity = d.finalDex, constitution = d.finalCon, intelligence = d.finalInt, wisdom = d.finalWis, charisma = d.finalCha)
            )
            useCases.saveCharacter(domainChar)
            onSuccess()
        }
    }

    private fun maxOf(a: Int, b: Int): Int {
        return if (a > b) a else b
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt