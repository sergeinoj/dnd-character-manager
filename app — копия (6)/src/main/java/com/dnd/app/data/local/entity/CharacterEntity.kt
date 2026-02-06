// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/CharacterEntity.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dnd.app.domain.model.DraftCharacter

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,

    // Поля "Кэша" (для отображения в списке без десериализации всего)
    @ColumnInfo(name = "race_name") val raceName: String = "",
    @ColumnInfo(name = "class_name") val className: String = "",
    @ColumnInfo(name = "level") val level: Int = 1,
    @ColumnInfo(name = "hp_current") val hpCurrent: Int = 10,
    @ColumnInfo(name = "hp_max") val hpMax: Int = 10,

    // JSON-поля для готового листа (Domain Model)
    @ColumnInfo(name = "stats_json") val statsJson: String,
    @ColumnInfo(name = "inventory_ids_json") val inventoryIdsJson: String,
    @ColumnInfo(name = "spells_known_ids_json") val spellsKnownIdsJson: String,
    @ColumnInfo(name = "bio_json") val bioJson: String,
    @ColumnInfo(name = "skill_proficiencies_json") val skillProficienciesJson: String,

    /**
     * "Мастер-лента" (Master Tape).
     * Здесь хранится полная история создания персонажа (DraftCharacter).
     * При загрузке листа мы можем пересобрать (re-assemble) персонажа из этого поля,
     * если логика игры изменится.
     */
    @ColumnInfo(name = "draft_data")
    val draftData: DraftCharacter? = null
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/CharacterEntity.kt