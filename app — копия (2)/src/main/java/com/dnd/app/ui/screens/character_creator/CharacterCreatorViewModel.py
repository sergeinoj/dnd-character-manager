// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.CharacterAssembler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatorUiState(
    val draft: DraftCharacter = DraftCharacter(),
    val availableRaces: List<Race> = emptyList(),
    val availableSubraces: List<Race> = emptyList(),
    val availableClasses: List<ClassInfo> = emptyList(),
    val availableBackgrounds: List<Background> = emptyList(),
    val availableAlignments: List<AlignmentEntity> = emptyList(),
    val raceFeatures: List<Feature> = emptyList(),
    val classFeatures: List<Feature> = emptyList(),
    val backgroundFeatures: List<Feature> = emptyList(),
    val globalExclusions: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CharacterCreatorViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val characterRepository: CharacterRepository,
    private val assembler: CharacterAssembler
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorUiState())
    val uiState = _uiState.asStateFlow()

    init { loadInitialData() }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(
                availableRaces = libraryRepository.getAllParentRaces(),
                availableClasses = libraryRepository.getAllClasses(),
                availableBackgrounds = libraryRepository.getAllBackgrounds(),
                availableAlignments = libraryRepository.getAllAlignments(),
                isLoading = false
            ) }
        }
    }

    private fun updateGlobalExclusions() { _uiState.update { it.copy(globalExclusions = it.draft.getGlobalExclusions()) } }

    fun selectRace(idx: String) {
        viewModelScope.launch {
            val r = _uiState.value.availableRaces.find { it.index == idx } ?: return@launch
            val subraces = libraryRepository.getSubracesFromDb(r.id)
            val features = libraryRepository.getRaceFeatures(r.id, null)
            _uiState.update { it.copy(
                draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(raceIndex = idx, subraceIndex = null, raceSelections = emptyMap(), staticRaceBonuses = r.baseStats)),
                availableSubraces = subraces, raceFeatures = features
            ) }
            updateGlobalExclusions()
        }
    }

    fun selectSubrace(idx: String) {
        viewModelScope.launch {
            val rIdx = _uiState.value.draft.baseInfo.raceIndex
            val r = _uiState.value.availableRaces.find { it.index == rIdx } ?: return@launch
            val feats = libraryRepository.getRaceFeatures(r.id, idx)
            _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(subraceIndex = idx)), raceFeatures = feats) }
            updateGlobalExclusions()
        }
    }

    fun onRaceSelectionChange(fIdx: String, res: ChoiceResult) {
        val sel = _uiState.value.draft.baseInfo.raceSelections.toMutableMap()
        sel[fIdx] = res
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(raceSelections = sel))) }
        updateGlobalExclusions()
    }

    fun selectClass(idx: String) {
        viewModelScope.launch {
            val feats = libraryRepository.getProgressionFeatures(idx, 1, null)
            _uiState.update { it.copy(draft = it.draft.copy(levelStack = listOf(LevelStep(classIndex = idx))), classFeatures = feats) }
            updateGlobalExclusions()
        }
    }

    fun selectSubclass(sIdx: String) {
        val cur = _uiState.value.draft.levelStack.firstOrNull() ?: return
        viewModelScope.launch {
            val feats = libraryRepository.getProgressionFeatures(cur.classIndex, 1, sIdx)
            _uiState.update { it.copy(draft = it.draft.copy(levelStack = listOf(cur.copy(subclassIndex = sIdx))), classFeatures = feats) }
            updateGlobalExclusions()
        }
    }

    fun onClassSelectionChange(fIdx: String, res: ChoiceResult) {
        val stack = _uiState.value.draft.levelStack.toMutableList()
        if (stack.isNotEmpty()) {
            val sel = stack[0].selections.toMutableMap(); sel[fIdx] = res
            stack[0] = stack[0].copy(selections = sel)
            _uiState.update { it.copy(draft = it.draft.copy(levelStack = stack)) }
            updateGlobalExclusions()
        }
    }

    fun selectBackground(bg: Background) {
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(backgroundIndex = bg.name, backgroundSelections = emptyMap())), backgroundFeatures = bg.features) }
        updateGlobalExclusions()
    }

    fun onBgSelectionChange(fIdx: String, res: ChoiceResult) {
        val sel = _uiState.value.draft.baseInfo.backgroundSelections.toMutableMap(); sel[fIdx] = res
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(backgroundSelections = sel))) }
        updateGlobalExclusions()
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
    fun updateStat(s: String, d: Int) {
        val sc = _uiState.value.draft.baseInfo.baseAbilityScores.toMutableMap()
        sc[s] = ((sc[s] ?: 8) + d).coerceIn(8, 15)
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(baseAbilityScores = sc))) }
    }

    fun saveCharacter(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val char = assembler.assemble(_uiState.value.draft)
            characterRepository.saveCharacter(char)
            onSuccess()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt