// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\AppDatabase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dnd.app.data.local.converters.DraftConverters
import com.dnd.app.data.local.converters.SnapshotConverters
import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.entity.CharacterEntity


@Database(
    entities = [CharacterEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(DraftConverters::class, SnapshotConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao

    companion object {

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {

            }
        }


        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = OFF")

                db.execSQL("DROP TABLE IF EXISTS `characters_new` ")


                db.execSQL("""
                    CREATE TABLE `characters_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `version_id` INTEGER NOT NULL DEFAULT 0,
                        `race_name` TEXT NOT NULL,
                        `class_name` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `hp_current` INTEGER NOT NULL,
                        `hp_max` INTEGER NOT NULL,
                        `stats_json` TEXT NOT NULL,
                        `inventory_ids_json` TEXT NOT NULL,
                        `spells_known_ids_json` TEXT NOT NULL,
                        `bio_json` TEXT NOT NULL,
                        `skill_proficiencies_json` TEXT NOT NULL,
                        `draft_data` TEXT,
                        `snapshot_json` TEXT,
                        `live_state_json` TEXT
                    )
                """)


                db.execSQL("""
                    INSERT INTO `characters_new` (
                        id, name, version_id, race_name, class_name, level,
                        hp_current, hp_max, stats_json, inventory_ids_json,
                        spells_known_ids_json, bio_json, skill_proficiencies_json,
                        draft_data, snapshot_json, live_state_json
                    )
                    SELECT
                        id, name, version_id, race_name, class_name, level,
                        hp_current, hp_max, stats_json, inventory_ids_json,
                        spells_known_ids_json, bio_json, skill_proficiencies_json,
                        draft_data, snapshot_json, live_state_json
                    FROM `characters`
                """)


                db.execSQL("DROP TABLE `characters` ")
                db.execSQL("ALTER TABLE `characters_new` RENAME TO `characters` ")


                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS conditions (
                        index_name TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        ui_color_hex TEXT,
                        mechanics_json TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS monster_action_effects (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        monster_index TEXT NOT NULL,
                        action_index TEXT NOT NULL,
                        trigger_event TEXT NOT NULL,
                        trigger_condition TEXT,
                        effect_type TEXT NOT NULL,
                        target TEXT NOT NULL DEFAULT 'TARGET',
                        payload_json TEXT NOT NULL,
                        save_dc_override INTEGER,
                        save_stat TEXT,
                        FOREIGN KEY(monster_index) REFERENCES monsters(index_name) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS idx_effects_lookup ON monster_action_effects(monster_index, action_index)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS monster_attack_patterns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        monster_index TEXT NOT NULL,
                        pattern_slug TEXT NOT NULL,
                        logic_operator TEXT NOT NULL,
                        description TEXT,
                        FOREIGN KEY(monster_index) REFERENCES monsters(index_name) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS monster_attack_pattern_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        pattern_id INTEGER NOT NULL,
                        entry_type TEXT NOT NULL,
                        entry_index TEXT NOT NULL,
                        count INTEGER DEFAULT 1,
                        FOREIGN KEY(pattern_id) REFERENCES monster_attack_patterns(id) ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\AppDatabase.kt
