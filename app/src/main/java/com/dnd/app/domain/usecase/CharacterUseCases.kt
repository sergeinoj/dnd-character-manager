// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\CharacterUseCases.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.repository.CharacterRepository
import javax.inject.Inject

data class CharacterUseCases @Inject constructor(
    val getAllCharacters: GetAllCharactersUseCase,
    val getCharacter: GetCharacterByIdUseCase,
    val saveCharacter: SaveCharacterUseCase,
    val deleteCharacter: DeleteCharacterUseCase
)

class GetAllCharactersUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    operator fun invoke() = repository.getAllCharacters()
}

class GetCharacterByIdUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    operator fun invoke(id: Long) = repository.getCharacterForSheet(id)
}


class SaveCharacterUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.syncLiveState(id) { currentInDb -> currentInDb }
    }
}

class DeleteCharacterUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteCharacter(id)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\CharacterUseCases.kt