// Имя файла: app/src/main/java/com/dnd/app/domain/repository/BackgroundRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.repository

import com.dnd.app.domain.model.Background

/**
 * Репозиторий для работы с предысториями.
 * Отвечает за трансформацию Entity в Domain модели и разрешение зависимых индексов.
 */
interface BackgroundRepository {
    suspend fun getBackgrounds(): List<Background>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/repository/BackgroundRepository.kt