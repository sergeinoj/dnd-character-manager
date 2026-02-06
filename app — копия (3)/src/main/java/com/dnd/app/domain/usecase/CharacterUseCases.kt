// Имя файла: domain/usecase/CharacterUseCases.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.repository.CharacterRepository
import javax.inject.Inject

// @Inject говорит Hilt'у, как создавать этот класс. Модуль не нужен.
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
    suspend operator fun invoke(id: Long): CharacterDomain? {
        return repository.getCharacterById(id)
    }
}

class SaveCharacterUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke(character: CharacterDomain): Long {
        if (character.name.isBlank()) {
            throw IllegalArgumentException("Character name cannot be empty")
        }
        return repository.saveCharacter(character)
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
// Имя файла: domain/usecase/CharacterUseCases.kt