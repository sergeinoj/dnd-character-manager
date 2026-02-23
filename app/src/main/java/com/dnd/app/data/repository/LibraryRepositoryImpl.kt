// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\LibraryRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.data.local.entity.ArmorEntity
import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.data.local.entity.EquipmentEntity
import com.dnd.app.data.local.entity.LanguageEntity
import com.dnd.app.data.local.entity.ProficiencyEntity
import com.dnd.app.data.local.entity.SkillEntity
import com.dnd.app.data.local.entity.WeaponEntity
import com.dnd.app.data.repository.datasource.*
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.BackgroundRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassProgressionUseCase
import com.dnd.app.domain.usecase.class_feature_orchestration.FeatureFactory
import com.dnd.app.domain.usecase.class_feature_orchestration.GetFeaturesForLevelUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton



@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val raceSource: RaceDataSource,
    private val classRepo: ClassFeatureRepository,
    private val dictionarySource: DictionaryDataSource,
    private val spellSource: SpellDataSource,
    private val featSource: FeatDataSource,
    private val backgroundRepository: BackgroundRepository,
    private val getFeaturesForLevelUseCase: GetFeaturesForLevelUseCase,
    private val progressionUseCase: ClassProgressionUseCase,
    private val json: Json,
    private val featureFactory: FeatureFactory
) : LibraryRepository {

    private val _featMetadataRegistry = MutableStateFlow<Map<String, Feature>>(emptyMap())
    override val featMetadataRegistry: StateFlow<Map<String, Feature>> = _featMetadataRegistry.asStateFlow()

    override suspend fun getAllClasses(): List<ClassInfo> {
        val classEntities = classRepo.getAllClassesEntities()
        return classEntities.map { entity ->
            val subclasses = getSubclassesForClass(entity.indexName)
            val requirements = progressionUseCase.parseMulticlassPrerequisites(entity)
            ClassInfo(
                id = entity.id ?: 0,
                index = entity.indexName,
                name = entity.name,
                hitDie = entity.hitDie,
                subclasses = subclasses,
                multiclassRequirements = requirements
            )
        }
    }

    override suspend fun getSubclassesForClass(classIndex: String): List<SubclassInfo> {
        return classRepo.getSubclassesForClassEntity(classIndex).map { entity ->
            SubclassInfo(
                index = entity.indexName,
                name = entity.name,
                flavor = entity.subclassFlavor ?: "",
                description = entity.desc ?: ""
            )
        }
    }

    override suspend fun getClassEntityByIndex(index: String): ClassEntity? {
        return classRepo.getClassEntity(index)
    }


    override suspend fun getClassFeaturesForLevel(
        classIndex: String,
        level: Int,
        subclassIndex: String?,
        abilityModifier: Int,
        isGenesis: Boolean,
        proficiencyProvider: (() -> Map<String, Int>)?
    ): ClassFeaturesForLevel {
        return getFeaturesForLevelUseCase.invokeWithContext(
            classIndex = classIndex,
            level = level,
            subclassIndex = subclassIndex,
            abilityModifier = abilityModifier,
            isGenesis = isGenesis,
            proficiencyProvider = proficiencyProvider
        )
    }

    override suspend fun getAllBackgrounds(): List<Background> = backgroundRepository.getBackgrounds()
    override suspend fun getBackgroundByIndex(index: String): Background? = backgroundRepository.getBackgroundByIndex(index)


    override suspend fun getAllParentRaces(): List<Race> = raceSource.getAllParentRaces()
    override suspend fun getSubracesFromDb(parentId: Int): List<Race> = raceSource.getSubracesFromDb(parentId)
    override suspend fun getBaseRaceFeatures(raceId: Int): List<Feature> = raceSource.getBaseRaceFeatures(raceId)
    override suspend fun getSubraceFeatures(subraceIndex: String): List<Feature> = raceSource.getSubraceFeatures(subraceIndex)
    override suspend fun getRaceByIndex(index: String): Race? = raceSource.getRaceByIndex(index)
    override suspend fun getRaceFullData(index: String): RaceFullData? = raceSource.getRaceFullData(index)
    override suspend fun getSubraceModelByIndex(index: String): Race? = raceSource.getSubraceModelByIndex(index)


    override suspend fun getAllAlignments(): List<AlignmentEntity> = dictionarySource.getAllAlignments()
    override suspend fun getAllLanguages(): List<LanguageEntity> = dictionarySource.getAllLanguages()
    override suspend fun getAllSkills(): List<SkillEntity> = classRepo.getAllSkills()
    override suspend fun getAllProficiencies(): List<ProficiencyEntity> = classRepo.getAllProficiencies()

    override fun getAllSpells(): Flow<List<Spell>> = dictionarySource.getAllSpells()
    override suspend fun getSpellsByIds(ids: List<Int>): List<Spell> = dictionarySource.getSpellsByIds(ids)

    override suspend fun getSpellsByIndexes(indexes: List<String>): List<Spell> {
        val jsonString = json.encodeToString(indexes)
        return spellSource.getGrantedSpells(jsonString)
    }

    override suspend fun getAllSpellsByClass(classIndex: String): List<Spell> = spellSource.getAllSpellsByClass(classIndex)

    override fun getAllWeapons(): Flow<List<Weapon>> = dictionarySource.getAllWeapons()
    override suspend fun getWeaponsByIds(ids: List<Int>): List<Weapon> = dictionarySource.getWeaponsByIds(ids)
    override fun getAllArmor(): Flow<List<ArmorEntity>> = dictionarySource.getAllArmor()
    override suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity> = dictionarySource.getEquipmentByIndexes(indexes)
    override suspend fun getEquipmentIdsByNames(idxNames: List<String>): List<Int> = dictionarySource.getEquipmentIdsByNames(idxNames)
    override suspend fun getWeaponsByIndexes(indexes: List<String>): List<WeaponEntity> = dictionarySource.getWeaponsByIndexes(indexes)
    override suspend fun getArmorByIndexes(indexes: List<String>): List<ArmorEntity> = dictionarySource.getArmorByIndexes(indexes)


    override suspend fun getRootShopCategories(): List<ShopCategory> = dictionarySource.getRootShopCategories()
    override suspend fun getChildShopCategories(parentIndex: String): List<ShopCategory> = dictionarySource.getChildShopCategories(parentIndex)
    override suspend fun getItemsForCategory(categoryIndex: String): List<ShopItem> = dictionarySource.getItemsForCategory(categoryIndex)
    override suspend fun searchAllItems(query: String): List<ShopItem> = dictionarySource.searchAllItems(query)


    override suspend fun getFeatureById(id: Int): Feature? {
        return classRepo.getFeatureById(id)?.let { featureFactory.create(it) }
    }

    override suspend fun getFeatureByName(name: String): Feature? {
        return getFeatureByIndex(name)
    }

    override suspend fun getFeatureByIndex(index: String): Feature? {
        return classRepo.getFeatureByIndex(index)?.let { featureFactory.create(it) }
    }

    override fun publishFeatMetadataRegistry(registry: Map<String, Feature>) {
        _featMetadataRegistry.value = registry
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\LibraryRepositoryImpl.kt
