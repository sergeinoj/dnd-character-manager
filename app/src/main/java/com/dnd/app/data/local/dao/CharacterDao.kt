// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\dao\CharacterDao.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.dnd.app.data.local.entity.CharacterEntity
import com.dnd.app.domain.model.DraftCharacter
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY id DESC")
    fun getAllCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: Long): CharacterEntity?

    @Query("SELECT draft_data FROM characters WHERE id = :id")
    suspend fun getDraftRawJson(id: Long): String?

    @Query("SELECT * FROM characters WHERE id = :id")
    fun getCharacterFlowById(id: Long): Flow<CharacterEntity?>


    @Upsert
    suspend fun upsertCharacter(character: CharacterEntity): Long


    @Query("""
        UPDATE characters SET
        version_id = :newVersion,
        draft_data = :draftData,
        snapshot_json = :snapshotJson,
        live_state_json = :liveStateJson,
        name = :name,
        race_name = :raceName,
        class_name = :className,
        level = :level,
        hp_current = :hpCurrent,
        hp_max = :hpMax
        WHERE id = :id AND version_id = :expectedVersion
    """)
    suspend fun updateWithOptimisticLock(
        id: Long,
        expectedVersion: Long,
        newVersion: Long,
        draftData: DraftCharacter?,
        snapshotJson: String,
        liveStateJson: String,
        name: String,
        raceName: String,
        className: String,
        level: Int,
        hpCurrent: Int,
        hpMax: Int
    ): Int

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\dao\CharacterDao.kt
