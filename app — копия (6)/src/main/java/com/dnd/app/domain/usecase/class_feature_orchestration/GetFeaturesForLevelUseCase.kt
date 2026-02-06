// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/GetFeaturesForLevelUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import android.util.Log
import com.dnd.app.domain.model.ClassFeaturesForLevel
import com.dnd.app.domain.model.Feature
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetFeaturesForLevelUseCase @Inject constructor(
    private val dataSource: ClassFeatureRepository, // ИЗМЕНЕНО
    private val featureFactory: FeatureFactory,
    private val progressionUseCase: ClassProgressionUseCase,
    private val enricher: FeatureEnricher
) {
    private val TAG = "DND_DEBUG_ORCHESTRATOR"

    suspend operator fun invoke(
        classIndex: String,
        level: Int,
        subclassIndex: String?,
        abilityModifier: Int
    ): ClassFeaturesForLevel = coroutineScope {
        val classEntity = dataSource.getClassEntity(classIndex) ?: return@coroutineScope ClassFeaturesForLevel()

        val baseFeatures = mutableListOf<Feature>()
        val subclassFeatures = mutableListOf<Feature>()
        var subclassChoiceFeature: Feature? = null

        val virtualFeaturesDeferred = async {
            if (level == 1) {
                progressionUseCase.generateInitialFeatures(classEntity, abilityModifier)
            } else {
                progressionUseCase.generateLevelUpFeatures(classIndex, level)
            }
        }

        val staticFeaturesDeferred = async {
            val allProgressionRows = dataSource.getProgressionForLevel(classIndex, level)
            val featureIndexes = allProgressionRows.flatMap { row ->
                row.featureIndicesJson?.let { runCatching { kotlinx.serialization.json.Json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
            }
            val featuresFromContext = dataSource.findFeaturesByContext(classIndex, subclassIndex, level).map { it.indexName }

            val allIndexes = (featureIndexes + featuresFromContext).distinct()
            dataSource.getFeaturesByIndexes(allIndexes)
        }

        val allEntities = staticFeaturesDeferred.await()

        for (entity in allEntities) {
            val featureDomain = featureFactory.create(entity)
            val enrichedFeature = enricher.enrich(featureDomain, entity)

            // [ИЗМЕНЕНО v1.26] Четкое разделение и установка приоритета
            when {
                enrichedFeature.uiGroup == "SUBCLASS_CHOICE" -> {
                    subclassChoiceFeature = enrichedFeature.copy(priority = 1) // Высший приоритет
                }
                enrichedFeature.subclassIndex != null -> {
                    // Это способность подкласса, добавляем только если подкласс выбран
                    if (enrichedFeature.subclassIndex == subclassIndex) {
                        subclassFeatures.add(enrichedFeature)
                    }
                }
                else -> {
                    // Это способность базового класса
                    baseFeatures.add(enrichedFeature)
                }
            }
        }

        baseFeatures.addAll(virtualFeaturesDeferred.await())

        Log.d(TAG, "Level $level for $classIndex ($subclassIndex): Base feats: ${baseFeatures.size}, Subclass feats: ${subclassFeatures.size}, Choice: ${subclassChoiceFeature != null}")

        return@coroutineScope ClassFeaturesForLevel(
            baseClassFeatures = baseFeatures.distinctBy { it.index },
            subclassChoiceFeature = subclassChoiceFeature,
            selectedSubclassFeatures = subclassFeatures.distinctBy { it.index }
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/GetFeaturesForLevelUseCase.kt