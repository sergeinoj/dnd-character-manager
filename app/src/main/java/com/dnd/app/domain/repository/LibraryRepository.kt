// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\repository\LibraryRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.repository

import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.data.local.entity.ArmorEntity
import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.data.local.entity.EquipmentEntity
import com.dnd.app.data.local.entity.LanguageEntity
import com.dnd.app.data.local.entity.ProficiencyEntity
import com.dnd.app.data.local.entity.WeaponEntity
import com.dnd.app.data.local.entity.SkillEntity
import com.dnd.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow



interface LibraryRepository {

    suspend fun getAllClasses(): List<ClassInfo>
    suspend fun getSubclassesForClass(classIndex: String): List<SubclassInfo>
    suspend fun getClassEntityByIndex(index: String): ClassEntity?


    suspend fun getClassFeaturesForLevel(
        classIndex: String,
        level: Int,
        subclassIndex: String? = null,
        abilityModifier: Int = 0,
        isGenesis: Boolean = true,
        proficiencyProvider: (() -> Map<String, Int>)? = null
    ): ClassFeaturesForLevel


    suspend fun getAllParentRaces(): List<Race>
    suspend fun getSubracesFromDb(parentId: Int): List<Race>
    suspend fun getBaseRaceFeatures(raceId: Int): List<Feature>
    suspend fun getSubraceFeatures(subraceIndex: String): List<Feature>
    suspend fun getAllBackgrounds(): List<Background>
    suspend fun getBackgroundByIndex(index: String): Background?
    suspend fun getAllAlignments(): List<AlignmentEntity>
    suspend fun getRaceByIndex(index: String): Race?
    suspend fun getRaceFullData(index: String): RaceFullData?
    suspend fun getSubraceModelByIndex(index: String): Race?
    suspend fun getAllLanguages(): List<LanguageEntity>


    suspend fun getAllSkills(): List<SkillEntity>
    suspend fun getAllProficiencies(): List<ProficiencyEntity>


    fun getAllSpells(): Flow<List<Spell>>
    suspend fun getSpellsByIds(ids: List<Int>): List<Spell>
    suspend fun getSpellsByIndexes(indexes: List<String>): List<Spell>
    suspend fun getAllSpellsByClass(classIndex: String): List<Spell>

    fun getAllWeapons(): Flow<List<Weapon>>
    suspend fun getWeaponsByIds(ids: List<Int>): List<Weapon>
    fun getAllArmor(): Flow<List<ArmorEntity>>
    suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity>
    suspend fun getWeaponsByIndexes(indexes: List<String>): List<WeaponEntity>
    suspend fun getArmorByIndexes(indexes: List<String>): List<ArmorEntity>

    suspend fun getRootShopCategories(): List<ShopCategory>
    suspend fun getChildShopCategories(parentIndex: String): List<ShopCategory>
    suspend fun getItemsForCategory(categoryIndex: String): List<ShopItem>
    suspend fun searchAllItems(query: String): List<ShopItem>


    suspend fun getFeatureById(id: Int): Feature?
    suspend fun getFeatureByName(name: String): Feature?
    suspend fun getEquipmentIdsByNames(idxNames: List<String>): List<Int>
    suspend fun getFeatureByIndex(index: String): Feature?
    val featMetadataRegistry: StateFlow<Map<String, Feature>>
    fun publishFeatMetadataRegistry(registry: Map<String, Feature>)
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\repository\LibraryRepository.kt
