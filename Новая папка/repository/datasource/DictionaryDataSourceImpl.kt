// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/DictionaryDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*
import com.dnd.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ОБНОВЛЕНО v2.1] Логика работы с Backgrounds полностью удалена
 * и перенесена в BackgroundRepository/BackgroundOrchestrator.
 */
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