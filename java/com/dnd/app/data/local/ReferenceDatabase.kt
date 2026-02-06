// Имя файла: app/src/main/java/com/dnd/app/data/local/ReferenceDatabase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*

@Database(
    entities = [
        ClassEntity::class,
        SubclassEntity::class,
        ProgressionEntity::class,
        FeatureEntity::class,
        RaceEntity::class,
        SubraceEntity::class,
        BackgroundEntity::class,
        AlignmentEntity::class,
        SpellEntity::class,
        MagicSchoolEntity::class,
        EquipmentEntity::class,
        WeaponEntity::class,
        ArmorEntity::class,
        MagicItemEntity::class,
        WeaponPropertyEntity::class,
        ProficiencyEntity::class,
        SkillEntity::class,
        LanguageEntity::class,
        DamageTypeEntity::class,
        EquipmentCategoryEntity::class,
        EquipmentCategoryLinkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ReferenceDatabase : RoomDatabase() {
    abstract fun referenceDao(): ReferenceDao
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/ReferenceDatabase.kt