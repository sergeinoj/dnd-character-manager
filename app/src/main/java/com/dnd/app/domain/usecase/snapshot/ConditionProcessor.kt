package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.data.local.entity.ConditionEntity
import com.dnd.app.domain.model.condition.ConditionDefinition
import com.dnd.app.domain.model.condition.ConditionMechanics
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConditionProcessor @Inject constructor(
    private val json: Json
) {

    fun toDefinition(entity: ConditionEntity): ConditionDefinition {
        return ConditionDefinition(
            indexName = entity.indexName,
            name = entity.name,
            description = entity.description,
            uiColorHex = entity.uiColorHex,
            mechanics = parseMechanics(entity.mechanicsJson)
        )
    }

    fun parseMechanics(raw: String): ConditionMechanics {
        val trimmed = raw.trim()
        return runCatching {
            json.decodeFromString(ConditionMechanics.serializer(), trimmed)
        }.getOrDefault(ConditionMechanics())
    }
}
