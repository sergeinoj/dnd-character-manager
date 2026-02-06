// Имя файла: app/src/main/java/com/dnd/app/domain/model/creator/ClassStepData.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model.creator

import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.SubclassInfo

/**
 * [НОВЫЙ ФАЙЛ]
 * Структурированный контейнер данных для вкладки "Класс".
 * Является результатом работы GetClassStepDataUseCase.
 */
data class ClassStepData(
    val classFeatures: List<Feature> = emptyList(),
    val inventoryChoiceFeatures: List<Feature> = emptyList(),
    val subclassChoiceFeature: Feature? = null,
    val aggregatedSpellFeature: Feature? = null,
    val availableSubclasses: List<SubclassInfo> = emptyList()
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/creator/ClassStepData.kt