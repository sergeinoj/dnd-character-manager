// Имя файла: app/src/main/java/com/dnd/app/domain/model/creator/HandleSelectionResult.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model.creator

import com.dnd.app.domain.model.DraftCharacter

/**
 * [НОВЫЙ ФАЙЛ]
 * Результат работы HandleSelectionUseCase, который может содержать
 * как обновленный черновик, так и сайд-эффект для ViewModel (например, необходимость загрузить детали черты).
 */
data class HandleSelectionResult(
    val draft: DraftCharacter,
    val featToLoad: String? = null
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/creator/HandleSelectionResult.kt