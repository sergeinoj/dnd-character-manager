// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/PartitionClassFeaturesUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ClassFeaturesForLevel
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.PartitionedFeatures
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [НОВЫЙ ФАЙЛ v1.26]
 * UseCase, отвечающий за разделение "сырого" набора способностей класса
 * на логические группы для отображения в разных частях UI (вкладка Класс, вкладка Вещи).
 */
@Singleton
class PartitionClassFeaturesUseCase @Inject constructor() {

    operator fun invoke(features: ClassFeaturesForLevel): PartitionedFeatures {
        val classFeatures = mutableListOf<Feature>()
        val inventoryFeatures = mutableListOf<Feature>()

        // 1. Разделяем базовые способности по их UI-группе
        features.baseClassFeatures.forEach { feature ->
            when (feature.uiGroup) {
                "INVENTORY" -> inventoryFeatures.add(feature)
                else -> classFeatures.add(feature) // Все остальное идет во вкладку "Класс"
            }
        }

        // 2. Способности выбранного подкласса всегда относятся к классу
        classFeatures.addAll(features.selectedSubclassFeatures)

        return PartitionedFeatures(
            classSkillFeatures = classFeatures.distinctBy { it.index },
            inventoryChoiceFeatures = inventoryFeatures.distinctBy { it.index },
            subclassChoiceFeature = features.subclassChoiceFeature
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/PartitionClassFeaturesUseCase.kt