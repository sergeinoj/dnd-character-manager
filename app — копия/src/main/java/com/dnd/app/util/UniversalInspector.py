// Имя файла: util/UniversalInspector.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log

object UniversalInspector {
    private const val TAG = "DND_FULL_SCAN"

    fun inspectAll(context: Context) {
        Thread {
            try {
                // Используем базу v2, которая точно открывается
                val dbPath = context.getDatabasePath("dnd_fixed_v2.db").absolutePath
                val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

                Log.e(TAG, ">>>>>>>>>>>> СТАРТ ПОЛНОГО СКАНИРОВАНИЯ <<<<<<<<<<<<")

                // 1. СПИСОК ВСЕХ ТАБЛИЦ (вдруг мы пропустили таблицы-справочники?)
                Log.e(TAG, "--- СПИСОК ТАБЛИЦ ---")
                val cursorTables = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
                while (cursorTables.moveToNext()) {
                    val tableName = cursorTables.getString(0)
                    if (tableName != "android_metadata" && tableName != "sqlite_sequence") {
                        Log.e(TAG, "Найдена таблица: $tableName")
                    }
                }
                cursorTables.close()

                // 2. АНАЛИЗ FEATURES (Расы, Классы и т.д.)
                inspectTableGrouped(db, "features", "type", "name")

                // 3. АНАЛИЗ SPELLS (Школы магии)
                inspectTableGrouped(db, "spells", "school_type", "name")

                // 4. АНАЛИЗ WEAPONS (Типы урона)
                inspectTableGrouped(db, "weapons", "damage_type", "label")

                Log.e(TAG, ">>>>>>>>>>>> СКАНИРОВАНИЕ ЗАВЕРШЕНО <<<<<<<<<<<<")
                db.close()

            } catch (e: Exception) {
                Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА СКАНЕРА: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    // Универсальная функция: берет таблицу, группирует по ID и показывает 3 примера названий
    private fun inspectTableGrouped(db: SQLiteDatabase, table: String, groupCol: String, nameCol: String) {
        Log.e(TAG, "--- АНАЛИЗ ТАБЛИЦЫ [$table] ПО КОЛОНКЕ [$groupCol] ---")
        try {
            // Получаем все уникальные ID
            val cursorGroups = db.rawQuery("SELECT DISTINCT $groupCol FROM $table ORDER BY $groupCol", null)
            
            if (cursorGroups.count == 0) {
                Log.e(TAG, "Таблица $table пуста или колонка $groupCol не найдена.")
            }

            while (cursorGroups.moveToNext()) {
                val groupId = cursorGroups.getString(0) // Берем как строку, чтобы не гадать Int/String
                
                // Берем 3 примера для этого ID
                val cursorExamples = db.rawQuery(
                    "SELECT $nameCol FROM $table WHERE $groupCol = ? LIMIT 3",
                    arrayOf(groupId)
                )
                
                val examples = StringBuilder()
                while (cursorExamples.moveToNext()) {
                    if (examples.isNotEmpty()) examples.append(", ")
                    examples.append(cursorExamples.getString(0))
                }
                cursorExamples.close()

                Log.e(TAG, "ID Группы [$groupId] ===> Примеры: $examples")
            }
            cursorGroups.close()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при чтении $table: ${e.message}")
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: util/UniversalInspector.kt