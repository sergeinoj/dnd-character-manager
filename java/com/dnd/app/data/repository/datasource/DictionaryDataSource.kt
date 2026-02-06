// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/DictionaryDataSource.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.data.local.entity.ArmorEntity
import com.dnd.app.data.local.entity.EquipmentEntity
import com.dnd.app.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * [ОБНОВЛЕНО v2.1] Логика предысторий (Backgrounds) полностью удалена.
 */
interface DictionaryDataSource {
    suspend fun getAllAlignments(): List<AlignmentEntity>
    fun getAllSpells(): Flow<List<Spell>>
    suspend fun getSpellsByIds(ids: List<Int>): List<Spell>
    fun getAllWeapons(): Flow<List<Weapon>>
    suspend fun getWeaponsByIds(ids: List<Int>): List<Weapon>
    fun getAllArmor(): Flow<List<ArmorEntity>>
    suspend fun getEquipmentIdsByNames(idxNames: List<String>): List<Int>
    suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity>

    // --- v1.25 SHOP ---
    suspend fun getRootShopCategories(): List<ShopCategory>
    suspend fun getChildShopCategories(parentIndex: String): List<ShopCategory>
    suspend fun getItemsForCategory(categoryIndex: String): List<ShopItem>
    suspend fun searchAllItems(query: String): List<ShopItem>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/DictionaryDataSource.kt