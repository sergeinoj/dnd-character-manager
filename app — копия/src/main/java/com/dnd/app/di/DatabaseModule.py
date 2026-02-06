// Имя файла: app/src/main/java/com/dnd/app/di/DatabaseModule.kt
// --- НАЧАО ФАЙЛА ---
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
import java.io.FileOutputStream
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val REFERENCE_DB_NAME = "dnd_reference.db"

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "dnd_app_user.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideReferenceDatabase(@ApplicationContext context: Context): ReferenceDatabase {
        // --- НАЧАЛО ЛОГИКИ РУЧНОГО КОПИРОВАНИЯ ---
        val dbFile = context.getDatabasePath(REFERENCE_DB_NAME)

        if (!dbFile.exists()) {
            try {
                val inputStream = context.assets.open("database/dnd_database.db")
                val outputStream = FileOutputStream(dbFile)

                inputStream.copyTo(outputStream)

                outputStream.flush()
                outputStream.close()
                inputStream.close()
            } catch (e: Exception) {
                // В случае ошибки, приложение, скорее всего, упадет при попытке
                // создать пустую БД, что укажет на проблему с копированием.
                throw RuntimeException("Failed to copy pre-packaged database", e)
            }
        }
        // --- КОНЕЦ ЛОГИКИ РУЧНОГО КОПИРОВАНИЯ ---

        return Room.databaseBuilder(
            context,
            ReferenceDatabase::class.java,
            REFERENCE_DB_NAME // Используем стабильное имя
        )
            // УБИРАЕМ .createFromAsset(), так как мы сами скопировали файл
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
// Имя файла: app/src/main/java/com/dnd/app/di/DatabaseModule.kt