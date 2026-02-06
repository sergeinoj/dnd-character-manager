// Имя файла: di/AppModule.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.di

import com.dnd.app.data.repository.CharacterRepositoryImpl
import com.dnd.app.data.repository.LibraryRepositoryImpl
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
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
    fun provideLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository {
        return impl
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: di/AppModule.kt