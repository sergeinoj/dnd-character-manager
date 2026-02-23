package com.dnd.app.domain.usecase

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.ConditionEntity
import com.dnd.app.ui.screens.sheet.ConditionUiModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetConditionsUseCase @Inject constructor(
    private val dao: ReferenceDao
) {
    @Volatile
    private var cached: List<ConditionUiModel>? = null

    suspend operator fun invoke(): List<ConditionUiModel> {
        cached?.let { return it }
        val entities = dao.getAllConditions()
        val models = entities.map { entity ->
            ConditionUiModel(
                indexName = entity.indexName,
                name = entity.name,
                uiColorHex = entity.uiColorHex
            )
        }
        cached = models
        return models
    }
}
