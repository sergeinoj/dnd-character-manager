// Имя файла: ui/screens/character_list/CharacterListViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.usecase.CharacterUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterListViewModel @Inject constructor(
    private val useCases: CharacterUseCases
) : ViewModel() {

    // Подписываемся на поток персонажей из БД
    val characters: StateFlow<List<CharacterDomain>> = useCases.getAllCharacters()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            useCases.deleteCharacter(id)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_list/CharacterListViewModel.kt