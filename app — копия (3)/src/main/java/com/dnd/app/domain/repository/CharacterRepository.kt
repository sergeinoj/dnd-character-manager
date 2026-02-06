// Имя файла: domain/repository/CharacterRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.repository

import com.dnd.app.domain.model.CharacterDomain
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun getAllCharacters(): Flow<List<CharacterDomain>>
    suspend fun getCharacterById(id: Long): CharacterDomain?
    suspend fun saveCharacter(character: CharacterDomain): Long
    suspend fun deleteCharacter(characterId: Long)
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: domain/repository/CharacterRepository.kt