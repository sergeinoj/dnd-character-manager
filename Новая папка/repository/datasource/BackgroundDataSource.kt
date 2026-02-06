// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/BackgroundDataSource.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.entity.BackgroundEntity

/**
 * "Тупой" источник данных для предысторий.
 * Только чтение сырых сущностей из БД. Никакой логики парсинга.
 */
interface BackgroundDataSource {
    suspend fun loadAllBackgroundEntities(): List<BackgroundEntity>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/BackgroundDataSource.kt