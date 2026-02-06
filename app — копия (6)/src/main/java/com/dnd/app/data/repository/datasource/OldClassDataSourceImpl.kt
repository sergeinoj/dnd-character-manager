// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/OldClassDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.ClassEntity
import com.dnd.app.domain.model.ClassInfo
import com.dnd.app.domain.model.SubclassInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ПЕРЕИМЕНОВАНО В OldClassDataSourceImpl]
 * Упрощенная реализация, которая теперь отвечает только за получение
 * базовой информации о классах и подклассах для отображения в списках выбора.
 * Вся сложная логика парсинга и генерации способностей удалена.
 */
@Singleton
class OldClassDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao
) : OldClassDataSource {

    override suspend fun getAllClasses(): List<ClassInfo> {
        return dao.getAllClasses().map { entity ->
            val subclasses = dao.getSubclassesForClass(entity.indexName).map { sub ->
                SubclassInfo(
                    index = sub.indexName,
                    name = sub.name,
                    flavor = sub.subclassFlavor ?: "",
                    description = sub.desc ?: ""
                )
            }
            ClassInfo(
                id = entity.id ?: 0,
                index = entity.indexName,
                name = entity.name,
                hitDie = entity.hitDie ?: 8,
                subclasses = subclasses
            )
        }
    }

    override suspend fun getSubclassesForClass(classIndex: String): List<SubclassInfo> {
        return dao.getSubclassesForClass(classIndex).map { entity ->
            SubclassInfo(
                index = entity.indexName,
                name = entity.name,
                flavor = entity.subclassFlavor ?: "",
                description = entity.desc ?: ""
            )
        }
    }

    override suspend fun getClassEntityByIndex(index: String): ClassEntity? {
        return dao.getClassByIndex(index)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/OldClassDataSourceImpl.kt