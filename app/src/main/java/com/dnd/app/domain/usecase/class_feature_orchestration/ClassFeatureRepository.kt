// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\ClassFeatureRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import com.dnd.app.data.local.entity.*


interface ClassFeatureRepository {

    suspend fun getAllClassesEntities(): List<ClassEntity>
    suspend fun getSubclassesForClassEntity(classIndex: String): List<SubclassEntity>


    suspend fun getClassEntity(index: String): ClassEntity?
    suspend fun getSubclassEntity(index: String): SubclassEntity?


    suspend fun getProgressionForLevel(classIndex: String, level: Int): List<ProgressionEntity>
    suspend fun getProgressionForLevels(classIndex: String, levels: List<Int>): List<ProgressionEntity>
    suspend fun getProgressionForLevelAndSubclass(classIndex: String, level: Int, subclassIndex: String?): List<ProgressionEntity>
    suspend fun getFeaturesByIndexes(indexes: List<String>): List<FeatureEntity>
    suspend fun getFeatureById(id: Int): FeatureEntity?
    suspend fun getFeatureByIndex(index: String): FeatureEntity?
    suspend fun findFeaturesByContext(classIndex: String? = null, subclassIndex: String? = null, level: Int): List<FeatureEntity>


    suspend fun getDamageTypeByIndex(index: String): DamageTypeEntity?
    suspend fun getEquipmentByCategory(categoryIndex: String): List<EquipmentEntity>
    suspend fun getEquipmentCategoryByIndex(index: String): EquipmentCategoryEntity?
    suspend fun getAllSkills(): List<SkillEntity>
    suspend fun getAllProficiencies(): List<ProficiencyEntity>
    suspend fun getAllLanguages(): List<LanguageEntity>
    suspend fun getAllFeats(): List<FeatureEntity>


    suspend fun getLinksForCategory(categoryIndex: String): List<String>
    suspend fun getWeaponsByIndexes(indexes: List<String>): List<WeaponEntity>
    suspend fun getArmorByIndexes(indexes: List<String>): List<ArmorEntity>
    suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity>

    suspend fun getWeaponsByCategory(categoryIndex: String): List<WeaponEntity>
    suspend fun getArmorByCategory(categoryIndex: String): List<ArmorEntity>


    suspend fun getAllItemIndexesByCategoryRecursive(categoryIndex: String): List<String>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\ClassFeatureRepository.kt
