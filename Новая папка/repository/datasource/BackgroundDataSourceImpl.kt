// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/BackgroundDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.BackgroundEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao
) : BackgroundDataSource {
    override suspend fun loadAllBackgroundEntities(): List<BackgroundEntity> {
        return dao.getAllBackgrounds()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/BackgroundDataSourceImpl.kt