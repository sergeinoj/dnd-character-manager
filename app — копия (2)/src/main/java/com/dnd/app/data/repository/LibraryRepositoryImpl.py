// Имя файла: app/src/main/java/com/dnd/app/data/repository/LibraryRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*
import com.dnd.app.data.model.*
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.stripHtml
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val dao: ReferenceDao
) : LibraryRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun getAllClasses(): List<ClassInfo> {
        return dao.getAllClasses().map { entity ->
            val subclasses = dao.getSubclassesForClass(entity.indexName).map { sub ->
                SubclassInfo(
                    index = sub.indexName,
                    name = sub.name,
                    flavor = sub.subclassFlavor ?: "",
                    description = sub.desc ?: ""
                )
            }
            ClassInfo(
                id = entity.id ?: 0,
                index = entity.indexName,
                name = entity.name,
                hitDie = entity.hitDie ?: 8,
                subclasses = subclasses
            )
        }
    }

    override suspend fun getSubclassesForClass(classIndex: String): List<SubclassInfo> {
        return dao.getSubclassesForClass(classIndex).map { entity ->
            SubclassInfo(
                index = entity.indexName,
                name = entity.name,
                flavor = entity.subclassFlavor ?: "",
                description = entity.desc ?: ""
            )
        }
    }

    override suspend fun getProgressionFeatures(
        classIndex: String,
        level: Int,
        subclassIndex: String?
    ): List<Feature> {
        val finalFeatures = mutableListOf<Feature>()

        if (level == 1) {
            val classEntity = dao.getAllClasses().find { it.indexName == classIndex }
            classEntity?.let { cls ->
                cls.proficienciesJson?.let { raw ->
                    try {
                        val profs = json.decodeFromString<List<ReferenceJson>>(raw)
                        val names = profs.joinToString(", ") {
                            DndLocalization.translateProficiency(it.name)
                        }
                        if (names.isNotBlank()) {
                            finalFeatures.add(
                                Feature(
                                    id = -200,
                                    index = "class_base_proficiencies",
                                    name = "Владения",
                                    description = names,
                                    priority = 10
                                )
                            )
                        }
                    } catch (e: Exception) {}
                }

                cls.proficiencyChoicesJson?.let { raw ->
                    try {
                        val choices = json.decodeFromString<List<JsonObject>>(raw)
                        choices.forEachIndexed { i, choiceJson ->
                            finalFeatures.add(
                                Feature(
                                    id = -300 - i,
                                    index = "class_proficiency_choice_$i",
                                    name = "Владение",
                                    description = choiceJson["desc"]?.jsonPrimitive?.content ?: "Выберите владение",
                                    choices = listOf(parseSmartChoice(choiceJson)),
                                    priority = 20
                                )
                            )
                        }
                    } catch (e: Exception) {}
                }
            }
        }

        val progressionRows = dao.getProgressionForLevel(classIndex, level)
        val relevantRow = progressionRows.find { it.subclassIndex == null || it.subclassIndex == subclassIndex }

        relevantRow?.let { row ->
            row.featureIndicesJson?.let { raw ->
                try {
                    val featureIndexes = json.decodeFromString<List<String>>(raw)
                    val entities = dao.getFeaturesByIndexes(featureIndexes)
                    entities.forEach { entity ->
                        finalFeatures.add(mapFeature(entity))
                    }
                } catch (e: Exception) {}
            }
        }

        return finalFeatures.sortedBy { it.priority }
    }

    override suspend fun getClassSkillOptions(classId: Int): Pair<Int, List<String>> {
        // У класса в DAO нет ID для поиска, ищем по getAllClasses
        val cls = dao.getAllClasses().find { it.id == classId } ?: return 0 to emptyList()
        cls.proficiencyChoicesJson?.let { raw ->
            try {
                val choices = json.decodeFromString<List<JsonObject>>(raw)
                val skillChoice = choices.find {
                    it["type"]?.jsonPrimitive?.content?.contains("skill") == true ||
                            it["from"]?.jsonObject?.get("options")?.jsonArray?.any { opt ->
                                opt.jsonObject["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content?.contains("skill-") == true
                            } == true
                }
                if (skillChoice != null) {
                    val count = skillChoice["choose"]?.jsonPrimitive?.int ?: 2
                    val options = skillChoice["from"]?.jsonObject?.get("options")?.jsonArray?.mapNotNull {
                        it.jsonObject["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                    } ?: emptyList()
                    return count to options
                }
            } catch (e: Exception) {}
        }
        return 0 to emptyList()
    }

    // ИСПРАВЛЕНИЕ: Используем dao.getAllRaces() вместо несуществующего getParentRaces()
    override suspend fun getAllParentRaces(): List<Race> {
        return dao.getAllRaces().map { entity ->
            Race(
                id = entity.id ?: 0,
                index = entity.indexName,
                name = entity.name,
                description = entity.description,
                age = entity.age,
                alignment = entity.alignment,
                sizeDesc = entity.sizeDescription,
                languagesDesc = entity.languageDesc,
                speed = entity.speed ?: 30,
                baseStats = emptyMap()
            )
        }
    }

    override suspend fun getSubracesFromDb(parentId: Int): List<Race> {
        val parent = dao.getAllRaces().find { it.id == parentId } ?: return emptyList()
        return dao.getSubracesForRace(parent.indexName).map { entity ->
            Race(
                id = entity.id ?: 0,
                index = entity.indexName,
                name = entity.name,
                description = entity.description,
                age = null,
                alignment = null,
                sizeDesc = null,
                languagesDesc = null,
                speed = 30,
                baseStats = emptyMap()
            )
        }
    }

    override suspend fun getRaceFeatures(raceId: Int, subraceName: String?): List<Feature> {
        val race = dao.getAllRaces().find { it.id == raceId } ?: return emptyList()
        val entities = dao.findFeaturesByContext(
            raceIdx = race.indexName,
            subraceIdx = subraceName
        )
        val features = entities.map { mapFeature(it) }.toMutableList()

        if (dao.getSubracesForRace(race.indexName).isNotEmpty()) {
            features.add(
                Feature(
                    id = -1,
                    index = "virtual_subrace_selector",
                    name = DndLocalization.getSpeciesHeader(race.indexName),
                    description = "",
                    isSubraceSelector = true,
                    priority = 5
                )
            )
        }
        return features.sortedBy { it.priority }
    }

    override suspend fun getAllBackgrounds(): List<Background> {
        return dao.getAllBackgrounds().map { entity ->
            Background(
                id = entity.id ?: 0,
                name = entity.name,
                features = emptyList(),
                personalityTraits = parseJsonStrings(entity.personalityTraitsJson),
                ideals = parseJsonStrings(entity.idealsJson),
                bonds = parseJsonStrings(entity.bondsJson),
                flaws = parseJsonStrings(entity.flawsJson)
            )
        }
    }

    override suspend fun getAllAlignments(): List<AlignmentEntity> {
        return dao.getAllAlignments()
    }

    override fun getAllSpells(): Flow<List<Spell>> {
        return dao.getAllSpells().map { entities ->
            entities.map { mapSpell(it) }
        }
    }

    override suspend fun getSpellsByIds(ids: List<Int>): List<Spell> {
        return dao.getSpellsByIds(ids).map { mapSpell(it) }
    }

    override fun getAllWeapons(): Flow<List<Weapon>> {
        return dao.getAllWeapons().map { entities ->
            entities.map { entity ->
                // ИСПРАВЛЕНИЕ: Используем propertiesJson вместо properties
                Weapon(
                    id = entity.id ?: 0,
                    name = entity.name,
                    damage = entity.damage ?: "",
                    damageType = entity.damageType ?: "",
                    cost = entity.cost ?: "",
                    weight = entity.weight?.toString() ?: "0",
                    properties = entity.propertiesJson ?: ""
                )
            }
        }
    }

    override suspend fun getWeaponsByIds(ids: List<Int>): List<Weapon> {
        return dao.getWeaponsByIds(ids).map { entity ->
            Weapon(
                id = entity.id ?: 0,
                name = entity.name,
                damage = entity.damage ?: "",
                damageType = entity.damageType ?: "",
                cost = entity.cost ?: "",
                weight = entity.weight?.toString() ?: "0",
                properties = entity.propertiesJson ?: ""
            )
        }
    }

    override fun getAllArmor(): Flow<List<ArmorEntity>> {
        return dao.getAllArmor()
    }

    override suspend fun searchEquipment(query: String): List<EquipmentEntity> {
        return dao.searchEquipment(query)
    }

    override suspend fun getEquipmentIdsByNames(idxNames: List<String>): List<Int> {
        return dao.getEquipmentIdsByIdxNames(idxNames)
    }

    // ИСПРАВЛЕНИЕ: Методы реализованы и используют добавленные в DAO запросы
    override suspend fun getFeatureById(id: Int): Feature? {
        return dao.getFeatureById(id)?.let { mapFeature(it) }
    }

    override suspend fun getFeatureByName(name: String): Feature? {
        return dao.getFeatureByIndex(name)?.let { mapFeature(it) }
    }

    // --- ВНУТРЕННИЕ МАППЕРЫ ---

    private suspend fun parseSmartChoice(obj: JsonObject): FeatureChoiceDomain {
        val count = obj["choose"]?.jsonPrimitive?.int
            ?: obj["count"]?.jsonPrimitive?.int
            ?: 1
        val type = obj["type"]?.jsonPrimitive?.content ?: ""
        val options = mutableListOf<ChoiceOption>()

        val optionsArray = obj["options"]?.jsonArray
            ?: obj["from"]?.jsonObject?.get("options")?.jsonArray
            ?: obj["from"]?.jsonObject?.get("item")?.jsonArray

        optionsArray?.forEach { element ->
            try {
                val optObj = element.jsonObject
                val id = optObj["value"]?.jsonPrimitive?.content
                    ?: optObj["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                    ?: ""
                val label = optObj["label"]?.jsonPrimitive?.content
                    ?: optObj["item"]?.jsonObject?.get("name")?.jsonPrimitive?.content
                    ?: id

                val subChoiceJson = optObj["sub_choice"]?.jsonObject
                // Рекурсивный вызов - безопасно, так как метод suspend
                val subChoice = subChoiceJson?.let { parseSmartChoice(it) }

                options.add(ChoiceOption(id = id, label = label, subChoice = subChoice))
            } catch (e: Exception) {}
        }

        if (options.isEmpty()) {
            val catIdx = obj["from"]?.jsonObject?.get("equipment_category")?.jsonObject?.get("index")?.jsonPrimitive?.content
            if (catIdx != null) {
                dao.getEquipmentByCategory(catIdx).forEach { eq ->
                    options.add(ChoiceOption(id = eq.indexName, label = eq.name))
                }
            }
        }

        return when {
            type == "string_choice" -> FeatureChoiceDomain.SelectOption(count, options, isStringChoice = true)
            type.contains("skill") || options.any { it.id.contains("skill-") } ->
                FeatureChoiceDomain.SelectSkill(count, options)
            type.contains("spell") ->
                FeatureChoiceDomain.SelectSpell(count, "fixed", options)
            else ->
                FeatureChoiceDomain.SelectOption(count, options)
        }
    }

    private suspend fun mapFeature(entity: FeatureEntity): Feature {
        val choices = mutableListOf<FeatureChoiceDomain>()

        entity.choicesJson?.let { raw ->
            try {
                val element = json.parseToJsonElement(raw)
                if (element is JsonArray) {
                    element.forEach {
                        choices.add(parseSmartChoice(it.jsonObject))
                    }
                } else if (element is JsonObject) {
                    choices.add(parseSmartChoice(element))
                }
            } catch (ex: Exception) {}
        }

        if (choices.isEmpty() && entity.indexName.contains("expertise")) {
            choices.add(FeatureChoiceDomain.SelectExpertise(count = 2, options = emptyList()))
        }

        return Feature(
            id = entity.id ?: 0,
            index = entity.indexName,
            name = entity.name,
            description = entity.description?.stripHtml() ?: "",
            choices = choices,
            changeRule = entity.changeRule == 1,
            priority = 100
        )
    }

    private fun mapSpell(e: SpellEntity): Spell {
        return Spell(
            id = e.id ?: 0,
            index = e.indexName,
            name = e.name,
            level = e.level ?: 0,
            school = e.school ?: "",
            castingTime = e.castingTime ?: "",
            range = e.range ?: "",
            components = e.componentsJson ?: "",
            duration = e.duration ?: "",
            description = e.description ?: "",
            isConcentration = e.concentration == 1,
            isRitual = e.ritual == 1
        )
    }

    private fun parseJsonStrings(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = json.decodeFromString<List<JsonObject>>(raw)
            array.mapNotNull { it["string"]?.jsonPrimitive?.content ?: it["desc"]?.jsonPrimitive?.content }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/LibraryRepositoryImpl.kt