// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\PartitionClassFeaturesUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ClassFeaturesForLevel
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.PartitionedFeatures
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PartitionClassFeaturesUseCase @Inject constructor() {

    operator fun invoke(features: ClassFeaturesForLevel): PartitionedFeatures {
        val classFeatures = mutableListOf<Feature>()
        val inventoryFeatures = mutableListOf<Feature>()


        features.baseClassFeatures.forEach { feature ->
            when (feature.uiGroup) {
                "INVENTORY" -> inventoryFeatures.add(feature)

                "SUBCLASS_CHOICE" -> {  }
                else -> classFeatures.add(feature)
            }
        }


        classFeatures.addAll(features.selectedSubclassFeatures)

        return PartitionedFeatures(
            classSkillFeatures = classFeatures.distinctBy { it.index },
            inventoryChoiceFeatures = inventoryFeatures.distinctBy { it.index },
            subclassChoiceFeature = features.subclassChoiceFeature
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\PartitionClassFeaturesUseCase.kt