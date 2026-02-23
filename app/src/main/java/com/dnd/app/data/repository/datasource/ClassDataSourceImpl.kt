// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\ClassDataSourceImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*
import com.dnd.app.domain.usecase.class_feature_orchestration.ClassFeatureRepository
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ClassDataSourceImpl @Inject constructor(
    private val dao: ReferenceDao
) : ClassFeatureRepository {

    override suspend fun getAllClassesEntities(): List<ClassEntity> {
        return dao.getAllClasses()
    }

    override suspend fun getSubclassesForClassEntity(classIndex: String): List<SubclassEntity> {
        return dao.getSubclassesForClass(classIndex)
    }

    override suspend fun getClassEntity(index: String): ClassEntity? {
        return dao.getClassByIndex(index)
    }

    override suspend fun getSubclassEntity(index: String): SubclassEntity? {
        return dao.getSubclassByIndex(index)
    }

    override suspend fun getProgressionForLevel(classIndex: String, level: Int): List<ProgressionEntity> {
        return dao.getProgressionForLevel(classIndex, level)
    }

    override suspend fun getProgressionForLevels(classIndex: String, levels: List<Int>): List<ProgressionEntity> {
        return dao.getProgressionForLevels(classIndex, levels)
    }

    override suspend fun getProgressionForLevelAndSubclass(classIndex: String, level: Int, subclassIndex: String?): List<ProgressionEntity> {
        return dao.getProgressionForLevelAndSubclass(classIndex, level, subclassIndex)
    }

    override suspend fun getFeaturesByIndexes(indexes: List<String>): List<FeatureEntity> {
        return dao.getFeaturesByIndexes(indexes)
    }

    override suspend fun getFeatureById(id: Int): FeatureEntity? {
        return dao.getFeatureById(id)
    }

    override suspend fun getFeatureByIndex(index: String): FeatureEntity? {
        return dao.getFeatureByIndex(index)
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
        val allItemIndexes = getAllItemIndexesByCategoryRecursive(categoryIndex)
        return if (allItemIndexes.isNotEmpty()) {
            dao.getEquipmentByIndexes(allItemIndexes)
        } else {
            dao.getEquipmentByCategory(categoryIndex)
        }
    }

    override suspend fun getEquipmentCategoryByIndex(index: String): EquipmentCategoryEntity? {
        return dao.getEquipmentCategoryByIndex(index)
    }

    override suspend fun getAllSkills(): List<SkillEntity> {
        return dao.getAllSkills()
    }

    override suspend fun getAllProficiencies(): List<ProficiencyEntity> {
        return dao.getAllProficiencies()
    }

    override suspend fun getAllLanguages(): List<LanguageEntity> {
        return dao.getAllLanguages()
    }

    override suspend fun getAllFeats(): List<FeatureEntity> {
        return dao.getAllFeats()
    }

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

    override suspend fun getWeaponsByCategory(categoryIndex: String): List<WeaponEntity> {
        return dao.getWeaponsByCategory(categoryIndex)
    }

    override suspend fun getArmorByCategory(categoryIndex: String): List<ArmorEntity> {
        return dao.getArmorByCategory(categoryIndex)
    }

    override suspend fun getAllItemIndexesByCategoryRecursive(categoryIndex: String): List<String> {
        return dao.getAllItemIndexesByCategoryRecursive(categoryIndex)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\ClassDataSourceImpl.kt
