// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\FeatDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.ArmorEntity
import com.dnd.app.data.local.entity.EquipmentEntity
import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.data.local.entity.SpellEntity
import com.dnd.app.data.local.entity.WeaponEntity
import com.dnd.app.domain.model.*
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
    private val VALID_STATS = setOf("STR", "DEX", "CON", "INT", "WIS", "CHA")

    private val CATEGORY_REMAP = mapOf(
        "weapon" to "shop-weapons",
        "weapons" to "shop-weapons",
        "armor" to "shop-armor"
    )

    override suspend fun getFeatureByIndex(index: String): Feature? {
        val entity = dao.getFeatureByIndex(index)
        if (entity?.uiGroup != "FEAT") return null
        return mapFeatEntity(entity)
    }

    override suspend fun getFeatureById(id: Int): Feature? {
        val entity = dao.getFeatureById(id)
        if (entity?.uiGroup != "FEAT") return null
        return mapFeatEntity(entity)
    }

    override suspend fun getFeaturesByIndexes(indexes: List<String>): List<Feature> {
        return dao.getFeaturesByIndexes(indexes)
            .filter { it.uiGroup == "FEAT" }
            .map { mapFeatEntity(it) }
    }

    override suspend fun getAllFeats(): List<Feature> {
        return dao.getAllFeats().map { mapFeatEntity(it) }
    }

    private suspend fun mapFeatEntity(entity: FeatureEntity): Feature {
        val choices = mutableListOf<FeatureChoiceDomain>()
        val grantedProficiencies = mutableListOf<String>()

        entity.choicesJson?.let { raw ->
            if (raw.isNotBlank()) {
                try {
                    val el = json.parseToJsonElement(raw)
                    val parsedChoices = when (el) {
                        is JsonArray -> el.map { parseFeatChoice(it.jsonObject) }
                        is JsonObject -> listOf(parseFeatChoice(el))
                        else -> emptyList()
                    }
                    choices.addAll(parsedChoices)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing choices_json for ${entity.indexName}", e)
                }
            }
        }

        if (!entity.referenceJson.isNullOrBlank()) {
            try {
                val refObj = json.parseToJsonElement(entity.referenceJson).jsonObject
                when {
                    refObj.containsKey("languages_count") -> {
                        val count = refObj["languages_count"]?.jsonPrimitive?.int ?: 1
                        val options = dao.getAllLanguages().map {
                            ChoiceOption(normalizeId(it.indexName, ProficiencyKind.LANGUAGE), it.name, kind = ProficiencyKind.LANGUAGE)
                        }
                        choices.add(FeatureChoiceDomain.SelectOption(count, options, proficiencyKind = ProficiencyKind.LANGUAGE))
                    }
                    refObj.containsKey("maneuvers_count") -> {
                        val count = refObj["maneuvers_count"]?.jsonPrimitive?.int ?: 1
                        val maneuvers = dao.getFeaturesLike("maneuver-%")
                        val options = maneuvers.map { ChoiceOption(it.indexName, it.name) }
                        choices.add(FeatureChoiceDomain.SelectOption(count, options, description = "Выберите маневры"))
                    }
                }
                (refObj["granted_proficiencies"] as? JsonArray)?.forEach { prof ->
                    prof.jsonObject["index"]?.jsonPrimitive?.content?.let { grantedProficiencies.add(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing referenceJson for ${entity.indexName}", e)
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
        val contextKind = getProficiencyKindFromType(type)

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
        var inferredKind: ProficiencyKind? = null

        val options: List<ChoiceOption> = when (fromElement) {
            is JsonObject -> {
                val optionSetType = fromElement["option_set_type"]?.jsonPrimitive?.content
                when (optionSetType) {
                    "resource_list" -> {
                        when (fromElement["resource"]?.jsonPrimitive?.content) {
                            "skills" -> {
                                inferredKind = ProficiencyKind.SKILL
                                dao.getAllSkills().map { ChoiceOption(normalizeId(it.indexName, ProficiencyKind.SKILL), it.name, info = it.description?.stripHtml(), kind = ProficiencyKind.SKILL) }
                            }
                            "general_feats" -> {
                                inferredKind = ProficiencyKind.FEAT
                                dao.getAllFeats().map { ChoiceOption(it.indexName, it.name, kind = ProficiencyKind.FEAT) }
                            }
                            "languages" -> {
                                inferredKind = ProficiencyKind.LANGUAGE
                                dao.getAllLanguages().map { ChoiceOption(normalizeId(it.indexName, ProficiencyKind.LANGUAGE), it.name, kind = ProficiencyKind.LANGUAGE) }
                            }
                            else -> emptyList()
                        }
                    }
                    "equipment_category" -> {
                        val rawCategoryIndex = fromElement["equipment_category"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                        if (rawCategoryIndex != null) {
                            val categoryIndex = CATEGORY_REMAP[rawCategoryIndex] ?: rawCategoryIndex
                            val mappingKind = when {
                                rawCategoryIndex.contains("weapon") || categoryIndex.contains("weapon") -> ProficiencyKind.WEAPON
                                rawCategoryIndex.contains("armor") || categoryIndex.contains("armor") -> ProficiencyKind.ARMOR
                                else -> ProficiencyKind.TOOL
                            }
                            inferredKind = mappingKind
                            val itemIndexes = dao.getAllItemIndexesByCategoryRecursive(categoryIndex)
                            if (itemIndexes.isNotEmpty()) {
                                resolveEntitiesToOptions(itemIndexes, mappingKind)
                            } else {
                                if (mappingKind == ProficiencyKind.WEAPON) {
                                    dao.getAllWeaponsSuspend().map { mapEntityToOption(it, mappingKind, emptyMap()) }.sortedBy { it.label }
                                } else emptyList()
                            }
                        } else emptyList()
                    }
                    else -> fromElement["options"]?.jsonArray?.mapNotNull { parseOptionElement(it, contextKind) } ?: emptyList()
                }
            }
            is JsonPrimitive -> {
                when (fromElement.content) {
                    "all_skills_and_tools" -> (dao.getAllSkills().map { ChoiceOption(normalizeId(it.indexName, ProficiencyKind.SKILL), DndLocalization.translateSkill(it.indexName), info = it.description?.stripHtml(), kind = ProficiencyKind.SKILL) } +
                            dao.getAllProficiencies().filter { it.type == "tools" }.map { ChoiceOption(normalizeId(it.indexName, ProficiencyKind.TOOL), it.name, kind = ProficiencyKind.TOOL) }).sortedBy { it.label }
                    "martial_weapons" -> {
                        inferredKind = ProficiencyKind.WEAPON
                        val allIndexes = (dao.getAllItemIndexesByCategoryRecursive("martial-melee-weapons") + dao.getAllItemIndexesByCategoryRecursive("martial-ranged-weapons")).distinct()
                        resolveEntitiesToOptions(allIndexes, inferredKind)
                    }
                    "simple_weapons", "simple-weapons" -> {
                        inferredKind = ProficiencyKind.WEAPON
                        val allIndexes = (dao.getAllItemIndexesByCategoryRecursive("simple-melee-weapons") + dao.getAllItemIndexesByCategoryRecursive("simple-ranged-weapons")).distinct()
                        resolveEntitiesToOptions(allIndexes, inferredKind)
                    }
                    "all_classes" -> dao.getAllClasses().map { ChoiceOption(it.indexName, it.name) }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }

        val finalKind = inferredKind ?: contextKind
        return when {
            type.contains("ability") || type.contains("stat") -> FeatureChoiceDomain.SelectStatBonus(count, 1, options)
            finalKind != ProficiencyKind.NONE -> FeatureChoiceDomain.SelectOption(count, options, proficiencyKind = finalKind)
            type.contains("skill") -> FeatureChoiceDomain.SelectSkill(count, options)
            else -> FeatureChoiceDomain.SelectOption(count, options)
        }
    }

    private suspend fun resolveEntitiesToOptions(indexes: List<String>, kind: ProficiencyKind): List<ChoiceOption> {
        val weapons = dao.getWeaponsByIndexes(indexes)
        val armor = dao.getArmorByIndexes(indexes)
        val equipment = dao.getEquipmentByIndexes(indexes)

        val damageTypeIndices = weapons.mapNotNull { it.damageType }.distinct()
        val damageTypeMap = damageTypeIndices.mapNotNull { dao.getDamageTypeByIndex(it) }.associate { it.indexName to it.name }

        val weaponOptions = weapons.map { mapEntityToOption(it, kind, damageTypeMap) }
        val armorOptions = armor.map { mapEntityToOption(it, kind, emptyMap()) }
        val equipOptions = equipment.map { mapEntityToOption(it, kind, emptyMap()) }

        return (weaponOptions + armorOptions + equipOptions).distinctBy { it.id }.sortedBy { it.label }
    }

    private suspend fun mapEntityToOption(entity: Any, kind: ProficiencyKind, damageTypeMap: Map<String, String>): ChoiceOption {
        return when (entity) {
            is WeaponEntity -> {
                val infoText = DndLocalization.assembleEnrichedDescription(
                    rarity = DndLocalization.translateRarity(entity.rarity),
                    stats = if (kind == ProficiencyKind.WEAPON) null else DndLocalization.formatWeaponInfo(entity.damage, damageTypeMap[entity.damageType] ?: entity.damageType),
                    description = entity.description
                )
                ChoiceOption(id = entity.indexName, label = entity.name, info = infoText, kind = kind)
            }
            is ArmorEntity -> ChoiceOption(
                id = entity.indexName,
                label = entity.name,
                info = DndLocalization.assembleEnrichedDescription(
                    rarity = DndLocalization.translateRarity(entity.rarity),
                    stats = DndLocalization.formatArmorInfo(entity.acBase),
                    description = entity.description
                ),
                kind = kind
            )
            is EquipmentEntity -> ChoiceOption(
                id = entity.indexName,
                label = entity.name,
                info = entity.description?.stripHtml(),
                kind = kind
            )
            else -> ChoiceOption("", "Unknown", kind = kind)
        }
    }

    private suspend fun parseOptionElement(el: kotlinx.serialization.json.JsonElement, contextKind: ProficiencyKind): ChoiceOption? {
        return try {
            when (el) {
                is JsonPrimitive -> {
                    val id = normalizeId(el.content, contextKind)
                    val label = if (id.length == 3) DndLocalization.translateStat(id) else id.capitalizeFirst()
                    ChoiceOption(id, label, kind = contextKind)
                }
                is JsonObject -> {
                    val item = el["item"]?.jsonObject
                    val rawId = item?.get("index")?.jsonPrimitive?.content ?: el["value"]?.jsonPrimitive?.content ?: ""
                    val id = normalizeId(rawId, contextKind)
                    val rawLabel = item?.get("name")?.jsonPrimitive?.content ?: el["label"]?.jsonPrimitive?.content
                    val label = localizeStatToken(rawLabel) ?: DndLocalization.translateStat(id)
                    val info = el["desc"]?.jsonPrimitive?.content
                    val subChoice = el["choice"]?.jsonObject?.let { parseFeatChoice(it) }
                    ChoiceOption(id, DndLocalization.cleanLabel(label), info, subChoice, kind = contextKind)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getProficiencyKindFromType(type: String): ProficiencyKind {
        return when {
            type.contains("proficiencies", ignoreCase = true) -> ProficiencyKind.SKILL
            type.contains("skill", ignoreCase = true) -> ProficiencyKind.SKILL
            type.contains("language", ignoreCase = true) -> ProficiencyKind.LANGUAGE
            else -> ProficiencyKind.NONE
        }
    }

    private fun localizeStatToken(raw: String?): String? {
        val token = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val upper = token.take(3).uppercase()
        return when (upper) {
            "STR", "DEX", "CON", "INT", "WIS", "CHA" -> DndLocalization.translateStat(upper)
            else -> token
        }
    }

    private fun normalizeId(raw: String, kind: ProficiencyKind): String {
        if (raw.isBlank()) return ""
        val upper = raw.uppercase()
        if (VALID_STATS.contains(upper)) return upper
        val clean = raw.lowercase().trim()
        val prefix = when (kind) {
            ProficiencyKind.SKILL -> "skill-"
            ProficiencyKind.TOOL -> "tool-"
            ProficiencyKind.LANGUAGE -> "lang-"
            else -> ""
        }
        return if (prefix.isNotEmpty() && !clean.startsWith(prefix)) "$prefix$clean" else clean
    }

    private fun mapSpellEntityToDomain(e: SpellEntity): Spell {
        return Spell(
            id = e.id ?: 0, index = e.indexName, name = e.name, level = e.level,
            school = DndLocalization.translateSchool(e.school), castingTime = e.castingTime ?: "",
            range = e.range ?: "", components = e.componentsJson ?: "",
            duration = e.duration ?: "", description = e.description ?: "",
            isConcentration = e.concentration == 1, isRitual = e.ritual == 1
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\FeatDataSourceImpl.kt
