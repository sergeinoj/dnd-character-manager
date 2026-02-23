package com.dnd.app.domain.usecase

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.MonsterActionEffectEntity
import com.dnd.app.domain.model.monster.EffectTrigger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EffectTriggerSystem @Inject constructor(
    private val dao: ReferenceDao,
    private val json: Json
) {

    suspend fun getTriggers(monsterIndex: String, actionIndex: String?): List<EffectTrigger> {
        if (actionIndex.isNullOrBlank()) return emptyList()
        val effects = dao.getMonsterActionEffects(monsterIndex, actionIndex)
        return effects.map { mapEntity(it) }
    }

    private fun mapEntity(entity: MonsterActionEffectEntity): EffectTrigger {
        return EffectTrigger(
            event = entity.triggerEvent,
            condition = entity.triggerCondition,
            effectType = entity.effectType,
            target = entity.target,
            payloadSummary = describePayload(entity.payloadJson),
            saveDc = entity.saveDcOverride,
            saveStat = entity.saveStat
        )
    }

    private fun describePayload(payload: String?): String? {
        if (payload.isNullOrBlank()) return null
        val parsed = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null
        return parsed.entries.joinToString { (key, value) ->
            val valueText = value.jsonPrimitive.contentOrNull ?: value.toString()
            "$key=$valueText"
        }
    }
}
