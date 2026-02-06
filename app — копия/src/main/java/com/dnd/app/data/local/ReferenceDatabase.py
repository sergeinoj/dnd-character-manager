// Имя файла: app/src/main/java/com/dnd/app/data/local/ReferenceDatabase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.ClassRawEntity
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.data.local.entity.RaceRawEntity
import com.dnd.app.data.local.entity.SpellEntity
import com.dnd.app.data.local.entity.WeaponEntity

@Database(
    entities = [
        SpellEntity::class,
        WeaponEntity::class,
        FeatureEntity::class,
        RaceRawEntity::class,
        ClassRawEntity::class
    ],
    version = 4, // Увеличиваем версию просто для порядка
    exportSchema = false
)
// TypeConverter больше не нужен
abstract class ReferenceDatabase : RoomDatabase() {
    abstract fun referenceDao(): ReferenceDao
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/ReferenceDatabase.kt