// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\repository\CharacterRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.repository

import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.snapshot.CharacterLiveState
import com.dnd.app.domain.model.snapshot.CharacterSnapshot
import com.dnd.app.domain.model.snapshot.SheetCharacter
import kotlinx.coroutines.flow.Flow

typealias MutationResult<T> = Pair<CharacterLiveState, T>

interface CharacterRepository {
    fun getAllCharacters(): Flow<List<CharacterDomain>>

    fun getCharacterForSheet(id: Long): Flow<SheetCharacter?>

    suspend fun commitFullCharacter(draft: DraftCharacter): Result<Long>

    suspend fun syncLiveState(
        id: Long,
        transform: (CharacterLiveState) -> CharacterLiveState
    ): Result<Unit>

    suspend fun <T> performAtomicMutation(
        id: Long,
        block: suspend (snapshot: CharacterSnapshot, liveState: CharacterLiveState, draft: DraftCharacter) -> Result<MutationResult<T>>
    ): Result<T>

    suspend fun deleteCharacter(characterId: Long)

    suspend fun getDraftById(id: Long): DraftCharacter?
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\repository\CharacterRepository.kt