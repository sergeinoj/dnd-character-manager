// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\GetFeaturesForLevelUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import com.dnd.app.domain.model.ClassFeaturesForLevel
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.SelectionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class GetFeaturesForLevelUseCase @Inject constructor(
    private val dataSource: ClassFeatureRepository,
    private val featureFactory: FeatureFactory,
    private val progressionUseCase: ClassProgressionUseCase,
    private val enricher: FeatureEnricher
) {

    private val genesisProvider by lazy { GenesisFeatureProvider(dataSource, featureFactory, progressionUseCase) }
    private val multiclassProvider by lazy { MulticlassDipProvider(dataSource, featureFactory, progressionUseCase) }
    private val growthProvider by lazy { GrowthFeatureProvider(dataSource, featureFactory, progressionUseCase) }

    suspend fun invokeWithContext(
        classIndex: String,
        level: Int,
        subclassIndex: String?,
        abilityModifier: Int,
        isGenesis: Boolean,
        proficiencyProvider: (() -> Map<String, Int>)? = null
    ): ClassFeaturesForLevel = withContext(Dispatchers.Default) {

        val classEntity = dataSource.getClassEntity(classIndex)
            ?: return@withContext ClassFeaturesForLevel()

        val internalData = when {
            level == 1 && isGenesis -> {
                genesisProvider.provide(classEntity, subclassIndex, abilityModifier, proficiencyProvider)
            }
            level == 1 && !isGenesis -> {
                multiclassProvider.provide(classEntity, subclassIndex, abilityModifier, proficiencyProvider)
            }
            else -> {
                growthProvider.provide(classIndex, level, subclassIndex, proficiencyProvider)
            }
        }

        val result = internalData.result
        val entitiesMap = internalData.sourceEntities.associateBy { it.indexName }

        suspend fun enrichIfPossible(feature: Feature): Feature {
            val entity = entitiesMap[feature.index]
            return if (entity != null) {
                enricher.enrich(feature, entity, SelectionSource.CLASS)
            } else {
                feature
            }
        }

        result.copy(
            baseClassFeatures = result.baseClassFeatures.map { enrichIfPossible(it) },
            selectedSubclassFeatures = result.selectedSubclassFeatures.map { enrichIfPossible(it) },
            subclassChoiceFeature = result.subclassChoiceFeature?.let { enrichIfPossible(it) }
        )
    }

    suspend operator fun invoke(
        classIndex: String,
        level: Int,
        subclassIndex: String?,
        abilityModifier: Int,
        proficiencyProvider: (() -> Map<String, Int>)? = null
    ): ClassFeaturesForLevel = invokeWithContext(
        classIndex = classIndex,
        level = level,
        subclassIndex = subclassIndex,
        abilityModifier = abilityModifier,
        isGenesis = true,
        proficiencyProvider = proficiencyProvider
    )
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\GetFeaturesForLevelUseCase.kt