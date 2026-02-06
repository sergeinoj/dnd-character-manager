// Имя файла: app/src/main/java/com/dnd/app/di/AppModule.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.di

import com.dnd.app.data.repository.CharacterRepositoryImpl
import com.dnd.app.data.repository.LibraryRepositoryImpl
import com.dnd.app.data.repository.datasource.*
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
    fun provideRaceDataSource(impl: RaceDataSourceImpl): com.dnd.app.data.repository.datasource.RaceDataSource {
        return impl
    }

    @Provides
    @Singleton
    fun provideOldClassDataSource(impl: OldClassDataSourceImpl): OldClassDataSource {
        return impl
    }

    @Provides
    @Singleton
    fun provideClassFeatureRepository(impl: ClassDataSourceImpl): ClassFeatureRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideDictionaryDataSource(impl: DictionaryDataSourceImpl): DictionaryDataSource {
        return impl
    }

    @Provides
    @Singleton
    fun provideSpellDataSource(impl: SpellDataSourceImpl): SpellDataSource {
        return impl
    }

    @Provides
    @Singleton
    fun provideFeatDataSource(impl: FeatDataSourceImpl): FeatDataSource {
        return impl
    }

    @Provides
    @Singleton
    fun provideLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository {
        return impl
    }

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
    fun providePartitionClassFeaturesUseCase(): PartitionClassFeaturesUseCase {
        return PartitionClassFeaturesUseCase()
    }

    @Provides
    @Singleton
    fun provideFeatResolver(): FeatResolver {
        return FeatResolver()
    }

    @Provides
    @Singleton
    fun provideFeatUiModelExtractor(): FeatUiModelExtractor {
        return FeatUiModelExtractor()
    }

    @Provides
    @Singleton
    fun provideDraftStatsUseCase(repository: LibraryRepository): DraftStatsUseCase {
        return DraftStatsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSpellChoiceAggregatorUseCase(): SpellChoiceAggregatorUseCase {
        return SpellChoiceAggregatorUseCase()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/di/AppModule.kt