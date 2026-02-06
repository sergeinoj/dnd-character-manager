// Имя файла: app/src/main/java/com/dnd/app/domain/model/PartitionedFeatures.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

/**
 * [НОВЫЙ ФАЙЛ v1.26]
 * Структурированный контейнер, результат работы PartitionClassFeaturesUseCase.
 * Содержит способности, уже разделенные по их назначению в UI.
 */
data class PartitionedFeatures(
    // Способности для вкладки "Класс" (навыки, заклинания, общие фичи, фичи подкласса)
    val classSkillFeatures: List<Feature>,

    // Способности для вкладки "Вещи" (выбор стартового снаряжения)
    val inventoryChoiceFeatures: List<Feature>,

    // Отдельная способность выбора подкласса для вкладки "Класс"
    val subclassChoiceFeature: Feature?
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/PartitionedFeatures.kt