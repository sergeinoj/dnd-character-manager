// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/DictionaryDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*
import com.dnd.app.domain.model.*
import com.dnd.app.util.stripHtml
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao,
    private val spellDataSource: SpellDataSource
) : DictionaryDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    override suspend fun getAllBackgrounds(): List<Background> {
        return dao.getAllBackgrounds().map { entity ->
            val allFeatures = mutableListOf<Feature>()
            entity.featureIndex?.split(",")?.map { it.trim() }?.forEach { featureIndex ->
                dao.getFeatureByIndex(featureIndex)?.let { featureEntity ->
                    allFeatures.add(mapBackgroundFeature(featureEntity))
                }
            }
            val staticEquipment = parseSimpleReference(entity.startingEquipmentJson)
            Background(
                id = entity.id ?: 0,
                name = entity.name,
                features = allFeatures,
                staticEquipment = staticEquipment,
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

    // --- v1.25 SHOP IMPLEMENTATION ---

    override suspend fun getRootShopCategories(): List<ShopCategory> {
        return dao.getRootEquipmentCategories().map { ShopCategory(it.indexName, it.name ?: "Без названия") }
    }

    override suspend fun getChildShopCategories(parentIndex: String): List<ShopCategory> {
        return dao.getChildEquipmentCategories(parentIndex).map { ShopCategory(it.indexName, it.name ?: "Без названия") }
    }

    override suspend fun getItemsForCategory(categoryIndex: String): List<ShopItem> {
        val itemIndexes = dao.getLinksForCategory(categoryIndex)
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


    // --- LEGACY ---

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

    // --- MAPPERS ---

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
            json.decodeFromString<List<JsonObject>>(raw).mapNotNull {
                it["index"]?.jsonPrimitive?.content
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseJsonStrings(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (e: Exception) {
            try {
                val array = json.decodeFromString<List<JsonObject>>(raw)
                array.mapNotNull { it["string"]?.jsonPrimitive?.content ?: it["desc"]?.jsonPrimitive?.content }
            } catch (e2: Exception) { emptyList() }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/DictionaryDataSourceImpl.kt