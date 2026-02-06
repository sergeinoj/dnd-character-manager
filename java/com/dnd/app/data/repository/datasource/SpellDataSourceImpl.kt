// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/SpellDataSourceImpl.kt
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SpellDataSourceImpl]
 *
 * Этот источник данных отвечает ИСКЛЮЧИТЕЛЬНО за извлечение и преобразование сущностей и JSON-структур, связанных с заклинаниями.
 * Он является единственным источником истины для преобразования SpellEntity в доменную модель Spell, а также для парсинга выборов заклинаний.
 *
 * ВАЖНО: Не перегружайте этот класс логикой, не связанной напрямую с получением или парсингом заклинаний.
 * Архитектура продумана для строгой изоляции ответственности.
 */

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
        val fromObj = choiceJson["from"]?.jsonObject ?: return FeatureChoiceDomain.SelectSpell(0, "", emptyList())
        val poolType = fromObj["option_set_type"]?.jsonPrimitive?.content ?: "fixed"

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
            "options_array", "equipment_category" -> { // Handle fixed lists of spells
                val indexes = fromObj["options"]?.jsonArray?.mapNotNull {
                    it.jsonObject["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                } ?: emptyList()
                if (indexes.isNotEmpty()) dao.getSpellsByIndexes(indexes) else emptyList()
            }
            "resource_list" -> { // For High Elf Cantrip
                val resource = fromObj["resource"]?.jsonPrimitive?.content
                if(resource?.contains("spells_level_0") == true) {
                    // This is a special case for high-elf, it needs wizard cantrips
                    val wizardCantrips = dao.getSpellsByLevel(0)
                    wizardCantrips.filter { it.classesJson?.contains("\"wizard\"") == true }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }

        val options = spellEntities.map { entity ->
            val spell = mapSpellEntity(entity)
            ChoiceOption(id = spell.index, label = spell.name, spell = spell)
        }

        return FeatureChoiceDomain.SelectSpell(count, poolType, options)
    }

    override suspend fun parseFeatSpellChoice(choiceJson: JsonObject): FeatureChoiceDomain {
        val type = choiceJson["type"]?.jsonPrimitive?.content ?: ""
        val fromClasses = choiceJson["from"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
        val allSpells = dao.getAllSpellsSuspend()

        val classOptions = fromClasses.map { classIndex ->
            val classLabel = DndLocalization.translateSkill(classIndex) // Capitalizes if not found

            val subChoice = when (type) {
                "class_spell_list" -> { // feat-magic-initiate
                    val cantrips = allSpells.filter { it.level == 0 && it.classesJson?.contains("\"$classIndex\"") == true }
                    val level1Spells = allSpells.filter { it.level == 1 && it.classesJson?.contains("\"$classIndex\"") == true }

                    val cantripChoice = FeatureChoiceDomain.SelectSpell(count = 2, poolType = "cantrips", options = cantrips.map { spellToOption(it) }.sortedBy { it.label })
                    val level1Choice = FeatureChoiceDomain.SelectSpell(count = 1, poolType = "level1", options = level1Spells.map { spellToOption(it) }.sortedBy { it.label })

                    FeatureChoiceDomain.SelectOption(
                        count = 1, // container itself isn't a choice
                        options = listOf(
                            ChoiceOption(id = "cantrips", label = "Заговоры (Выберите 2)", subChoice = cantripChoice),
                            ChoiceOption(id = "level1", label = "Заклинание 1 ур. (Выберите 1)", subChoice = level1Choice)
                        ),
                        description = "@CONTAINER@"
                    )
                }
                "class_ritual_list" -> { // feat-ritual-caster
                    val rituals = allSpells.filter { it.level == 1 && it.ritual == 1 && it.classesJson?.contains("\"$classIndex\"") == true }
                    FeatureChoiceDomain.SelectSpell(count = 2, poolType = "rituals", options = rituals.map { spellToOption(it) }.sortedBy { it.label })
                }
                else -> FeatureChoiceDomain.SelectOption(0, emptyList())
            }
            ChoiceOption(id = classIndex, label = classLabel, subChoice = subChoice)
        }

        return FeatureChoiceDomain.SelectOption(1, classOptions, "Выберите класс")
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

    private fun spellToOption(spellEntity: SpellEntity): ChoiceOption {
        val spell = mapSpellEntity(spellEntity)
        return ChoiceOption(id = spell.index, label = spell.name, spell = spell)
    }

    private fun mapSpellEntity(e: SpellEntity): Spell {
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
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/SpellDataSourceImpl.kt