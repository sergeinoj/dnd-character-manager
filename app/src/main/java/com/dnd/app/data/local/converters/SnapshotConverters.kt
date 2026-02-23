// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\converters\SnapshotConverters.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.converters

import android.util.Log
import androidx.room.TypeConverter
import com.dnd.app.domain.model.snapshot.CharacterLiveState
import com.dnd.app.domain.model.snapshot.CharacterSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class SnapshotConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val TAG = "SnapshotConverter"


    @TypeConverter
    fun fromSnapshot(snapshot: CharacterSnapshot?): String? {
        return snapshot?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toSnapshot(data: String?): CharacterSnapshot? {
        return data?.let {
            runCatching {
                json.decodeFromString<CharacterSnapshot>(it)
            }.onFailure { e ->
                Log.e(TAG, "Failed to parse CharacterSnapshot JSON.", e)
                Log.e(TAG, "Corrupted data: $it")
            }.getOrNull()
        }
    }


    @TypeConverter
    fun fromLiveState(liveState: CharacterLiveState?): String? {
        return liveState?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toLiveState(data: String?): CharacterLiveState? {
        return data?.let {
            runCatching {
                json.decodeFromString<CharacterLiveState>(it)
            }.onFailure { e ->
                Log.e(TAG, "Failed to parse CharacterLiveState JSON.", e)
                Log.e(TAG, "Corrupted data: $it")
            }.getOrNull()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\converters\SnapshotConverters.kt