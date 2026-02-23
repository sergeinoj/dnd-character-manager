package com.dnd.app.domain.usecase

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.ProgressionEntity
import com.dnd.app.domain.model.MonsterFilter
import com.dnd.app.domain.model.MonsterRecord
import com.dnd.app.data.repository.datasource.MonstersDataSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class GetAvailableShapesUseCase @Inject constructor(
    private val monstersDataSource: MonstersDataSource,
    private val referenceDao: ReferenceDao,
    private val json: Json
) {
    private data class WildShapeRules(
        val maxCr: Double?,
        val canSwim: Boolean?,
        val canFly: Boolean?
    )

    suspend operator fun invoke(filter: MonsterFilter? = null, context: WildShapeContext? = null): List<MonsterRecord> {
        val all = monstersDataSource.listMonsters()
        val rules = context?.let {
            try {
                loadWildShapeRules(it)
            } catch (_: Throwable) {
                null
            }
        }
        val filteredByMovement = applyMovementRules(all, rules)
        val effectiveFilter = mergeFilters(filter, rules?.maxCr)
        return effectiveFilter?.let { applyFilter(filteredByMovement, it) } ?: filteredByMovement
    }

    private fun applyFilter(monsters: List<MonsterRecord>, filter: MonsterFilter): List<MonsterRecord> {
        if (filter.minChallenge == null && filter.maxChallenge == null) return monsters
        return monsters.filter { monster ->
            val cr = monster.challengeRating
            val matchesMin = filter.minChallenge?.let { cr != null && cr >= it } ?: true
            val matchesMax = filter.maxChallenge?.let { cr != null && cr <= it } ?: true
            matchesMin && matchesMax
        }
    }

    private suspend fun loadWildShapeRules(context: WildShapeContext): WildShapeRules? {
        val progression = referenceDao.getProgressionForLevelAndSubclass(
            classIndex = context.classIndex,
            level = context.level,
            subclassIndex = context.subclassIndex
        ).firstOrNull()
        return progression?.let(::extractWildShapeRules)
    }

    private fun extractWildShapeRules(entity: ProgressionEntity): WildShapeRules {
        val base = parseJsonNumber(entity.classSpecificJson, "wild_shape_max_cr")
        val bonus = parseJsonNumber(entity.subclassSpecificJson, "wild_shape_bonus_cr")
        val canSwim = parseJsonBoolean(entity.classSpecificJson, "wild_shape_swim")
        val canFly = parseJsonBoolean(entity.classSpecificJson, "wild_shape_fly")
        val maxCr = when {
            base != null -> base + (bonus ?: 0.0)
            bonus != null -> bonus
            else -> null
        }
        return WildShapeRules(maxCr = maxCr, canSwim = canSwim, canFly = canFly)
    }

    private fun parseJsonNumber(raw: String?, key: String): Double? {
        val jsonObject = parseJsonObject(raw) ?: return null
        return jsonObject[key]?.jsonPrimitive?.doubleOrNull
    }

    private fun parseJsonBoolean(raw: String?, key: String): Boolean? {
        val jsonObject = parseJsonObject(raw) ?: return null
        return jsonObject[key]?.jsonPrimitive?.booleanOrNull
    }

    private fun parseJsonObject(raw: String?): JsonObject? {
        if (raw.isNullOrBlank()) return null
        fun parse(candidate: String): JsonObject? {
            return runCatching { json.parseToJsonElement(candidate) }.getOrNull() as? JsonObject
        }
        parse(raw)?.let { return it }

        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) {
            val wrapped = "{$trimmed}"
            parse(wrapped)?.let { return it }
        }
        return null
    }

    private fun mergeFilters(base: MonsterFilter?, limit: Double?): MonsterFilter? {
        if (base == null && limit == null) return null
        val minChallenge = base?.minChallenge
        val maxCandidates = mutableListOf<Double>()
        base?.maxChallenge?.let { maxCandidates.add(it) }
        limit?.let { maxCandidates.add(it) }
        val maxChallenge = maxCandidates.minOrNull()
        return MonsterFilter(minChallenge = minChallenge, maxChallenge = maxChallenge)
    }

    private fun applyMovementRules(monsters: List<MonsterRecord>, rules: WildShapeRules?): List<MonsterRecord> {
        if (rules == null) return monsters
        return monsters.filter { monster ->
            val hasSwim = monster.speed.keys.any { it.equals("swim", ignoreCase = true) }
            val hasFly = monster.speed.keys.any { it.equals("fly", ignoreCase = true) }
            val swimAllowed = rules.canSwim ?: true
            val flyAllowed = rules.canFly ?: true
            (!hasSwim || swimAllowed) && (!hasFly || flyAllowed)
        }
    }
}

data class WildShapeContext(
    val classIndex: String,
    val subclassIndex: String? = null,
    val level: Int
)
