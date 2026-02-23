// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\SpellDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.SpellEntity
import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.model.Spell
import com.dnd.app.util.DndLocalization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SpellDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao
) : SpellDataSource {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun getGrantedSpells(spellShowJson: String?): List<Spell> {
        if (spellShowJson.isNullOrBlank()) return emptyList()
        return try {
            val indexes = json.decodeFromString<List<String>>(spellShowJson)
            dao.getSpellsByIndexes(indexes).map { mapSpellEntity(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun parseSpellChoice(choiceJson: JsonObject): FeatureChoiceDomain.SelectSpell {
        val count = choiceJson["choose"]?.jsonPrimitive?.int ?: 1
        val fromElement = choiceJson["from"]
        val fromObj = when (fromElement) {
            is JsonObject -> fromElement
            is JsonArray -> fromElement.mapNotNull { it as? JsonObject }.firstOrNull()
            else -> null
        } ?: return FeatureChoiceDomain.SelectSpell(0, "unknown", emptyList())
        val poolType = fromObj["option_set_type"]?.jsonPrimitive?.content ?: "fixed"
        val spellListType = choiceJson["spell_list_type"]?.jsonPrimitive?.contentOrNull?.lowercase()
        val targetClasses = parseSpellListClasses(spellListType)

        val spellEntities: List<SpellEntity> = when (poolType) {
            "spells_by_class_level" -> {
                val className = fromObj["class"]?.jsonPrimitive?.content
                val targetLevel = fromObj["level"]?.jsonPrimitive?.int ?: 1

                if (className != null) {
                    val spellPool = dao.getSpellsByLevel(targetLevel)
                    spellPool.filter { it.classesJson?.contains("\"$className\"") == true }
                } else {
                    emptyList()
                }
            }
            "options_array", "equipment_category" -> {
                val indexes = fromObj["options"]?.jsonArray?.mapNotNull {
                    it.jsonObject["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                } ?: emptyList()
                if (indexes.isNotEmpty()) dao.getSpellsByIndexes(indexes) else emptyList()
            }
            "resource_list" -> {
                val resource = fromObj["resource"]?.jsonPrimitive?.content ?: ""
                val normalizedResource = resource.lowercase()
                val isCantripPool = normalizedResource.contains("spells_level_0")
                    || normalizedResource.contains("level_0")
                    || normalizedResource.endsWith("_0")
                if (!isCantripPool) {
                    emptyList()
                } else {
                val allCantrips = dao.getSpellsByLevel(0)
                allCantrips.filter { spell ->
                    val necromancyMatch = if (normalizedResource.contains("necromancy")) {
                        spell.school?.lowercase() == "necromancy"
                    } else {
                        true
                    }
                    val druidMatch = if (normalizedResource.contains("druid")) {
                        spell.classesJson?.lowercase()?.contains("\"druid\"") == true
                    } else {
                        true
                    }
                    val wizardMatch = if (normalizedResource.contains("wizard")) {
                        spell.classesJson?.lowercase()?.contains("\"wizard\"") == true
                    } else {
                        true
                    }
                    necromancyMatch && druidMatch && wizardMatch && spell.matchesClassFilters(targetClasses)
                }
            }
            }
            else -> emptyList()
        }

        val options = spellEntities.map { entity ->
            val spell = mapSpellEntity(entity)
            ChoiceOption(id = spell.index, label = spell.name, spell = spell)
        }

        val finalPoolType = (fromObj["resource"]?.jsonPrimitive?.content) ?: poolType
        return FeatureChoiceDomain.SelectSpell(count, finalPoolType, options)
    }

    override suspend fun parseFeatSpellChoice(choiceJson: JsonObject): FeatureChoiceDomain {
        val type = choiceJson["type"]?.jsonPrimitive?.content ?: ""
        val fromClasses = choiceJson["from"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
        val allSpells = dao.getAllSpellsSuspend()

        val classOptions = fromClasses.map { classIndex ->
            val classEntity = dao.getClassByIndex(classIndex)
            val classLabel = classEntity?.name ?: DndLocalization.translateSkill(classIndex)

            val subChoice = when (type) {
                "class_spell_list" -> {
                    val cantrips = allSpells.filter { it.level == 0 && it.classesJson?.contains("\"$classIndex\"") == true }
                    val level1Spells = allSpells.filter { it.level == 1 && it.classesJson?.contains("\"$classIndex\"") == true }

                    val cantripChoice = FeatureChoiceDomain.SelectSpell(count = 2, poolType = "cantrips_$classIndex", options = cantrips.map { spellToOption(it) }.sortedBy { it.label })
                    val level1Choice = FeatureChoiceDomain.SelectSpell(count = 1, poolType = "level1_$classIndex", options = level1Spells.map { spellToOption(it) }.sortedBy { it.label })

                    FeatureChoiceDomain.SelectOption(
                        count = 1,
                        options = listOf(
                            ChoiceOption(id = "cantrips", label = "Заговоры (Выберите 2)", subChoice = cantripChoice),
                            ChoiceOption(id = "level1", label = "Заклинание 1 ур. (Выберите 1)", subChoice = level1Choice)
                        ),
                        isTransparent = true
                    )
                }
                "class_ritual_list" -> {
                    val rituals = allSpells.filter { it.level == 1 && it.ritual == 1 && it.classesJson?.contains("\"$classIndex\"") == true }
                    FeatureChoiceDomain.SelectSpell(count = 2, poolType = "rituals_$classIndex", options = rituals.map { spellToOption(it) }.sortedBy { it.label })
                }
                else -> FeatureChoiceDomain.SelectOption(0, emptyList())
            }
            ChoiceOption(id = classIndex, label = classLabel, subChoice = subChoice)
        }

        return FeatureChoiceDomain.SelectOption(1, classOptions, description = "Выберите класс")
    }

    override fun getAllSpells(): Flow<List<Spell>> {
        return dao.getAllSpells().map { entities ->
            entities.map { mapSpellEntity(it) }
        }
    }

    override suspend fun getSpellsByIds(ids: List<Int>): List<Spell> {
        return dao.getSpellsByIds(ids).map { mapSpellEntity(it) }
    }

    override suspend fun getSpellsByLevelAndClass(level: Int, classIndex: String): List<Spell> {
        return dao.getSpellsByLevel(level)
            .filter { it.classesJson?.contains("\"$classIndex\"") == true }
            .map { mapSpellEntity(it) }
    }

    override suspend fun getAllSpellsByClass(classIndex: String): List<Spell> {
        return dao.getAllSpellsSuspend()
            .filter { it.classesJson?.contains("\"$classIndex\"") == true }
            .map { mapSpellEntity(it) }
    }

    override suspend fun getSpellsByLevelRange(minLevel: Int, maxLevel: Int, classIndex: String): List<Spell> {
        return dao.getAllSpellsSuspend()
            .filter { it.level in minLevel..maxLevel && it.classesJson?.contains("\"$classIndex\"") == true }
            .map { mapSpellEntity(it) }
    }

    private fun spellToOption(spellEntity: SpellEntity): ChoiceOption {
        val spell = mapSpellEntity(spellEntity)
        return ChoiceOption(id = spell.index, label = spell.name, spell = spell)
    }

    private fun mapSpellEntity(e: SpellEntity): Spell {
        val extractedDamageMap = mutableMapOf<Int, String>()
        var extractedDamageType: String? = null
        var extractedSaveStat: String? = null

        e.damageJson?.let { raw ->
            runCatching {
                val root = json.parseToJsonElement(raw).jsonObject


                root["damage_at_character_level"]?.jsonObject?.forEach { (lvl, dice) ->
                    lvl.toIntOrNull()?.let { extractedDamageMap[it] = dice.jsonPrimitive.content }
                }
                root["damage_at_slot_level"]?.jsonObject?.forEach { (lvl, dice) ->
                    lvl.toIntOrNull()?.let { extractedDamageMap[it] = dice.jsonPrimitive.content }
                }

                val dmgTypeElement = root["damage_type"]
                extractedDamageType = when (dmgTypeElement) {
                    is JsonPrimitive -> dmgTypeElement.content
                    is JsonObject -> dmgTypeElement["index"]?.jsonPrimitive?.content
                    else -> null
                }
            }
        }

        e.dcJson?.let { raw ->
            runCatching {
                val root = json.parseToJsonElement(raw).jsonObject
                val dcType = root["dc_type"]
                extractedSaveStat = when (dcType) {
                    is JsonObject -> dcType["index"]?.jsonPrimitive?.content
                    is JsonPrimitive -> dcType.contentOrNull
                    else -> null
                }?.uppercase()
            }
        }

        return Spell(
            id = e.id ?: 0,
            index = e.indexName,
            name = e.name,
            level = e.level,
            school = DndLocalization.translateSchool(e.school),
            castingTime = e.castingTime ?: "",
            range = e.range ?: "",
            components = e.componentsJson ?: "",
            duration = e.duration ?: "",
            description = e.description ?: "",
            isConcentration = e.concentration == 1,
            isRitual = e.ritual == 1,
            attackType = e.attackType,
            damageMap = extractedDamageMap,
            damageType = extractedDamageType,
            saveStat = extractedSaveStat
        )
    }

    private fun parseSpellListClasses(spellListType: String?): List<String> {
        if (spellListType.isNullOrBlank()) return emptyList()
        val supportedClasses = listOf("wizard", "druid", "cleric", "bard", "sorcerer", "warlock", "paladin", "ranger", "artificer")
        val normalized = spellListType.lowercase()
        return supportedClasses.filter { normalized.contains(it) }
    }

    private fun SpellEntity.matchesClassFilters(classFilters: List<String>): Boolean {
        if (classFilters.isEmpty()) return true
        val availableClasses = classesJson?.lowercase() ?: return false
        return classFilters.any { availableClasses.contains("\"$it\"") }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\SpellDataSourceImpl.kt
