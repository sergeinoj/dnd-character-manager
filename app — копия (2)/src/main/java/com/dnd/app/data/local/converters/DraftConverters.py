// Имя файла: app/src/main/java/com/dnd/app/data/local/converters/DraftConverters.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.converters

import androidx.room.TypeConverter
import com.dnd.app.domain.model.DraftCharacter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Конвертер для сохранения всего объекта DraftCharacter в одну колонку БД.
 * Это упрощает архитектуру: мы не нормализуем черновик, так как он временный и сложный.
 */
class DraftConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromDraft(draft: DraftCharacter?): String? {
        return draft?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toDraft(data: String?): DraftCharacter? {
        return data?.let {
            try {
                json.decodeFromString<DraftCharacter>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/converters/DraftConverters.kt