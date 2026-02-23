// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\repository\BackgroundRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.repository

import com.dnd.app.domain.model.Background


interface BackgroundRepository {
    suspend fun getBackgrounds(): List<Background>
    suspend fun getBackgroundByIndex(index: String): Background?
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\repository\BackgroundRepository.kt