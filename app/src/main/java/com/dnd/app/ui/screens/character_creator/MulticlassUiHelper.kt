package com.dnd.app.ui.screens.character_creator

import com.dnd.app.domain.model.ClassInfo
import com.dnd.app.util.DndLocalization

data class MulticlassEvaluation(
    val meetsRequirements: Boolean,
    val failureMessage: String?,
    val requirementLabel: String?
)

fun evaluateClassMulticlassRequirements(classInfo: ClassInfo, stats: Map<String, Int>): MulticlassEvaluation {
    val requirements = classInfo.multiclassRequirements
    if (requirements.isEmpty()) {
        return MulticlassEvaluation(true, null, null)
    }

    val requirementLabel = requirements.joinToString("; ") { requirement ->
        "нужно ${DndLocalization.translateStat(requirement.stat)} ${requirement.minScore}"
    }

    val failedRequirement = requirements.firstOrNull { requirement ->
        stats.getOrDefault(requirement.stat, 0) < requirement.minScore
    }

    val failureMessage = failedRequirement?.let {
        "Мультиклассирование недоступно, нужно ${DndLocalization.translateStat(it.stat)} ${it.minScore}"
    }

    return MulticlassEvaluation(
        meetsRequirements = failedRequirement == null,
        failureMessage = failureMessage,
        requirementLabel = requirementLabel
    )
}
