// Имя файла: app/src/main/java/com/dnd/app/data/local/AppDatabase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dnd.app.data.local.converters.DraftConverters
import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.entity.CharacterEntity

@Database(
    entities = [CharacterEntity::class],
    version = 2, // Увеличиваем версию для миграции
    exportSchema = false
)
@TypeConverters(DraftConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/AppDatabase.kt