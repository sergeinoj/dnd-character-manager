// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\di\DatabaseModule.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.di

import android.content.Context
import androidx.room.Room
import com.dnd.app.data.local.AppDatabase
import com.dnd.app.data.local.ReferenceDatabase
import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.dao.ReferenceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val REFERENCE_DB_NAME = "dnd_v5.db"
    private const val ASSET_DB_PATH = "database/dnd_clean.db"
    private const val USER_DB_NAME = "dnd_app_user.db"

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            USER_DB_NAME
        )
            .addMigrations(
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )

            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideReferenceDatabase(@ApplicationContext context: Context): ReferenceDatabase {
        return Room.databaseBuilder(
            context,
            ReferenceDatabase::class.java,
            REFERENCE_DB_NAME
        )
            .createFromAsset(ASSET_DB_PATH)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideReferenceDao(db: ReferenceDatabase): ReferenceDao {
        return db.referenceDao()
    }

    @Provides
    fun provideCharacterDao(db: AppDatabase): CharacterDao {
        return db.characterDao()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\di\DatabaseModule.kt
