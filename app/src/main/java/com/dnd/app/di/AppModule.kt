// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\di\AppModule.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.di

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.repository.*
import com.dnd.app.data.repository.datasource.*
import com.dnd.app.data.repository.mapper.ProficiencyMapper
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.repository.BackgroundRepository
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.ItemRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.*
import com.dnd.app.domain.usecase.AttackPatternParser
import com.dnd.app.domain.usecase.EffectTriggerSystem
import com.dnd.app.domain.usecase.class_feature_orchestration.*
import com.dnd.app.domain.usecase.inventory.*
import com.dnd.app.domain.usecase.level_up.ValidateLevelUpUseCase
import com.dnd.app.domain.usecase.magic.ManagePreparedSpellsUseCase
import com.dnd.app.domain.usecase.magic.RestorationUseCase
import com.dnd.app.domain.usecase.magic.SpendSpellSlotUseCase
import com.dnd.app.domain.usecase.ConcentrationProtocol
import com.dnd.app.domain.usecase.snapshot.*
import com.dnd.app.ui.screens.sheet.MerchantManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides @Singleton
    fun provideRaceDataSource(impl: RaceDataSourceImpl): RaceDataSource = impl
    @Provides @Singleton
    fun provideClassFeatureRepository(impl: ClassDataSourceImpl): ClassFeatureRepository = impl
    @Provides @Singleton
    fun provideDictionaryDataSource(impl: DictionaryDataSourceImpl): DictionaryDataSource = impl
    @Provides @Singleton
    fun provideSpellDataSource(impl: SpellDataSourceImpl): SpellDataSource = impl
    @Provides @Singleton
    fun provideFeatDataSource(impl: FeatDataSourceImpl): FeatDataSource = impl
    @Provides @Singleton
    fun provideMonstersDataSource(impl: MonstersDataSourceImpl): MonstersDataSource = impl
    @Provides @Singleton
    fun provideBackgroundDataSource(impl: BackgroundDataSourceImpl): BackgroundDataSource = impl
    @Provides @Singleton
    fun provideBackgroundRepository(impl: BackgroundRepositoryImpl): BackgroundRepository = impl

    @Provides @Singleton
    fun provideLibraryRepository(
        raceSource: RaceDataSource,
        classRepo: ClassFeatureRepository,
        dictionarySource: DictionaryDataSource,
        spellSource: SpellDataSource,
        featSource: FeatDataSource,
        backgroundRepository: BackgroundRepository,
        getFeaturesForLevelUseCase: GetFeaturesForLevelUseCase,
        progressionUseCase: ClassProgressionUseCase,
        json: Json,
        featureFactory: FeatureFactory
    ): LibraryRepository {
        return LibraryRepositoryImpl(
            raceSource, classRepo, dictionarySource, spellSource, featSource,
            backgroundRepository, getFeaturesForLevelUseCase, progressionUseCase, json, featureFactory
        )
    }

    @Provides @Singleton
    fun provideCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository = impl
    @Provides @Singleton
    fun provideItemRepository(impl: ItemRepositoryImpl): ItemRepository = impl
    @Provides @Singleton
    fun provideDndCalculator(): DndCalculator = DndCalculator()
    @Provides @Singleton
    fun provideProficiencyMapper(dao: ReferenceDao): ProficiencyMapper = ProficiencyMapper(dao)

    @Provides @Singleton
    fun provideFeatureFactory(repo: ClassFeatureRepository, spells: SpellDataSource, dao: ReferenceDao): FeatureFactory = FeatureFactory(repo, spells, dao)
    @Provides @Singleton
    fun provideEffectTriggerSystem(dao: ReferenceDao, json: Json): EffectTriggerSystem = EffectTriggerSystem(dao, json)
    @Provides @Singleton
    fun provideClassProgressionUseCase(repo: ClassFeatureRepository, spells: SpellDataSource, factory: FeatureFactory, calc: DndCalculator): ClassProgressionUseCase = ClassProgressionUseCase(repo, spells, factory, calc)
    @Provides @Singleton
    fun provideFeatureEnricher(repo: ClassFeatureRepository): FeatureEnricher = FeatureEnricher(repo)
    @Provides @Singleton
    fun provideGetFeaturesForLevelUseCase(repo: ClassFeatureRepository, factory: FeatureFactory, progression: ClassProgressionUseCase, enricher: FeatureEnricher): GetFeaturesForLevelUseCase = GetFeaturesForLevelUseCase(repo, factory, progression, enricher)
    @Provides @Singleton
    fun providePartitionClassFeaturesUseCase(): PartitionClassFeaturesUseCase = PartitionClassFeaturesUseCase()
    @Provides @Singleton
    fun provideDraftStatsUseCase(repo: LibraryRepository): DraftStatsUseCase = DraftStatsUseCase(repo)
    @Provides @Singleton
    fun provideUpdateStatUseCase(draftStatsUseCase: DraftStatsUseCase): UpdateStatUseCase = UpdateStatUseCase(draftStatsUseCase)
    @Provides @Singleton
    fun provideSpellChoiceAggregatorUseCase(): SpellChoiceAggregatorUseCase = SpellChoiceAggregatorUseCase()

    @Provides @Singleton
    fun provideBakeCharacterUseCase(
        repo: ClassFeatureRepository, calc: DndCalculator, lib: LibraryRepository,
        draftStats: DraftStatsUseCase, mapper: ProficiencyMapper, json: Json
    ): BakeCharacterUseCase = BakeCharacterUseCase(lib, draftStats, mapper, repo, calc, json)

    @Provides @Singleton
    fun provideBackgroundOrchestrator(dao: ReferenceDao, featureFactory: FeatureFactory, json: Json): BackgroundOrchestrator = BackgroundOrchestrator(dao, featureFactory, json)
    @Provides @Singleton
    fun provideUnpackEquipmentUseCase(repo: ClassFeatureRepository, json: Json): UnpackEquipmentUseCase = UnpackEquipmentUseCase(repo, json)
    @Provides @Singleton
    fun provideGetClassProgressionDataUseCase(getFeatures: GetFeaturesForLevelUseCase, partition: PartitionClassFeaturesUseCase, spellAggregator: SpellChoiceAggregatorUseCase, unpackEquipment: UnpackEquipmentUseCase, calculator: DndCalculator): GetClassProgressionDataUseCase = GetClassProgressionDataUseCase(getFeatures, partition, spellAggregator, unpackEquipment, calculator)
    @Provides @Singleton
    fun provideGetBackgroundDataUseCase(repo: BackgroundRepository, unpack: UnpackEquipmentUseCase): GetBackgroundDataUseCase = GetBackgroundDataUseCase(repo, unpack)
    @Provides @Singleton
    fun provideGetAvailableShapesUseCase(
        monsters: MonstersDataSource,
        dao: ReferenceDao,
        json: Json
    ): GetAvailableShapesUseCase = GetAvailableShapesUseCase(monsters, dao, json)
    @Provides @Singleton
    fun provideCharacterExporter(repository: CharacterRepository, json: Json): CharacterExporter = CharacterExporter(repository, json)
    @Provides @Singleton
    fun provideCharacterImporter(repository: CharacterRepository, json: Json): CharacterImporter = CharacterImporter(repository, json)
    @Provides @Singleton
    fun provideAttackPatternParser(dao: ReferenceDao): AttackPatternParser = AttackPatternParser(dao)
    @Provides @Singleton
    fun provideHandleSelectionUseCase(): HandleSelectionUseCase = HandleSelectionUseCase()
    @Provides @Singleton
    fun provideValidateMulticlassPrerequisitesUseCase(
        classRepo: ClassFeatureRepository,
        progression: ClassProgressionUseCase
    ): ValidateMulticlassPrerequisitesUseCase = ValidateMulticlassPrerequisitesUseCase(classRepo, progression)
    @Provides @Singleton
    fun provideValidateDraftUseCase(
        lib: LibraryRepository,
        classRepo: ClassFeatureRepository,
        progression: GetClassProgressionDataUseCase,
        calculator: DndCalculator,
        validator: ValidateMulticlassPrerequisitesUseCase
    ): ValidateDraftUseCase = ValidateDraftUseCase(lib, classRepo, progression, calculator, validator)
    @Provides @Singleton
    fun provideValidateLevelUpUseCase(
        classRepo: ClassFeatureRepository,
        getClassProgressionDataUseCase: GetClassProgressionDataUseCase,
        calculator: DndCalculator,
        validator: ValidateMulticlassPrerequisitesUseCase
    ): ValidateLevelUpUseCase = ValidateLevelUpUseCase(classRepo, getClassProgressionDataUseCase, calculator, validator)
    @Provides @Singleton
    fun provideFeatResolver(json: Json): FeatResolver = FeatResolver(json)
    @Provides @Singleton
    fun provideFeatUiModelExtractor(): FeatUiModelExtractor = FeatUiModelExtractor()
    @Provides @Singleton
    fun provideUnpackItemUseCase(repo: ItemRepository, json: Json): UnpackItemUseCase = UnpackItemUseCase(repo, json)
    @Provides @Singleton
    fun provideCoreStatAssembler(calculator: DndCalculator): CoreStatAssembler = CoreStatAssembler(calculator)
    @Provides @Singleton
    fun provideSkillRegistryAssembler(calculator: DndCalculator): SkillRegistryAssembler = SkillRegistryAssembler(calculator)
    @Provides @Singleton
    fun provideTransformationApplier(calculator: DndCalculator): TransformationApplier = TransformationApplier(calculator)
    @Provides @Singleton
    fun provideDamageProcessor(): DamageProcessor = DamageProcessor()
    @Provides @Singleton
    fun provideFeatureRegistryAssembler(libraryRepository: LibraryRepository, json: Json): FeatureRegistryAssembler = FeatureRegistryAssembler(libraryRepository, json)
    @Provides @Singleton
    fun provideVitalsAssembler(json: Json): VitalsAssembler = VitalsAssembler(json)

    @Provides @Singleton
    fun provideMagicRegistryAssembler(libraryRepository: LibraryRepository, calculator: DndCalculator, json: Json): MagicRegistryAssembler = MagicRegistryAssembler(libraryRepository, calculator, json)

    @Provides @Singleton
    fun provideResourceRegistryAssembler(json: Json): ResourceRegistryAssembler = ResourceRegistryAssembler(json)
    @Provides @Singleton
    fun provideFeatureMechanicsProcessor(json: Json): FeatureMechanicsProcessor = FeatureMechanicsProcessor(json)
    @Provides @Singleton
    fun provideAresAssembler(calculator: DndCalculator, processor: FeatureMechanicsProcessor, json: Json): AresAssembler = AresAssembler(calculator, processor, json)
    @Provides @Singleton
    fun provideInventoryReconciler(itemFabricator: ItemFabricator, weightCalculator: CalculateWeightUseCase): InventoryReconciler = InventoryReconciler(itemFabricator, weightCalculator)
    @Provides @Singleton
    fun provideInventoryModifierExtractor(json: Json): InventoryModifierExtractor = InventoryModifierExtractor(json)

    @Provides @Singleton
    fun provideSnapshotAssembler(
        repository: LibraryRepository, classRepo: ClassFeatureRepository, calc: DndCalculator, coreStat: CoreStatAssembler,
        skillAssembler: SkillRegistryAssembler, featureAssembler: FeatureRegistryAssembler, resolveGlobalLoreUseCase: ResolveGlobalLoreUseCase, vitalsAssembler: VitalsAssembler,
        magicAssembler: MagicRegistryAssembler, resourceAssembler: ResourceRegistryAssembler, reconciler: InventoryReconciler,
        monsters: MonstersDataSource, ares: AresAssembler, acCalc: CalculateArmorClassUseCase, modExtractor: InventoryModifierExtractor,
        weightCalc: CalculateWeightUseCase, transformationApplier: TransformationApplier, json: Json
    ): SnapshotAssembler {
        return SnapshotAssembler(
            repository, classRepo, calc, coreStat, skillAssembler, featureAssembler,
            resolveGlobalLoreUseCase,
            vitalsAssembler, magicAssembler, resourceAssembler, reconciler, monsters, ares,
            acCalc, modExtractor, weightCalc, transformationApplier, json
        )
    }

    @Provides @Singleton
    fun provideCalculateArmorClassUseCase(): CalculateArmorClassUseCase = CalculateArmorClassUseCase()
    @Provides @Singleton
    fun provideCalculateWeightUseCase(): CalculateWeightUseCase = CalculateWeightUseCase()
    @Provides @Singleton
    fun provideItemFabricator(calculator: DndCalculator, json: Json): ItemFabricator = ItemFabricator(calculator, json)
    @Provides @Singleton
    fun providePriceParser(): PriceParser = PriceParser()
    @Provides @Singleton
    fun providePriceCalculator(): PriceCalculator = PriceCalculator()
    @Provides @Singleton
    fun provideModifyInventoryUseCase(repo: CharacterRepository, unpack: UnpackItemUseCase, weight: CalculateWeightUseCase, price: PriceCalculator): ModifyInventoryUseCase = ModifyInventoryUseCase(repo, unpack, weight, price)
}

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelScopedModule {
    @Provides @ViewModelScoped
    fun provideMerchantManager(libraryRepository: LibraryRepository): MerchantManager = MerchantManager(libraryRepository)
}

@Module
@InstallIn(SingletonComponent::class)
object MagicBindingModule {
    @Provides @Singleton
    fun provideSpendSpellSlotUseCase(
        repo: CharacterRepository,
        concentrationProtocol: ConcentrationProtocol
    ) = SpendSpellSlotUseCase(repo, concentrationProtocol)
    @Provides @Singleton
    fun provideManagePreparedSpellsUseCase(repo: CharacterRepository) = ManagePreparedSpellsUseCase(repo)
    @Provides @Singleton
    fun provideRestorationUseCase(repo: CharacterRepository) = RestorationUseCase(repo)
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\di\AppModule.kt
