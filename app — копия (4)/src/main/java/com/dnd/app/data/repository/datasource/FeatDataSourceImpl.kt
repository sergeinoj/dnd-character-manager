// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/FeatDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.data.local.entity.SpellEntity
import com.dnd.app.data.model.ReferenceJson
import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.model.Spell
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.capitalizeFirst
import com.dnd.app.util.stripHtml
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao,
    private val spellDataSource: SpellDataSource
) : FeatDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private val TAG = "DND_DEBUG_FEAT_DS"

    override suspend fun getFeatureByIndex(index: String): Feature? {
        val entity = dao.getFeatureByIndex(index)
        if (entity?.indexName?.startsWith("feat-") != true) return null
        return mapFeatEntity(entity)
    }

    override suspend fun getFeatureById(id: Int): Feature? {
        val entity = dao.getFeatureById(id)
        if (entity?.indexName?.startsWith("feat-") != true) return null
        return mapFeatEntity(entity)
    }

    override suspend fun getFeaturesByIndexes(indexes: List<String>): List<Feature> {
        return dao.getFeaturesByIndexes(indexes)
            .filter { it.indexName.startsWith("feat-") }
            .map { mapFeatEntity(it) }
    }

    override suspend fun getAllFeats(): List<Feature> {
        return dao.getAllFeats().map { mapFeatEntity(it) }
    }

    private suspend fun mapFeatEntity(entity: FeatureEntity): Feature {
        val choices = mutableListOf<FeatureChoiceDomain>()
        val grantedProficiencies = mutableListOf<String>()

        var choicesParsedFromJson = false
        entity.choicesJson?.let { raw ->
            if (raw.isNotBlank()) {
                try {
                    val el = json.parseToJsonElement(raw)
                    val parsedChoices = when (el) {
                        is JsonArray -> el.map { parseFeatChoice(it.jsonObject) }
                        is JsonObject -> listOf(parseFeatChoice(el))
                        else -> emptyList()
                    }

                    if (parsedChoices.isNotEmpty()) {
                        choices.addAll(parsedChoices)
                        choicesParsedFromJson = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing choices_json for ${entity.indexName}", e)
                }
            }
        }

        if (!choicesParsedFromJson && !entity.referenceJson.isNullOrBlank()) {
            try {
                val refObj = json.parseToJsonElement(entity.referenceJson).jsonObject
                Log.d(TAG, "Feature '${entity.indexName}': choices_json empty, checking reference_json for dynamic UI.")

                when {
                    refObj.containsKey("stat_choice") -> {
                        val statKeys = refObj["stat_choice"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                        if (statKeys.isNotEmpty()) {
                            val options = statKeys.map { ChoiceOption(it.uppercase(), DndLocalization.translateStat(it)) }
                            val bonusValue = refObj["stat_value"]?.jsonPrimitive?.int ?: 1
                            val count = if(entity.indexName == "feat-resilient") 1 else (refObj["stat_count"]?.jsonPrimitive?.int ?: 1)
                            choices.add(FeatureChoiceDomain.SelectStatBonus(count, bonusValue, options))
                        }
                    }
                    refObj.containsKey("languages_count") -> {
                        val count = refObj["languages_count"]?.jsonPrimitive?.int ?: 1
                        val options = dao.getAllLanguages().map { ChoiceOption(it.indexName, it.name) }
                        choices.add(FeatureChoiceDomain.SelectOption(count, options))
                    }
                    refObj.containsKey("maneuvers_count") -> {
                        val count = refObj["maneuvers_count"]?.jsonPrimitive?.int ?: 1
                        val maneuvers = dao.getFeaturesLike("maneuver-%")
                        val options = maneuvers.map { ChoiceOption(it.indexName, it.name) }
                        choices.add(FeatureChoiceDomain.SelectOption(count, options, "Выберите маневры"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing referenceJson for dynamic choices for ${entity.indexName}", e)
            }
        }

        entity.referenceJson?.let { raw ->
            try {
                val refObj = json.parseToJsonElement(raw).jsonObject
                (refObj["proficiency"] as? JsonArray)?.forEach { prof ->
                    prof.jsonPrimitive.content.let { grantedProficiencies.add(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing referenceJson for static proficiencies for ${entity.indexName}", e)
            }
        }

        val spells = spellDataSource.getGrantedSpells(entity.spellShowJson)

        return Feature(
            id = entity.id ?: 0, index = entity.indexName, name = entity.name,
            description = entity.description?.stripHtml() ?: "", choices = choices,
            embeddedSpells = spells, changeRule = entity.changeRule == 1, priority = 100,
            grantedProficiencies = grantedProficiencies.distinct(),
            referenceJson = entity.referenceJson,
            uiGroup = entity.uiGroup
        )
    }

    private suspend fun parseFeatChoice(obj: JsonObject): FeatureChoiceDomain {
        val count = obj["choose"]?.jsonPrimitive?.int ?: 1
        val type = obj["type"]?.jsonPrimitive?.content ?: ""

        if (type.contains("class_spell_list") || type.contains("class_ritual_list")) {
            return spellDataSource.parseFeatSpellChoice(obj)
        }
        if (type.contains("attack_cantrip")) {
            val allSpells = dao.getAllSpellsSuspend()
            val attackCantrips = allSpells.filter { it.level == 0 && !it.attackType.isNullOrBlank() }
            val options = attackCantrips.map { entity ->
                val spellDomain = mapSpellEntityToDomain(entity)
                ChoiceOption(id = spellDomain.index, label = spellDomain.name, spell = spellDomain)
            }.sortedBy { it.label }
            return FeatureChoiceDomain.SelectSpell(count, "cantrip_attack", options)
        }
        if (type.contains("spell")) {
            return spellDataSource.parseSpellChoice(obj)
        }

        val fromElement = obj["from"] ?: return FeatureChoiceDomain.SelectOption(0, emptyList())

        val options: List<ChoiceOption> = when (fromElement) {
            is JsonObject -> {
                when (fromElement["option_set_type"]?.jsonPrimitive?.content) {
                    "resource_list" -> {
                        when (val resource = fromElement["resource"]?.jsonPrimitive?.content) {
                            "skills" -> dao.getAllSkills().map { ChoiceOption("skill-${it.indexName}", it.name) }
                            "general_feats" -> dao.getAllFeats().map { ChoiceOption(it.indexName, it.name) }
                            "languages" -> dao.getAllLanguages().map { ChoiceOption(it.indexName, it.name) }
                            else -> {
                                Log.w(TAG, "Unknown resource list type: $resource"); emptyList()
                            }
                        }
                    }
                    else -> fromElement["options"]?.jsonArray?.mapNotNull { parseOptionElement(it) } ?: emptyList()
                }
            }
            is JsonPrimitive -> {
                when (val resource = fromElement.content) {
                    "all_skills_and_tools" -> (dao.getAllSkills().map { ChoiceOption("skill-${it.indexName}", DndLocalization.translateSkill(it.indexName)) } +
                            dao.getAllProficiencies().filter { it.type == "tools" }.map { ChoiceOption(it.indexName, it.name) }).sortedBy { it.label }
                    "martial_weapons" -> {
                        val allWeapons = dao.getAllWeaponsSuspend()
                        val martialWeapons = allWeapons.filter { weapon ->
                            weapon.propertiesJson?.let { props ->
                                runCatching {
                                    json.decodeFromString<List<ReferenceJson>>(props)
                                        .any { prop -> prop.index == "martial" }
                                }.getOrDefault(false)
                            } ?: false
                        }
                        martialWeapons.map { ChoiceOption(it.indexName, it.name) }
                    }
                    "all_classes" -> dao.getAllClasses().map { ChoiceOption(it.indexName, it.name) }
                    else -> {
                        Log.w(TAG, "Unknown resource string: $resource"); emptyList()
                    }
                }
            }
            is JsonArray -> fromElement.mapNotNull { parseOptionElement(it) }
            else -> emptyList()
        }

        return when {
            type.contains("ability") || type.contains("stat") -> FeatureChoiceDomain.SelectStatBonus(count, 1, options)
            type.contains("skill") -> FeatureChoiceDomain.SelectSkill(count, options)
            type.contains("damage_type") -> FeatureChoiceDomain.SelectOption(count, options, "Выберите тип урона")
            type.contains("class") -> FeatureChoiceDomain.SelectOption(count, options, "Выберите класс")
            else -> FeatureChoiceDomain.SelectOption(count, options)
        }
    }

    private suspend fun parseOptionElement(el: kotlinx.serialization.json.JsonElement): ChoiceOption? {
        return try {
            when (el) {
                is JsonPrimitive -> {
                    val id = el.content
                    val label = when {
                        id.length == 3 -> DndLocalization.translateStat(id)
                        else -> id.capitalizeFirst()
                    }
                    ChoiceOption(id, label)
                }
                is JsonObject -> {
                    val item = el["item"]?.jsonObject
                    val rawId = item?.get("index")?.jsonPrimitive?.content ?: el["value"]?.jsonPrimitive?.content ?: ""
                    val id = if (rawId.startsWith("skill-", ignoreCase = true)) rawId else rawId.uppercase()
                    val label = item?.get("name")?.jsonPrimitive?.content ?: el["label"]?.jsonPrimitive?.content ?: DndLocalization.translateStat(id)
                    val info = el["desc"]?.jsonPrimitive?.content
                    val subChoice = el["choice"]?.jsonObject?.let { parseFeatChoice(it) }
                    ChoiceOption(id, DndLocalization.cleanLabel(label), info, subChoice)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse option element: $el", e)
            null
        }
    }

    private fun mapSpellEntityToDomain(e: SpellEntity): Spell {
        return Spell(
            id = e.id ?: 0,
            index = e.indexName,
            name = e.name,
            level = e.level ?: 0,
            school = e.school ?: "Неизвестно",
            castingTime = e.castingTime ?: "",
            range = e.range ?: "",
            components = e.componentsJson ?: "",
            duration = e.duration ?: "",
            description = e.description ?: "",
            isConcentration = e.concentration == 1,
            isRitual = e.ritual == 1
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/FeatDataSourceImpl.kt