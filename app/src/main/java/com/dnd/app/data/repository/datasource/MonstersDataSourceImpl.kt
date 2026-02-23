// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\MonstersDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.MonsterActionEntity
import com.dnd.app.data.local.entity.MonsterEntity
import com.dnd.app.domain.model.MonsterAction
import com.dnd.app.domain.model.MonsterDamage
import com.dnd.app.domain.model.MonsterRecord
import com.dnd.app.domain.usecase.EffectTriggerSystem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonstersDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao,
    private val triggerSystem: EffectTriggerSystem
) : MonstersDataSource {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun getMonster(index: String): MonsterRecord? {
        val entity = dao.getMonsterByIndex(index) ?: return null
        return mapMonster(entity)
    }

    override suspend fun getMonsters(indexes: List<String>): List<MonsterRecord> {
        if (indexes.isEmpty()) return emptyList()
        return indexes.mapNotNull { idx -> dao.getMonsterByIndex(idx)?.let { mapMonster(it) } }
    }

    override suspend fun listMonsters(): List<MonsterRecord> {
        return dao.getAllMonsters().map { mapMonsterSummary(it) }
    }

    private suspend fun mapMonster(entity: MonsterEntity): MonsterRecord {
        val actions = mutableListOf<MonsterAction>()
        for (actionEntity in dao.getMonsterActions(entity.indexName)) {
            actions.add(mapAction(actionEntity))
        }
        val (resistances, immunities) = parseDamageMods(dao.getMonsterDamageMods(entity.indexName))
        return MonsterRecord(
            index = entity.indexName,
            name = entity.name,
            description = entity.descriptionRu ?: entity.descRu ?: entity.description,
            size = entity.size,
            type = entity.type,
            alignment = entity.alignment,
            armorClass = parseArmorClass(entity.armorClassJson),
            hitPoints = entity.hitPoints,
            speed = parseSpeed(entity.speedJson),
            stats = parseStats(entity.statsJson),
            challengeRating = parseChallengeRating(entity.challengeRating),
            senses = parseSenses(entity.sensesJson),
            languages = parseEntityLanguages(entity.languages),
            damageResistances = resistances,
            damageImmunities = immunities,
            conditionImmunities = parseConditionImmunities(entity.conditionImmunitiesJson),
            actions = actions
        )
    }

    private fun mapMonsterSummary(entity: MonsterEntity): MonsterRecord {
        return MonsterRecord(
            index = entity.indexName,
            name = entity.name,
            description = entity.descriptionRu ?: entity.descRu ?: entity.description,
            size = entity.size,
            type = entity.type,
            alignment = entity.alignment,
            armorClass = parseArmorClass(entity.armorClassJson),
            hitPoints = entity.hitPoints,
            speed = parseSpeed(entity.speedJson),
            stats = parseStats(entity.statsJson),
            challengeRating = parseChallengeRating(entity.challengeRating),
            senses = parseSenses(entity.sensesJson),
            languages = parseEntityLanguages(entity.languages),
            conditionImmunities = parseConditionImmunities(entity.conditionImmunitiesJson),
            actions = emptyList()
        )
    }

    private suspend fun mapAction(entity: MonsterActionEntity): MonsterAction {
        val attackBonus = entity.attackBonus
        val range = parseRange(entity.attackJson)
        val damage = parseDamage(entity.damageJson)
        val triggers = triggerSystem.getTriggers(entity.monsterIndex, entity.actionIndex)
        return MonsterAction(
            name = entity.name,
            description = entity.desc,
            attackBonus = attackBonus,
            range = range,
            damage = damage,
            actionIndex = entity.actionIndex,
            triggers = triggers
        )
    }

    private fun parseJson(raw: String?): JsonElement? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.parseToJsonElement(raw) }.getOrNull()
    }

    private fun asString(el: JsonElement?): String? {
        val p = el as? JsonPrimitive ?: return null
        return p.content
    }

    private fun asInt(el: JsonElement?): Int? {
        val p = el as? JsonPrimitive ?: return null
        return p.content.toIntOrNull()
    }

    private fun parseArmorClass(raw: String?): Int? {
        return when (val el = parseJson(raw)) {
            is JsonPrimitive -> asInt(el)
            is JsonArray -> {
                val first = el.firstOrNull()
                when (first) {
                    is JsonPrimitive -> asInt(first)
                    is JsonObject -> asInt(first["value"])
                    else -> null
                }
            }
            is JsonObject -> asInt(el["value"])
            else -> null
        }
    }

    private fun parseSpeed(raw: String?): Map<String, String> {
        val obj = parseJson(raw) as? JsonObject ?: return emptyMap()
        return obj.mapNotNull { (k, v) -> asString(v)?.let { k to it } }.toMap()
    }

    private fun parseStats(raw: String?): Map<String, Int> {
        val obj = parseJson(raw) as? JsonObject ?: return emptyMap()
        return obj.mapNotNull { (k, v) -> asInt(v)?.let { k.uppercase() to it } }.toMap()
    }

    private fun parseRange(raw: String?): String? {
        val obj = parseJson(raw) as? JsonObject ?: return null
        val reach = asString(obj["reach_ft"]) ?: asInt(obj["reach_ft"])?.toString()
        val range = asString(obj["range_ft"]) ?: asInt(obj["range_ft"])?.toString()
        return when {
            !reach.isNullOrBlank() -> "${reach} ft."
            !range.isNullOrBlank() -> "${range} ft."
            else -> null
        }
    }

    private fun parseDamage(raw: String?): List<MonsterDamage> {
        val arr = parseJson(raw) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val dice = asString(obj["damage_dice"]) ?: asString(obj["dice"]) ?: return@mapNotNull null
            val type = when (val dmgType = obj["damage_type"]) {
                is JsonPrimitive -> dmgType.content
                is JsonObject -> asString(dmgType["index"])
                else -> null
            }
            MonsterDamage(dice = dice, type = type)
        }
    }

    private fun parseSenses(raw: String?): Map<String, String> {
        val obj = parseJson(raw) as? JsonObject ?: return emptyMap()
        return obj.mapNotNull { (key, value) ->
            asString(value)?.let { key to it }
        }.toMap()
    }

    private fun parseChallengeRating(value: Double?): Double? {
        return value?.let {
            if (it % 1.0 == 0.0) it else String.format(Locale.US, "%.3f", it).toDouble()
        }
    }

    private fun parseEntityLanguages(raw: String?): List<String> {
        return raw?.replace(";", ",")
            ?.split(',')
            ?.mapNotNull { it.trim().takeIf { trimmed -> trimmed.isNotBlank() } }
            ?: emptyList()
    }

    private fun parseDamageMods(rows: List<com.dnd.app.data.local.entity.MonsterDamageModEntity>): Pair<List<String>, List<String>> {
        val resistances = rows.filter { it.kind?.contains("resist", ignoreCase = true) == true }
            .mapNotNull { it.value?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
        val immunities = rows.filter { it.kind?.contains("immun", ignoreCase = true) == true }
            .mapNotNull { it.value?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
        return resistances to immunities
    }

    private fun parseConditionImmunities(raw: String?): List<String> {
        val el = parseJson(raw) ?: return emptyList()
        return when (el) {
            is JsonArray -> el.mapNotNull { item ->
                when (item) {
                    is JsonPrimitive -> item.content
                    is JsonObject -> asString(item["name"]) ?: asString(item["index"])
                    else -> null
                }?.trim()?.takeIf { it.isNotBlank() }
            }
            else -> emptyList()
        }.distinct()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\MonstersDataSourceImpl.kt
