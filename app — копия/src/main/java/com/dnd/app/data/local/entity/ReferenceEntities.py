// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/ReferenceEntities.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.Spell
import com.dnd.app.domain.model.Weapon

@Entity(tableName = "races")
data class RaceRawEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "speed") val speed: Int?,
    @ColumnInfo(name = "stats_json") val statsJson: String?,
    @ColumnInfo(name = "languages_json") val languagesJson: String?,
    @ColumnInfo(name = "source_file") val sourceFile: String?
)

@Entity(tableName = "classes")
data class ClassRawEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "hit_die") val hitDie: Int?,
    @ColumnInfo(name = "saving_throws_json") val savingThrowsJson: String?,
    @ColumnInfo(name = "skill_choices_json") val skillChoicesJson: String?,
    @ColumnInfo(name = "source_file") val sourceFile: String?
)

@Entity(tableName = "spells")
data class SpellEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int?,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "level") val level: Int?,
    @ColumnInfo(name = "school") val school: String?,
    @ColumnInfo(name = "casting_time") val castingTime: String?,
    @ColumnInfo(name = "range") val range: String?,
    @ColumnInfo(name = "components_json") val components: String?,
    @ColumnInfo(name = "duration") val duration: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "classes_json") val classesJson: String?,
    // Возвращаемся к Int?, так как это самый безопасный тип для данных 0/1
    @ColumnInfo(name = "concentration") val concentration: Int?,
    @ColumnInfo(name = "ritual") val ritual: Int?
) {
    fun toDomain() = Spell(
        id = id ?: 0,
        name = name ?: "Unknown Spell",
        level = level ?: 0,
        school = school ?: "",
        castingTime = castingTime ?: "",
        range = range ?: "",
        components = components ?: "",
        duration = duration ?: "",
        description = description ?: "",
        isConcentration = concentration == 1,
        isRitual = ritual == 1
    )
}

@Entity(tableName = "weapons")
data class WeaponEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "label") val name: String,
    @ColumnInfo(name = "damage_dice") val damage: String?,
    @ColumnInfo(name = "damage_type") val damageType: String?,
    @ColumnInfo(name = "cost") val cost: Int?,
    @ColumnInfo(name = "weight") val weight: Float?,
    @ColumnInfo(name = "properties_json") val properties: String?
) {
    fun toDomain() = Weapon(
        id = id,
        name = name,
        damage = damage ?: "",
        damageType = damageType ?: "",
        cost = cost?.toString() ?: "",
        weight = weight?.toString() ?: "",
        properties = properties ?: ""
    )
}

@Entity(tableName = "features")
data class FeatureEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "raw_data") val modifiers: String?
) {
    fun toDomain() = Feature(
        id = id,
        name = name,
        type = type.toString(),
        description = description ?: "",
        modifiers = modifiers ?: ""
    )
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/ReferenceEntities.kt