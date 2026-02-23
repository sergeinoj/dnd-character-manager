// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\RaceDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.data.local.entity.RaceEntity
import com.dnd.app.data.model.ReferenceJson
import com.dnd.app.domain.model.*
import com.dnd.app.domain.model.snapshot.ResetRule
import com.dnd.app.domain.usecase.class_feature_orchestration.FeatureEnricher
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.stripHtml
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RaceDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao,
    private val spellDataSource: SpellDataSource,
    private val featureEnricher: FeatureEnricher
) : RaceDataSource {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_DEBUG_RACE_DS"
    private val VALID_STATS = setOf("STR", "DEX", "CON", "INT", "WIS", "CHA")

    override suspend fun getAllParentRaces(): List<Race> {
        return dao.getAllRaces().map { mapRaceEntity(it) }
    }

    override suspend fun getSubracesFromDb(parentId: Int): List<Race> {
        val parent = dao.getAllRaces().find { it.id == parentId } ?: return emptyList()
        return dao.getSubracesForRace(parent.indexName).map { entity ->
            Race(
                id = entity.id ?: 0,
                index = entity.indexName,
                name = entity.name,
                description = entity.description,
                speed = parent.speed,
                baseStats = parseAbilityBonuses(entity.abilityBonusesJson),
                baseProficiencies = parseProficiencies(entity.startingProficienciesJson)
            )
        }
    }

    override suspend fun getBaseRaceFeatures(raceId: Int): List<Feature> = coroutineScope {
        val race = dao.getAllRaces().find { it.id == raceId } ?: return@coroutineScope emptyList()
        val features = mutableListOf<Feature>()

        features.add(Feature(-100, "desc", "Описание", race.description?.stripHtml() ?: "", priority = 0))

        val staticBonuses = parseAbilityBonuses(race.abilityBonusesJson)
        if (staticBonuses.isNotEmpty()) {
            features.add(Feature(-90, "stat_bonus", "Увеличение характеристик", DndLocalization.getStatIncreaseSummary(staticBonuses), priority = 10))
        }

        if (!race.traitsJson.isNullOrBlank()) {
            try {
                val traitIndexes = json.decodeFromString<List<String>>(race.traitsJson)
                val traitEntities = dao.getFeaturesByIndexes(traitIndexes)
                val processedFeatures = traitEntities.map { entity ->
                    val feature = mapRaceFeature(entity, race.indexName, null)
                    featureEnricher.enrich(feature, entity, SelectionSource.RACE)
                }
                features.addAll(processedFeatures)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка парсинга способностей для расы ${race.indexName}", e)
            }
        }

        if (dao.getSubracesForRace(race.indexName).isNotEmpty()) {
            features.add(Feature(-1, "subrace_selector", DndLocalization.getSpeciesHeader(race.indexName), "Выберите вашу разновидность или происхождение.", isSubraceSelector = true, priority = 999))
        }

        return@coroutineScope features.sortedBy { it.priority }
    }

    override suspend fun getSubraceFeatures(subraceIndex: String): List<Feature> = coroutineScope {
        val sub = dao.getSubraceByIndex(subraceIndex) ?: return@coroutineScope emptyList()
        val features = mutableListOf<Feature>()

        val subBonuses = parseAbilityBonuses(sub.abilityBonusesJson)
        if (subBonuses.isNotEmpty()) {
            features.add(Feature(-70, "sub_stat_bonus", "Увеличение характеристик", DndLocalization.getStatIncreaseSummary(subBonuses), priority = 10))
        }

        if (!sub.traitsJson.isNullOrBlank()) {
            try {
                val traitIndexes = json.decodeFromString<List<String>>(sub.traitsJson)
                val traitEntities = dao.getFeaturesByIndexes(traitIndexes)
                features.addAll(traitEntities.map { mapRaceFeature(it, sub.raceIndex, sub.indexName) })
            } catch (e: Exception) {  }
        }

        return@coroutineScope features.sortedBy { it.priority }
    }

    override suspend fun getRaceByIndex(index: String): Race? {
        val entity = dao.getAllRaces().find { it.indexName == index } ?: return null
        return mapRaceEntity(entity)
    }

    override suspend fun getRaceFullData(index: String): RaceFullData? {
        val entity = dao.getAllRaces().find { it.indexName == index } ?: return null
        val raceModel = mapRaceEntity(entity)
        val features = getBaseRaceFeatures(raceModel.id)
        return RaceFullData(race = raceModel, features = features)
    }

    override suspend fun getSubraceModelByIndex(index: String): Race? {
        val subraceEntity = dao.getSubraceByIndex(index) ?: return null
        val parentEntity = dao.getAllRaces().find { it.indexName == subraceEntity.raceIndex }
        return Race(
            id = subraceEntity.id ?: 0,
            index = subraceEntity.indexName,
            name = subraceEntity.name,
            description = subraceEntity.description,
            speed = parentEntity?.speed ?: 30,
            baseStats = parseAbilityBonuses(subraceEntity.abilityBonusesJson),
            baseProficiencies = parseProficiencies(subraceEntity.startingProficienciesJson)
        )
    }

    private suspend fun mapRaceFeature(entity: FeatureEntity, raceIndex: String, subraceIndex: String?): Feature {
        val choices = mutableListOf<FeatureChoiceDomain>()
        entity.choicesJson?.let { raw ->
            try {
                val el = json.parseToJsonElement(raw)
                val elements = if (el is JsonArray) el else listOf(el)
                elements.forEach {
                    if (it is JsonObject) choices.add(parseRaceChoice(it))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing choices_json for race feature ${entity.indexName}", e)
            }
        }

        val grantedProficiencies = mutableListOf<String>()
        entity.referenceJson?.let { raw ->
            try {
                val refObj = json.parseToJsonElement(raw).jsonObject
                refObj["granted_proficiencies"]?.jsonArray?.forEach { prof ->
                    prof.jsonObject["index"]?.jsonPrimitive?.content?.let { grantedProficiencies.add(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing reference_json for granted proficiencies in ${entity.indexName}", e)
            }
        }

        val spells = spellDataSource.getGrantedSpells(entity.spellShowJson)

        return Feature(
            id = entity.id ?: 0, index = entity.indexName, name = entity.name,
            description = entity.description?.stripHtml() ?: "", choices = choices,
            embeddedSpells = spells, changeRule = entity.changeRule == 1, priority = 100,
            grantedProficiencies = grantedProficiencies,
            maxCharges = entity.maxCharges,
            resetRule = parseResetRule(entity.chargeResetRule),
            referenceJson = entity.referenceJson,
            uiGroup = entity.uiGroup,
            raceIndex = raceIndex,
            subraceIndex = subraceIndex
        )
    }

    private suspend fun parseRaceChoice(obj: JsonObject): FeatureChoiceDomain {
        val count = obj["choose"]?.jsonPrimitive?.int ?: 1
        val type = obj["type"]?.jsonPrimitive?.content ?: ""
        val contextKind = getProficiencyKindFromType(type)

        if (type.contains("spell")) {
            return spellDataSource.parseSpellChoice(obj)
        }

        val fromElement = obj["from"] ?: return FeatureChoiceDomain.SelectOption(0, emptyList())

        val options = mutableListOf<ChoiceOption>()

        when(fromElement) {
            is JsonObject -> {
                when (fromElement["option_set_type"]?.jsonPrimitive?.content) {
                    "resource_list" -> {
                        val resource = fromElement["resource"]?.jsonPrimitive?.content
                        val resourceOptions = when (resource) {
                            "skills" -> dao.getAllSkills().map { ChoiceOption(normalizeId(it.indexName, ProficiencyKind.SKILL), it.name, kind = ProficiencyKind.SKILL) }
                            "languages" -> dao.getAllLanguages().map { ChoiceOption(normalizeId(it.indexName, ProficiencyKind.LANGUAGE), it.name, kind = ProficiencyKind.LANGUAGE) }
                            "general_feats" -> dao.getAllFeats().map { ChoiceOption(it.indexName, it.name, kind = ProficiencyKind.FEAT) }
                            else -> emptyList()
                        }
                        options.addAll(resourceOptions)
                    }
                    else -> {
                        fromElement["options"]?.jsonArray?.forEach { el ->
                            try {
                                val o = el.jsonObject
                                val item = o["item"]?.jsonObject
                                val rawId = item?.get("index")?.jsonPrimitive?.content ?: o["value"]?.jsonPrimitive?.content ?: ""
                                val id = normalizeId(rawId, contextKind)
                                val label = item?.get("name")?.jsonPrimitive?.content ?: o["label"]?.jsonPrimitive?.content ?: DndLocalization.translateStat(id)
                                val info = o["desc"]?.jsonPrimitive?.content
                                val subChoice = o["choice"]?.jsonObject?.let { parseRaceChoice(it) }
                                options.add(ChoiceOption(id, DndLocalization.cleanLabel(label), info, subChoice, kind = contextKind))
                            } catch (e: Exception) {  }
                        }
                    }
                }
            }
            is JsonArray -> fromElement.forEach { el ->
                try {
                    val o = el.jsonObject
                    val item = o["item"]?.jsonObject
                    val rawId = item?.get("index")?.jsonPrimitive?.content ?: o["value"]?.jsonPrimitive?.content ?: ""
                    val id = normalizeId(rawId, contextKind)
                    val label = item?.get("name")?.jsonPrimitive?.content ?: o["label"]?.jsonPrimitive?.content ?: DndLocalization.translateStat(id)
                    options.add(ChoiceOption(id, DndLocalization.cleanLabel(label), kind = contextKind))
                } catch(e: Exception) {  }
            }
            else -> {}
        }

        return when {
            type.contains("ability_bonuses") -> FeatureChoiceDomain.SelectStatBonus(
                count = count,
                amount = 1,
                options = options,
                allowDuplicateSelections = false
            )
            contextKind != ProficiencyKind.NONE -> FeatureChoiceDomain.SelectOption(count, options, proficiencyKind = contextKind)
            type.contains("skill") -> FeatureChoiceDomain.SelectSkill(count, options)
            type.contains("proficiencies") -> FeatureChoiceDomain.SelectOption(count, options)
            type.contains("language") -> FeatureChoiceDomain.SelectOption(count, options, proficiencyKind = ProficiencyKind.LANGUAGE)
            else -> FeatureChoiceDomain.SelectOption(count, options)
        }
    }

    private fun parseAbilityBonuses(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString<List<JsonObject>>(raw).associate {
                (it["stat"]?.jsonPrimitive?.content ?: "") to (it["bonus"]?.jsonPrimitive?.int ?: 0)
            }
        } catch (e: Exception) { emptyMap() }
    }

    private fun parseProficiencies(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<ReferenceJson>>(raw).map { it.index }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseLanguages(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<ReferenceJson>>(raw).map { "lang-${it.index}" }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse languages_json: $raw", e)
            emptyList()
        }
    }

    private fun mapRaceEntity(entity: RaceEntity): Race {
        val proficiencies = parseProficiencies(entity.startingProficienciesJson)
        val languages = parseLanguages(entity.languagesJson)

        return Race(
            id = entity.id ?: 0, index = entity.indexName, name = entity.name,
            description = entity.description, speed = entity.speed,
            baseStats = parseAbilityBonuses(entity.abilityBonusesJson),
            baseProficiencies = proficiencies + languages
        )
    }

    private fun getProficiencyKindFromType(type: String): ProficiencyKind {
        return when {
            type.contains("proficiencies", ignoreCase = true) -> ProficiencyKind.SKILL
            type.contains("skill", ignoreCase = true) -> ProficiencyKind.SKILL
            type.contains("language", ignoreCase = true) -> ProficiencyKind.LANGUAGE
            else -> ProficiencyKind.NONE
        }
    }

    private fun normalizeId(raw: String, kind: ProficiencyKind): String {
        if (raw.isBlank()) return ""
        val upper = raw.uppercase()
        if (VALID_STATS.contains(upper)) return upper

        val clean = raw.lowercase().trim()
        val prefix = when(kind) {
            ProficiencyKind.SKILL -> "skill-"
            ProficiencyKind.TOOL -> "tool-"
            ProficiencyKind.LANGUAGE -> "lang-"
            else -> ""
        }

        return if (prefix.isNotEmpty() && !clean.startsWith(prefix)) {
            "$prefix$clean"
        } else {
            clean
        }
    }

    private fun parseResetRule(rule: String?): ResetRule = when (rule?.uppercase()) {
        "SHORT_REST" -> ResetRule.SHORT_REST
        "DAWN" -> ResetRule.DAWN
        "NEVER" -> ResetRule.NEVER
        else -> ResetRule.LONG_REST
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\RaceDataSourceImpl.kt
