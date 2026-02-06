// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/CharacterEntity.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "race_id")
    val raceId: Int,
    @ColumnInfo(name = "class_id")
    val classId: Int,
    @ColumnInfo(name = "level")
    val level: Int,
    @ColumnInfo(name = "hp_current")
    val hpCurrent: Int,
    @ColumnInfo(name = "hp_max")
    val hpMax: Int,
    @ColumnInfo(name = "stats_json")
    val statsJson: String,
    @ColumnInfo(name = "inventory_ids_json")
    val inventoryIdsJson: String,
    @ColumnInfo(name = "spells_known_ids_json")
    val spellsKnownIdsJson: String,
    @ColumnInfo(name = "bio_json")
    val bioJson: String,
    // Вот поле, на отсутствие которого ругался компилятор
    @ColumnInfo(name = "skill_proficiencies_json")
    val skillProficienciesJson: String
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/CharacterEntity.kt