// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\GrowthFeatureProvider.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import com.dnd.app.domain.model.ClassFeaturesForLevel
import com.dnd.app.domain.model.Feature
import kotlinx.serialization.json.Json


internal class GrowthFeatureProvider(
    private val dataSource: ClassFeatureRepository,
    private val featureFactory: FeatureFactory,
    private val progressionUseCase: ClassProgressionUseCase
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun provide(
        classIndex: String,
        level: Int,
        subclassIndex: String?,
        proficiencyProvider: (() -> Map<String, Int>)?
    ): InternalFeatureData {
        val baseFeatures = mutableListOf<Feature>()
        val subclassFeatures = mutableListOf<Feature>()
        var subclassChoiceFeature: Feature? = null

        val spellDeltas = progressionUseCase.generateLevelUpFeatures(classIndex, level)
        baseFeatures.addAll(spellDeltas)

        val progressionRows = dataSource.getProgressionForLevel(classIndex, level)
        val progressionIndices = progressionRows.flatMap { row ->
            row.featureIndicesJson?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
            } ?: emptyList()
        }.distinct()

        val baseEntities = if (progressionIndices.isNotEmpty()) {
            dataSource.getFeaturesByIndexes(progressionIndices)
        } else {
            emptyList()
        }

        val contextEntities = dataSource.findFeaturesByContext(
            classIndex = classIndex,
            subclassIndex = subclassIndex,
            level = level
        ).filter { entity ->
            entity.subclassIndex != null || entity.uiGroup == "SUBCLASS_CHOICE"
        }

        val allEntities = (baseEntities + contextEntities).distinctBy { it.indexName }

        for (entity in allEntities) {
            val feature = featureFactory.create(entity, proficiencyProvider)

            when {
                entity.uiGroup == "SUBCLASS_CHOICE" -> {
                    subclassChoiceFeature = feature.copy(priority = 1)
                }
                entity.subclassIndex != null -> {
                    if (compareIndexes(entity.subclassIndex, subclassIndex)) {
                        subclassFeatures.add(feature)
                    }
                }
                else -> {
                    baseFeatures.add(feature)
                }
            }
        }

        return InternalFeatureData(
            result = ClassFeaturesForLevel(
                baseClassFeatures = baseFeatures.distinctBy { it.index },
                subclassChoiceFeature = subclassChoiceFeature,
                selectedSubclassFeatures = subclassFeatures.distinctBy { it.index }
            ),
            sourceEntities = allEntities
        )
    }

    private fun compareIndexes(dbIndex: String?, draftIndex: String?): Boolean {
        if (dbIndex == null || draftIndex == null) return false
        if (dbIndex == draftIndex) return true
        return dbIndex.endsWith(draftIndex) || draftIndex.endsWith(dbIndex)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\GrowthFeatureProvider.kt