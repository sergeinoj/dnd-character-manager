// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app/domain/usecase/ValidateMulticlassPrerequisitesUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.SelectionSource
import com.dnd.app.domain.model.ValidationIssue
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassProgressionUseCase
import com.dnd.app.util.DndLocalization
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ValidateMulticlassPrerequisitesUseCase @Inject constructor(
    private val classFeatureRepository: ClassFeatureRepository,
    private val progressionUseCase: ClassProgressionUseCase
) {

    suspend operator fun invoke(
        draft: DraftCharacter,
        targetClassIndex: String,
        currentStats: Map<String, Int>
    ): List<ValidationIssue> {
        val classEntity = classFeatureRepository.getClassEntity(targetClassIndex) ?: return emptyList()
        val requirements = progressionUseCase.parseMulticlassPrerequisites(classEntity)
        if (requirements.isEmpty()) return emptyList()

        val resolvedStats = currentStats.ifEmpty { buildCurrentStats(draft) }
        return requirements.mapNotNull { requirement ->
            val actualScore = resolvedStats[requirement.stat] ?: 0
            if (actualScore >= requirement.minScore) return@mapNotNull null

            val label = DndLocalization.translateStat(requirement.stat)
            val message = "Требуется $label ${requirement.minScore}"
            ValidationIssue(
                source = SelectionSource.CLASS,
                featureName = message,
                missingCount = requirement.minScore - actualScore,
                tabIndex = 1
            )
        }
    }

    private fun buildCurrentStats(draft: DraftCharacter): Map<String, Int> {
        return draft.baseInfo.baseAbilityScores.mapValues { (stat, value) ->
            value + (draft.baseInfo.aggregateStatBonuses[stat] ?: 0)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
