// Имя файла: util/SchemaDumper.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object SchemaDumper {
    fun dump(context: Context) {
        Thread {
            try {
                // 1. Копируем базу из assets во временное место, чтобы открыть её напрямую
                val dbName = "dnd_database.db"
                val dbFile = context.getDatabasePath("temp_dump.db")

                if (!dbFile.parentFile.exists()) dbFile.parentFile.mkdirs()

                context.assets.open("database/$dbName").use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // 2. Открываем базу через стандартный Android API (не Room)
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )

                Log.e("DB_INSPECTOR", "================ СТАРТ АНАЛИЗА БАЗЫ ================")

                // 3. Читаем список всех таблиц
                val cursor = db.rawQuery("SELECT name, sql FROM sqlite_master WHERE type='table'", null)

                if (cursor.moveToFirst()) {
                    do {
                        val tableName = cursor.getString(0)
                        val createSql = cursor.getString(1)

                        // Игнорируем служебные таблицы Android
                        if (tableName != "android_metadata" && tableName != "sqlite_sequence") {
                            Log.e("DB_INSPECTOR", "ТАБЛИЦА: $tableName")
                            Log.e("DB_INSPECTOR", "СХЕМА: $createSql")
                            Log.e("DB_INSPECTOR", "------------------------------------------------")
                        }
                    } while (cursor.moveToNext())
                } else {
                    Log.e("DB_INSPECTOR", "Таблицы не найдены!")
                }

                cursor.close()
                db.close()
                Log.e("DB_INSPECTOR", "================ КОНЕЦ АНАЛИЗА БАЗЫ ================")

            } catch (e: Exception) {
                Log.e("DB_INSPECTOR", "ОШИБКА АНАЛИЗА: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: util/SchemaDumper.kt