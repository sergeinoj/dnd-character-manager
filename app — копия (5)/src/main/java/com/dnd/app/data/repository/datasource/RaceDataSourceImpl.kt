// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/RaceDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.data.local.entity.RaceEntity
import com.dnd.app.domain.model.*
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.capitalizeFirst
import com.dnd.app.util.stripHtml
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [RaceDataSourceImpl]
 *
 * Этот источник данных отвечает ИСКЛЮЧИТЕЛЬНО за извлечение и преобразование сущностей, связанных с расами и подрасами.
 * Он является единственным источником истины для преобразования RaceEntity, SubraceEntity и связанных с ними FeatureEntity в доменные модели.
 *
 * ВАЖНО: Не перегружайте этот класс логикой, относящейся к другим доменам (например, парсинг классовых способностей или общих черт).
 * Архитектура продумана для строгой изоляции ответственности.
 */

@Singleton
class RaceDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao,
    private val spellDataSource: SpellDataSource
) : RaceDataSource {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_DEBUG_RACE_DS"

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
                speed = parent.speed ?: 30,
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
                    if (entity.indexName == "dragon-ancestor") {
                        enrichDragonAncestorFeature(entity)
                    } else {
                        mapRaceFeature(entity)
                    }
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
                features.addAll(traitEntities.map { mapRaceFeature(it) })
            } catch (e: Exception) { /* Malformed JSON, ignore */ }
        }

        return@coroutineScope features.sortedBy { it.priority }
    }

    override suspend fun getRaceByIndex(index: String): Race? {
        val entity = dao.getAllRaces().find { it.indexName == index } ?: return null
        return mapRaceEntity(entity)
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

    private suspend fun mapRaceFeature(entity: FeatureEntity): Feature {
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
            referenceJson = entity.referenceJson,
            uiGroup = entity.uiGroup
        )
    }

    private suspend fun parseRaceChoice(obj: JsonObject): FeatureChoiceDomain {
        val count = obj["choose"]?.jsonPrimitive?.int ?: 1
        val type = obj["type"]?.jsonPrimitive?.content ?: ""

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
                            "skills" -> dao.getAllSkills().map { ChoiceOption("skill-${it.indexName}", it.name) }
                            "languages" -> dao.getAllLanguages().map { ChoiceOption(it.indexName, it.name) }
                            "general_feats" -> dao.getAllFeats().map { ChoiceOption(it.indexName, it.name) }
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
                                // [ИСПРАВЛЕНО] Умное преобразование регистра
                                val id = when {
                                    rawId.startsWith("skill-", ignoreCase = true) -> rawId
                                    rawId.length == 3 -> rawId.uppercase() // Только для статов (STR, DEX)
                                    else -> rawId // Сохраняем регистр для сложных индексов
                                }
                                val label = item?.get("name")?.jsonPrimitive?.content ?: o["label"]?.jsonPrimitive?.content ?: DndLocalization.translateStat(id)
                                val info = o["desc"]?.jsonPrimitive?.content
                                val subChoice = o["choice"]?.jsonObject?.let { parseRaceChoice(it) }
                                options.add(ChoiceOption(id, DndLocalization.cleanLabel(label), info, subChoice))
                            } catch (e: Exception) { /* ignore */ }
                        }
                    }
                }
            }
            is JsonArray -> fromElement.forEach { el ->
                try {
                    val o = el.jsonObject
                    val item = o["item"]?.jsonObject
                    val rawId = item?.get("index")?.jsonPrimitive?.content ?: o["value"]?.jsonPrimitive?.content ?: ""
                    // [ИСПРАВЛЕНО] Умное преобразование регистра
                    val id = when {
                        rawId.startsWith("skill-", ignoreCase = true) -> rawId
                        rawId.length == 3 -> rawId.uppercase() // Только для статов (STR, DEX)
                        else -> rawId // Сохраняем регистр для сложных индексов
                    }
                    val label = item?.get("name")?.jsonPrimitive?.content ?: o["label"]?.jsonPrimitive?.content ?: DndLocalization.translateStat(id)
                    options.add(ChoiceOption(id, DndLocalization.cleanLabel(label)))
                } catch(e: Exception) { /* ignore */ }
            }
            else -> {}
        }

        return when {
            type.contains("ability_bonuses") -> FeatureChoiceDomain.SelectStatBonus(count, 1, options)
            type.contains("skill") -> FeatureChoiceDomain.SelectSkill(count, options)
            type.contains("proficiencies") -> FeatureChoiceDomain.SelectOption(count, options)
            type.contains("language") -> FeatureChoiceDomain.SelectOption(count, options)
            else -> FeatureChoiceDomain.SelectOption(count, options)
        }
    }

    private suspend fun enrichDragonAncestorFeature(entity: FeatureEntity): Feature {
        try {
            val baseFeature = mapRaceFeature(entity)
            val choice = baseFeature.choices.firstOrNull() as? FeatureChoiceDomain.SelectOption ?: return baseFeature

            val choiceJson = entity.choicesJson?.let { rawJson -> json.parseToJsonElement(rawJson) as? JsonObject } ?: return baseFeature
            val optionsJson = choiceJson["from"]?.jsonArray ?: return baseFeature

            val childFeatureIndexes = optionsJson.mapNotNull { it.jsonObject["value"]?.jsonPrimitive?.content }
            if (childFeatureIndexes.isEmpty()) return baseFeature

            val childFeatures = dao.getFeaturesByIndexes(childFeatureIndexes).associateBy { it.indexName }
            if (childFeatures.isEmpty()) return baseFeature

            val enrichedOptions = choice.options.mapNotNull { opt ->
                val childFeature = childFeatures[opt.id]
                val referenceJson = childFeature?.referenceJson?.let { json.parseToJsonElement(it).jsonObject }

                if (referenceJson == null) {
                    Log.w(TAG, "No reference_json found for child feature: ${opt.id}")
                    return@mapNotNull opt
                }

                val damageTypeIndex = referenceJson["damage_type"]?.jsonPrimitive?.content ?: ""
                val breathType = referenceJson["breath"]?.jsonPrimitive?.content ?: ""
                val breathSize = referenceJson["line_size"]?.jsonPrimitive?.content
                    ?: referenceJson["cone_size"]?.jsonPrimitive?.intOrNull?.toString()
                    ?: ""

                val damageTypeName = dao.getDamageTypeByIndex(damageTypeIndex)?.name ?: damageTypeIndex.capitalizeFirst()
                val localizedBreathType = DndLocalization.translateBreathType(breathType)
                val saveStatAbbr = DndLocalization.getBreathSaveStatAbbr(damageTypeIndex)

                val infoString = "$damageTypeName | $breathSize футов $localizedBreathType ($saveStatAbbr)"
                opt.copy(info = infoString)
            }

            return baseFeature.copy(choices = listOf(choice.copy(options = enrichedOptions)))
        } catch (e: Exception) {
            Log.e(TAG, "Error during special processing for 'dragon-ancestor'", e)
            return mapRaceFeature(entity) // Fallback
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
            json.decodeFromString<List<com.dnd.app.data.model.ReferenceJson>>(raw).map { it.index }
        } catch (e: Exception) { emptyList() }
    }

    private fun mapRaceEntity(entity: RaceEntity): Race {
        return Race(
            id = entity.id ?: 0, index = entity.indexName, name = entity.name,
            description = entity.description, speed = entity.speed ?: 30,
            baseStats = parseAbilityBonuses(entity.abilityBonusesJson),
            baseProficiencies = parseProficiencies(entity.startingProficienciesJson)
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/RaceDataSourceImpl.kt