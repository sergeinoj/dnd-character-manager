// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/DictionaryDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*
import com.dnd.app.data.model.ReferenceJson
import com.dnd.app.domain.model.*
import com.dnd.app.domain.usecase.class_feature_orchestration.FeatureFactory
import com.dnd.app.util.stripHtml
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class DictionaryDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao,
    private val spellDataSource: SpellDataSource,
    // Используем Provider для предотвращения циклической зависимости, так как FeatureFactory нужен нам для парсинга выборов предыстории
    private val featureFactoryProvider: Provider<FeatureFactory>
) : DictionaryDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun getAllBackgrounds(): List<Background> {
        val featureFactory = featureFactoryProvider.get()

        return dao.getAllBackgrounds().map { entity ->
            val allFeatures = mutableListOf<Feature>()

            // [ИЗМЕНЕНО v1.26] Логика теперь эксклюзивная. Сначала проверяем новый формат.
            val featureIndices = entity.featureIndicesJson?.let {
                try { json.decodeFromString<List<String>>(it) } catch (e: Exception) { emptyList() }
            } ?: emptyList()

            if (featureIndices.isNotEmpty()) {
                // Новый формат: загрузка способностей по индексам
                featureIndices.forEach { index ->
                    dao.getFeatureByIndex(index)?.let { featEntity ->
                        allFeatures.add(featureFactory.create(featEntity))
                    }
                }
            } else {
                // Старый формат (fallback): парсинг выборов навыков и языков
                entity.startingProficienciesJson?.let { raw ->
                    try {
                        val obj = json.parseToJsonElement(raw).jsonObject
                        val choice = featureFactory.parseChoice(obj)
                        allFeatures.add(Feature(
                            id = -300 - (entity.id ?: 0),
                            index = "bg-prof-choice-${entity.indexName}",
                            name = "Владения предыстории",
                            description = "Выберите навыки, предоставляемые вашей предысторией.",
                            choices = listOf(choice),
                            uiGroup = "SKILLS"
                        ))
                    } catch (e: Exception) { Log.e("BG_MAPPER", "Error parsing bg profs for ${entity.indexName}") }
                }

                entity.languageOptionsJson?.let { raw ->
                    try {
                        val obj = json.parseToJsonElement(raw).jsonObject
                        val choice = featureFactory.parseChoice(obj)
                        allFeatures.add(Feature(
                            id = -400 - (entity.id ?: 0),
                            index = "bg-lang-choice-${entity.indexName}",
                            name = "Языки предыстории",
                            description = "Выберите дополнительные языки.",
                            choices = listOf(choice),
                            uiGroup = "GENERAL"
                        ))
                    } catch (e: Exception) { Log.e("BG_MAPPER", "Error parsing bg langs for ${entity.indexName}") }
                }
            }

            // [НОВЫЙ БЛОК v1.26] Обработка выбора снаряжения предыстории
            entity.startingEquipmentOptionsJson?.let { raw ->
                try {
                    json.decodeFromString<List<JsonObject>>(raw).forEachIndexed { i, choiceJson ->
                        val choiceDomain = featureFactory.parseChoice(choiceJson)
                        // [ИСПРАВЛЕНО] Добавлено безопасное приведение типа для доступа к description
                        val name = (choiceDomain as? FeatureChoiceDomain.SelectOption)?.description ?: "Снаряжение предыстории"
                        allFeatures.add(Feature(
                            id = -500 - (entity.id ?: 0) - i,
                            index = "bg-equip-choice-${entity.indexName}-$i",
                            name = name,
                            description = "Выберите стартовое снаряжение.",
                            choices = listOf(choiceDomain),
                            uiGroup = "INVENTORY"
                        ))
                    }
                } catch (e: Exception) { Log.e("BG_MAPPER", "Error parsing bg equip options for ${entity.indexName}", e) }
            }


            val staticEquipment = parseSimpleReference(entity.startingEquipmentJson)

            Background(
                id = entity.id ?: 0,
                name = entity.name,
                features = allFeatures.distinctBy { it.index },
                staticEquipment = staticEquipment,
                startingGold = entity.startingGold ?: 0,
                featureIndices = featureIndices,
                personalityTraits = parseJsonStrings(entity.personalityTraitsJson),
                ideals = parseJsonStrings(entity.idealsJson),
                bonds = parseJsonStrings(entity.bondsJson),
                flaws = parseJsonStrings(entity.flawsJson)
            )
        }
    }

    override suspend fun getAllAlignments(): List<AlignmentEntity> = dao.getAllAlignments()
    override fun getAllSpells(): Flow<List<Spell>> = spellDataSource.getAllSpells()
    override suspend fun getSpellsByIds(ids: List<Int>): List<Spell> = spellDataSource.getSpellsByIds(ids)
    override fun getAllArmor(): Flow<List<ArmorEntity>> = dao.getAllArmor()
    override suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity> = dao.getEquipmentByIndexes(indexes)
    override suspend fun getEquipmentIdsByNames(idxNames: List<String>): List<Int> = dao.getEquipmentIdsByIdxNames(idxNames)

    override suspend fun getRootShopCategories(): List<ShopCategory> {
        return dao.getRootEquipmentCategories().map { ShopCategory(it.indexName, it.name) }
    }

    override suspend fun getChildShopCategories(parentIndex: String): List<ShopCategory> {
        return dao.getChildEquipmentCategories(parentIndex).map { ShopCategory(it.indexName, it.name) }
    }

    override suspend fun getItemsForCategory(categoryIndex: String): List<ShopItem> {
        // [ИСПРАВЛЕНИЕ v1.26.1] Рекурсивный поиск по категориям
        val allCategoryIndexes = mutableSetOf(categoryIndex)
        allCategoryIndexes.addAll(getAllChildCategoryIndexesRecursive(categoryIndex))

        val itemIndexes = allCategoryIndexes.flatMap { dao.getLinksForCategory(it) }.distinct()
        if (itemIndexes.isEmpty()) return emptyList()

        val weapons = dao.getWeaponsByIndexes(itemIndexes).map(::mapWeaponToShopItem)
        val armor = dao.getArmorByIndexes(itemIndexes).map(::mapArmorToShopItem)
        val equipment = dao.getEquipmentByIndexes(itemIndexes).map(::mapEquipmentToShopItem)
        val magicItems = dao.getMagicItemsByIndexes(itemIndexes).map(::mapMagicItemToShopItem)

        return (weapons + armor + equipment + magicItems).sortedBy { it.name }
    }

    override suspend fun searchAllItems(query: String): List<ShopItem> {
        val weapons = dao.searchWeapons(query).map(::mapWeaponToShopItem)
        val armor = dao.searchArmor(query).map(::mapArmorToShopItem)
        val equipment = dao.searchEquipment(query).map(::mapEquipmentToShopItem)
        val magicItems = dao.searchMagicItems(query).map(::mapMagicItemToShopItem)
        return (weapons + armor + equipment + magicItems).sortedBy { it.name }
    }

    override fun getAllWeapons(): Flow<List<Weapon>> {
        return dao.getAllWeapons().map { entities ->
            entities.map { entity ->
                Weapon(
                    id = entity.id ?: 0, name = entity.name, damage = entity.damage ?: "",
                    damageType = entity.damageType ?: "", cost = "0",
                    weight = entity.weight?.toString() ?: "0", properties = entity.propertiesJson ?: ""
                )
            }
        }
    }

    override suspend fun getWeaponsByIds(ids: List<Int>): List<Weapon> {
        return dao.getWeaponsByIds(ids).map { entity ->
            Weapon(
                id = entity.id ?: 0, name = entity.name, damage = entity.damage ?: "",
                damageType = entity.damageType ?: "", cost = "0",
                weight = entity.weight?.toString() ?: "0", properties = entity.propertiesJson ?: ""
            )
        }
    }

    private fun mapWeaponToShopItem(entity: WeaponEntity) = ShopItem(
        index = entity.indexName, name = entity.name,
        cost = Money.fromCp(entity.costCp ?: 0), weight = entity.weight,
        description = "Урон: ${entity.damage} ${entity.damageType}"
    )

    private fun mapArmorToShopItem(entity: ArmorEntity) = ShopItem(
        index = entity.indexName, name = entity.name,
        cost = Money.fromCp(entity.costCp ?: 0), weight = entity.weight,
        description = "Класс доспеха: ${entity.acBase}"
    )

    private fun mapEquipmentToShopItem(entity: EquipmentEntity) = ShopItem(
        index = entity.indexName, name = entity.name,
        cost = Money.fromCp(entity.costCp ?: 0), weight = entity.weight,
        description = entity.description
    )

    private fun mapMagicItemToShopItem(entity: MagicItemEntity) = ShopItem(
        index = entity.indexName, name = entity.name,
        cost = Money.fromCp(entity.costCp ?: 0), weight = null,
        description = entity.description
    )

    private suspend fun mapBackgroundFeature(entity: FeatureEntity): Feature {
        return Feature(
            id = entity.id ?: 0, index = entity.indexName, name = entity.name,
            description = entity.description?.stripHtml() ?: "",
            uiGroup = entity.uiGroup ?: "GENERAL"
        )
    }

    private fun parseSimpleReference(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val element = json.parseToJsonElement(raw)
            when (element) {
                is JsonArray -> element.mapNotNull { it.jsonObject["index"]?.jsonPrimitive?.content }
                is JsonObject -> listOfNotNull(element["index"]?.jsonPrimitive?.content)
                else -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseJsonStrings(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (e: Exception) {
            Log.e("DND_PARSER", "Background traits parsing failed for: $raw")
            emptyList()
        }
    }

    /**
     * [НОВЫЙ МЕТОД v1.26.1] Рекурсивно собирает индексы всех дочерних категорий.
     */
    private suspend fun getAllChildCategoryIndexesRecursive(parentIndex: String): Set<String> {
        val result = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(parentIndex)

        while (queue.isNotEmpty()) {
            val currentParent = queue.removeFirst()
            val children = dao.getChildEquipmentCategories(currentParent)
            for (child in children) {
                if (result.add(child.indexName)) {
                    queue.add(child.indexName)
                }
            }
        }
        return result
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/DictionaryDataSourceImpl.kt