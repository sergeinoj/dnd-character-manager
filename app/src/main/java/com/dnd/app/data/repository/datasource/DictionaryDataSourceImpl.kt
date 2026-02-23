// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\DictionaryDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*
import com.dnd.app.domain.model.*
import com.dnd.app.util.DndLocalization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DictionaryDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao,
    private val spellDataSource: SpellDataSource
) : DictionaryDataSource {

    override suspend fun getAllAlignments(): List<AlignmentEntity> = dao.getAllAlignments()
    override fun getAllSpells(): Flow<List<Spell>> = spellDataSource.getAllSpells()
    override suspend fun getSpellsByIds(ids: List<Int>): List<Spell> = spellDataSource.getSpellsByIds(ids)
    override fun getAllArmor(): Flow<List<ArmorEntity>> = dao.getAllArmor()
    override suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity> = dao.getEquipmentByIndexes(indexes)
    override suspend fun getEquipmentIdsByNames(idxNames: List<String>): List<Int> = dao.getEquipmentIdsByIdxNames(idxNames)

    override suspend fun getWeaponsByIndexes(indexes: List<String>): List<WeaponEntity> = dao.getWeaponsByIndexes(indexes)
    override suspend fun getArmorByIndexes(indexes: List<String>): List<ArmorEntity> = dao.getArmorByIndexes(indexes)

    override suspend fun getAllLanguages(): List<LanguageEntity> = dao.getAllLanguages()

    override suspend fun getRootShopCategories(): List<ShopCategory> {
        return dao.getRootEquipmentCategories().map { ShopCategory(it.indexName, it.name) }
    }

    override suspend fun getChildShopCategories(parentIndex: String): List<ShopCategory> {
        return dao.getChildEquipmentCategories(parentIndex).map { ShopCategory(it.indexName, it.name) }
    }

    override suspend fun getItemsForCategory(categoryIndex: String): List<ShopItem> {
        val allCategoryIndexes = mutableSetOf(categoryIndex)
        allCategoryIndexes.addAll(getAllChildCategoryIndexesRecursive(categoryIndex))

        val itemIndexes = allCategoryIndexes.flatMap { dao.getLinksForCategory(it) }.distinct()
        if (itemIndexes.isEmpty()) return emptyList()

        val weapons = dao.getWeaponsByIndexes(itemIndexes)
        val armor = dao.getArmorByIndexes(itemIndexes)
        val equipment = dao.getEquipmentByIndexes(itemIndexes)
        val magicItems = dao.getMagicItemsByIndexes(itemIndexes)

        val damageTypeIndices = weapons.mapNotNull { it.damageType }.distinct()
        val damageTypeMap = damageTypeIndices.mapNotNull { dao.getDamageTypeByIndex(it) }.associate { it.indexName to it.name }

        val mappedWeapons = weapons.map { mapWeaponToShopItem(it, damageTypeMap) }
        val mappedArmor = armor.map(::mapArmorToShopItem)
        val mappedEquipment = equipment.map(::mapEquipmentToShopItem)
        val mappedMagic = magicItems.map(::mapMagicItemToShopItem)

        return (mappedWeapons + mappedArmor + mappedEquipment + mappedMagic).sortedBy { it.name }
    }

    override suspend fun searchAllItems(query: String): List<ShopItem> {
        val weapons = dao.searchWeapons(query)
        val armor = dao.searchArmor(query).map(::mapArmorToShopItem)
        val equipment = dao.searchEquipment(query).map(::mapEquipmentToShopItem)
        val magicItems = dao.searchMagicItems(query)

        val damageTypeIndices = weapons.mapNotNull { it.damageType }.distinct()
        val damageTypeMap = damageTypeIndices.mapNotNull { dao.getDamageTypeByIndex(it) }.associate { it.indexName to it.name }

        val mappedWeapons = weapons.map { mapWeaponToShopItem(it, damageTypeMap) }
        val mappedMagic = magicItems.map(::mapMagicItemToShopItem)

        return (mappedWeapons + armor + equipment + mappedMagic).sortedBy { it.name }
    }

    override fun getAllWeapons(): Flow<List<Weapon>> {
        return dao.getAllWeapons().map { entities ->
            entities.map { entity ->
                Weapon(
                    id = entity.id ?: 0,
                    name = entity.name,
                    damage = entity.damage ?: "",
                    damageType = entity.damageType ?: "",
                    cost = "0",
                    weight = entity.weight.toString(),
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
                cost = "0",
                weight = entity.weight.toString(),
                properties = entity.propertiesJson ?: ""
            )
        }
    }

    private fun mapWeaponToShopItem(entity: WeaponEntity, damageTypeMap: Map<String, String>) = ShopItem(
        index = entity.indexName,
        name = entity.name,
        cost = Money.fromCp(entity.costCp),
        weight = entity.weight,
        description = DndLocalization.assembleEnrichedDescription(
            rarity = DndLocalization.translateRarity(entity.rarity),
            stats = DndLocalization.formatWeaponInfo(entity.damage, damageTypeMap[entity.damageType] ?: entity.damageType),
            description = entity.description
        )
    )

    private fun mapArmorToShopItem(entity: ArmorEntity) = ShopItem(
        index = entity.indexName,
        name = entity.name,
        cost = Money.fromCp(entity.costCp),
        weight = entity.weight,
        description = DndLocalization.assembleEnrichedDescription(
            rarity = DndLocalization.translateRarity(entity.rarity),
            stats = DndLocalization.formatArmorInfo(entity.acBase),
            description = entity.description
        )
    )

    private fun mapEquipmentToShopItem(entity: EquipmentEntity) = ShopItem(
        index = entity.indexName,
        name = entity.name,
        cost = Money.fromCp(entity.costCp),
        weight = entity.weight,
        description = entity.description
    )

    private fun mapMagicItemToShopItem(entity: MagicItemEntity) = ShopItem(
        index = entity.indexName,
        name = entity.name,
        cost = Money.fromCp(entity.costCp),
        weight = entity.weight.takeIf { it > 0.0 },
        description = DndLocalization.assembleEnrichedDescription(
            rarity = DndLocalization.translateRarity(entity.rarity),
            stats = if (entity.bonusAc > 0) "Бонус к КД: +${entity.bonusAc}" else null,
            description = entity.description
        )
    )

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
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\DictionaryDataSourceImpl.kt