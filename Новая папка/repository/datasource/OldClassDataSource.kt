// Имя файла: data/repository/datasource/OldClassDataSource.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.domain.model.ClassInfo
import com.dnd.app.domain.model.SubclassInfo

/**
 * [ПЕРЕИМЕНОВАНО В OldClassDataSource]
 * Этот интерфейс теперь используется только для получения простых списков классов/подклассов.
 * Основная логика получения способностей вынесена в GetFeaturesForLevelUseCase.
 */
interface OldClassDataSource {
    suspend fun getAllClasses(): List<ClassInfo>
    suspend fun getSubclassesForClass(classIndex: String): List<SubclassInfo>
    suspend fun getClassEntityByIndex(index: String): ClassEntity?
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: data/repository/datasource/OldClassDataSource.kt