// Имя файла: app/src/main/java/com/dnd/app/di/AppModule.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.di

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.repository.*
import com.dnd.app.data.repository.datasource.*
import com.dnd.app.domain.repository.BackgroundRepository
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.*
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassProgressionUseCase
import com.dnd.app.domain.usecase.class_feature_orchestration.FeatureEnricher
import com.dnd.app.domain.usecase.class_feature_orchestration.FeatureFactory
import com.dnd.app.domain.usecase.class_feature_orchestration.GetFeaturesForLevelUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRaceDataSource(impl: RaceDataSourceImpl): RaceDataSource = impl

    @Provides
    @Singleton
    fun provideOldClassDataSource(impl: OldClassDataSourceImpl): OldClassDataSource = impl

    @Provides
    @Singleton
    fun provideClassFeatureRepository(impl: ClassDataSourceImpl): ClassFeatureRepository = impl

    @Provides
    @Singleton
    fun provideDictionaryDataSource(impl: DictionaryDataSourceImpl): DictionaryDataSource = impl

    @Provides
    @Singleton
    fun provideSpellDataSource(impl: SpellDataSourceImpl): SpellDataSource = impl

    @Provides
    @Singleton
    fun provideFeatDataSource(impl: FeatDataSourceImpl): FeatDataSource = impl

    @Provides
    @Singleton
    fun provideBackgroundDataSource(impl: BackgroundDataSourceImpl): BackgroundDataSource = impl

    @Provides
    @Singleton
    fun provideBackgroundRepository(impl: BackgroundRepositoryImpl): BackgroundRepository = impl

    @Provides
    @Singleton
    fun provideLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository = impl

    @Provides
    @Singleton
    fun provideCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository = impl

    @Provides
    @Singleton
    fun provideFeatureFactory(
        classFeatureRepository: ClassFeatureRepository,
        spellDataSource: SpellDataSource
    ): FeatureFactory {
        return FeatureFactory(classFeatureRepository, spellDataSource)
    }

    @Provides
    @Singleton
    fun provideClassProgressionUseCase(
        classFeatureRepository: ClassFeatureRepository,
        spellDataSource: SpellDataSource,
        featureFactory: FeatureFactory
    ): ClassProgressionUseCase {
        return ClassProgressionUseCase(classFeatureRepository, spellDataSource, featureFactory)
    }

    @Provides
    @Singleton
    fun provideFeatureEnricher(classFeatureRepository: ClassFeatureRepository): FeatureEnricher {
        return FeatureEnricher(classFeatureRepository)
    }

    @Provides
    @Singleton
    fun provideGetFeaturesForLevelUseCase(
        dataSource: ClassFeatureRepository,
        featureFactory: FeatureFactory,
        progressionUseCase: ClassProgressionUseCase,
        enricher: FeatureEnricher
    ): GetFeaturesForLevelUseCase {
        return GetFeaturesForLevelUseCase(dataSource, featureFactory, progressionUseCase, enricher)
    }

    @Provides
    @Singleton
    fun providePartitionClassFeaturesUseCase(): PartitionClassFeaturesUseCase = PartitionClassFeaturesUseCase()

    @Provides
    @Singleton
    fun provideFeatResolver(): FeatResolver = FeatResolver()

    @Provides
    @Singleton
    fun provideFeatUiModelExtractor(): FeatUiModelExtractor = FeatUiModelExtractor()

    @Provides
    @Singleton
    fun provideDraftStatsUseCase(repository: LibraryRepository): DraftStatsUseCase = DraftStatsUseCase(repository)

    @Provides
    @Singleton
    fun provideSpellChoiceAggregatorUseCase(): SpellChoiceAggregatorUseCase = SpellChoiceAggregatorUseCase()

    @Provides
    @Singleton
    fun provideUpdateStatUseCase(draftStatsUseCase: DraftStatsUseCase): UpdateStatUseCase {
        return UpdateStatUseCase(draftStatsUseCase)
    }

    @Provides
    @Singleton
    fun provideBakeCharacterUseCase(
        libraryRepository: LibraryRepository,
        draftStatsUseCase: DraftStatsUseCase
    ): BakeCharacterUseCase {
        return BakeCharacterUseCase(libraryRepository, draftStatsUseCase)
    }

    @Provides
    @Singleton
    fun provideBackgroundOrchestrator(
        referenceDao: ReferenceDao
    ): BackgroundOrchestrator {
        return BackgroundOrchestrator(referenceDao)
    }

    @Provides
    @Singleton
    fun provideGetClassProgressionDataUseCase(
        getFeaturesForLevelUseCase: GetFeaturesForLevelUseCase,
        partitionUseCase: PartitionClassFeaturesUseCase,
        spellAggregatorUseCase: SpellChoiceAggregatorUseCase,
        libraryRepository: LibraryRepository
    ): GetClassProgressionDataUseCase {
        return GetClassProgressionDataUseCase(
            getFeaturesForLevelUseCase,
            partitionUseCase,
            spellAggregatorUseCase,
            libraryRepository
        )
    }

    // [НОВЫЙ ПРОВАЙДЕР - ЭТАП 6]
    @Provides
    @Singleton
    fun provideHandleSelectionUseCase(): HandleSelectionUseCase {
        return HandleSelectionUseCase()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/di/AppModule.kt