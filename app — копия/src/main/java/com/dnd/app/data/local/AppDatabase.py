// Имя файла: app/src/main/java/com/dnd/app/data/local/AppDatabase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.entity.CharacterEntity

@Database(
    entities = [CharacterEntity::class],
    version = 3, // Увеличиваем версию
    exportSchema = false
)
// УБРАН TypeConverter
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/AppDatabase.kt