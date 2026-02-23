// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\GenesisFeatureProvider.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.domain.model.ClassFeaturesForLevel
import com.dnd.app.domain.model.Feature
import kotlinx.serialization.json.Json


internal class GenesisFeatureProvider(
    private val dataSource: ClassFeatureRepository,
    private val featureFactory: FeatureFactory,
    private val progressionUseCase: ClassProgressionUseCase
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun provide(
        classEntity: ClassEntity,
        subclassIndex: String?,
        abilityMod: Int,
        proficiencyProvider: (() -> Map<String, Int>)?
    ): InternalFeatureData {
        val baseFeatures = mutableListOf<Feature>()
        val subclassFeatures = mutableListOf<Feature>()
        var subclassChoiceFeature: Feature? = null

        val virtuals = progressionUseCase.generateInitialFeatures(
            classEntity = classEntity,
            abilityModifier = abilityMod,
            isGenesis = true
        )
        baseFeatures.addAll(virtuals)

        val progRows = dataSource.getProgressionForLevel(classEntity.indexName, 1)
        val progIndices = progRows.flatMap { row ->
            row.featureIndicesJson?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
        }.distinct()

        val baseEntities = if (progIndices.isNotEmpty()) {
            dataSource.getFeaturesByIndexes(progIndices)
        } else {
            emptyList()
        }

        val contextEntities = dataSource.findFeaturesByContext(
            classIndex = classEntity.indexName,
            subclassIndex = subclassIndex,
            level = 1
        ).filter { it.subclassIndex != null || it.uiGroup == "SUBCLASS_CHOICE" }

        val allEntities = (baseEntities + contextEntities).distinctBy { it.indexName }

        allEntities.forEach { entity ->
            val feature = featureFactory.create(entity, proficiencyProvider)

            when {
                entity.uiGroup == "SUBCLASS_CHOICE" -> {
                    subclassChoiceFeature = feature
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
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\GenesisFeatureProvider.kt