// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\BackgroundDataSource.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.entity.BackgroundEntity


interface BackgroundDataSource {
    suspend fun loadAllBackgroundEntities(): List<BackgroundEntity>
    suspend fun loadBackgroundEntityByIndex(index: String): BackgroundEntity?
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\BackgroundDataSource.kt