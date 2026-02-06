// Имя файла: app/src/main/java/com/dnd/app/data/repository/LibraryRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.data.local.entity.ArmorEntity
import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.data.local.entity.EquipmentEntity
import com.dnd.app.data.repository.datasource.DictionaryDataSource
import com.dnd.app.data.repository.datasource.FeatDataSource
import com.dnd.app.data.repository.datasource.RaceDataSource
import com.dnd.app.data.repository.datasource.OldClassDataSource
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.BackgroundRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.class_feature_orchestration.GetFeaturesForLevelUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val raceSource: RaceDataSource,
    private val classSource: OldClassDataSource,
    private val dictionarySource: DictionaryDataSource,
    private val featSource: FeatDataSource,
    private val backgroundRepository: BackgroundRepository, // НОВЫЙ РЕПОЗИТОРИЙ
    private val getFeaturesForLevelUseCase: GetFeaturesForLevelUseCase
) : LibraryRepository {

    // --- BackgroundRepository Delegation (ИЗОЛИРОВАНО) ---
    override suspend fun getAllBackgrounds(): List<Background> = backgroundRepository.getBackgrounds()

    // --- RaceDataSource Delegation ---
    override suspend fun getAllParentRaces(): List<Race> = raceSource.getAllParentRaces()
    override suspend fun getSubracesFromDb(parentId: Int): List<Race> = raceSource.getSubracesFromDb(parentId)
    override suspend fun getBaseRaceFeatures(raceId: Int): List<Feature> = raceSource.getBaseRaceFeatures(raceId)
    override suspend fun getSubraceFeatures(subraceIndex: String): List<Feature> = raceSource.getSubraceFeatures(subraceIndex)
    override suspend fun getRaceByIndex(index: String): Race? = raceSource.getRaceByIndex(index)
    override suspend fun getSubraceModelByIndex(index: String): Race? = raceSource.getSubraceModelByIndex(index)

    // --- ClassDataSource Delegation (Simple Methods) ---
    override suspend fun getAllClasses(): List<ClassInfo> = classSource.getAllClasses()
    override suspend fun getSubclassesForClass(classIndex: String): List<SubclassInfo> = classSource.getSubclassesForClass(classIndex)
    override suspend fun getClassEntityByIndex(index: String): ClassEntity? = classSource.getClassEntityByIndex(index)
    override suspend fun getClassFeaturesForLevel(classIndex: String, level: Int, subclassIndex: String?, abilityModifier: Int): ClassFeaturesForLevel =
        getFeaturesForLevelUseCase(classIndex, level, subclassIndex, abilityModifier)

    // --- DictionaryDataSource Delegation ---
    override suspend fun getAllAlignments(): List<AlignmentEntity> = dictionarySource.getAllAlignments()
    override fun getAllSpells(): Flow<List<Spell>> = dictionarySource.getAllSpells()
    override suspend fun getSpellsByIds(ids: List<Int>): List<Spell> = dictionarySource.getSpellsByIds(ids)
    override fun getAllWeapons(): Flow<List<Weapon>> = dictionarySource.getAllWeapons()
    override suspend fun getWeaponsByIds(ids: List<Int>): List<Weapon> = dictionarySource.getWeaponsByIds(ids)
    override fun getAllArmor(): Flow<List<ArmorEntity>> = dictionarySource.getAllArmor()
    override suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity> = dictionarySource.getEquipmentByIndexes(indexes)
    override suspend fun getEquipmentIdsByNames(idxNames: List<String>): List<Int> = dictionarySource.getEquipmentIdsByNames(idxNames)

    // --- SHOP ---
    override suspend fun getRootShopCategories(): List<ShopCategory> = dictionarySource.getRootShopCategories()
    override suspend fun getChildShopCategories(parentIndex: String): List<ShopCategory> = dictionarySource.getChildShopCategories(parentIndex)
    override suspend fun getItemsForCategory(categoryIndex: String): List<ShopItem> = dictionarySource.getItemsForCategory(categoryIndex)
    override suspend fun searchAllItems(query: String): List<ShopItem> = dictionarySource.searchAllItems(query)

    // --- FeatDataSource Delegation ---
    override suspend fun getFeatureById(id: Int): Feature? = featSource.getFeatureById(id)
    override suspend fun getFeatureByName(name: String): Feature? = featSource.getFeatureByIndex(name)
    override suspend fun getFeatureByIndex(index: String): Feature? = featSource.getFeatureByIndex(index)
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/LibraryRepositoryImpl.kt