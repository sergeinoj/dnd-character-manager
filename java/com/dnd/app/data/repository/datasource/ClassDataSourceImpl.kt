// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/ClassDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация "тупого" источника данных для классов.
 * Каждый метод является простым делегатом для вызова соответствующего метода в ReferenceDao.
 * Этот класс не содержит бизнес-логики, парсинга JSON или преобразования в доменные модели.
 * [ИЗМЕНЕНО] Реализует интерфейс ClassFeatureRepository.
 */
@Singleton
class ClassDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao
) : ClassFeatureRepository {

    override suspend fun getClassEntity(index: String): ClassEntity? {
        return dao.getClassByIndex(index)
    }

    override suspend fun getSubclassEntity(index: String): SubclassEntity? {
        // [FIX v1.26.2] Заменен неэффективный поиск на прямой запрос к БД.
        return dao.getSubclassByIndex(index)
    }

    override suspend fun getProgressionForLevel(classIndex: String, level: Int): List<ProgressionEntity> {
        return dao.getProgressionForLevel(classIndex, level)
    }

    override suspend fun getProgressionForLevels(classIndex: String, levels: List<Int>): List<ProgressionEntity> {
        return dao.getProgressionForLevels(classIndex, levels)
    }

    override suspend fun getFeaturesByIndexes(indexes: List<String>): List<FeatureEntity> {
        return dao.getFeaturesByIndexes(indexes)
    }

    override suspend fun findFeaturesByContext(
        classIndex: String?,
        subclassIndex: String?,
        level: Int
    ): List<FeatureEntity> {
        return dao.findFeaturesByContext(classIdx = classIndex, subclassIdx = subclassIndex)
            .filter { it.level == level }
    }

    override suspend fun getDamageTypeByIndex(index: String): DamageTypeEntity? {
        return dao.getDamageTypeByIndex(index)
    }

    override suspend fun getEquipmentByCategory(categoryIndex: String): List<EquipmentEntity> {
        // [ИСПРАВЛЕНИЕ v1.26.1] Рекурсивный поиск по категориям
        val allCategoryIndexes = mutableSetOf(categoryIndex)
        allCategoryIndexes.addAll(getAllChildCategoryIndexesRecursive(categoryIndex))

        val allItemIndexes = allCategoryIndexes.flatMap { dao.getLinksForCategory(it) }.distinct()

        return if (allItemIndexes.isNotEmpty()) {
            dao.getEquipmentByIndexes(allItemIndexes)
        } else {
            // Fallback для старых категорий без связей
            dao.getEquipmentByCategory(categoryIndex)
        }
    }

    override suspend fun getEquipmentCategoryByIndex(index: String): EquipmentCategoryEntity? {
        return dao.getEquipmentCategoryByIndex(index)
    }

    override suspend fun getAllSkills(): List<SkillEntity> {
        return dao.getAllSkills()
    }

    override suspend fun getAllLanguages(): List<LanguageEntity> {
        return dao.getAllLanguages()
    }

    override suspend fun getAllFeats(): List<FeatureEntity> {
        return dao.getAllFeats()
    }

    // [НОВЫЕ МЕТОДЫ v1.26]
    override suspend fun getLinksForCategory(categoryIndex: String): List<String> {
        return dao.getLinksForCategory(categoryIndex)
    }

    override suspend fun getWeaponsByIndexes(indexes: List<String>): List<WeaponEntity> {
        return dao.getWeaponsByIndexes(indexes)
    }

    override suspend fun getArmorByIndexes(indexes: List<String>): List<ArmorEntity> {
        return dao.getArmorByIndexes(indexes)
    }

    override suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity> {
        return dao.getEquipmentByIndexes(indexes)
    }

    // [НОВЫЙ МЕТОД v1.26.2]
    override suspend fun getAllItemIndexesByCategoryRecursive(categoryIndex: String): List<String> {
        val allCategoryIndexes = mutableSetOf(categoryIndex)
        allCategoryIndexes.addAll(getAllChildCategoryIndexesRecursive(categoryIndex))
        return allCategoryIndexes.flatMap { dao.getLinksForCategory(it) }.distinct()
    }


    /**
     * [НОВЫЙ МЕТОД v1.26.1] Рекурсивно собирает индексы всех дочерних категорий.
     */
    private suspend fun getAllChildCategoryIndexesRecursive(parentIndex: String): Set<String> {
        val result = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(parentIndex)

        while (queue.isNotEmpty()) {
            val currentParent = queue.removeFirst()
            val children = dao.getChildEquipmentCategories(currentParent)
            for (child in children) {
                if (result.add(child.indexName)) {
                    queue.add(child.indexName)
                }
            }
        }
        return result
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/ClassDataSourceImpl.kt