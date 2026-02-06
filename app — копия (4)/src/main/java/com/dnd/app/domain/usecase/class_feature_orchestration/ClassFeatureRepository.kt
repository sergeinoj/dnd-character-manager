// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/ClassFeatureRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import com.dnd.app.data.local.entity.*

/**
 * [ПЕРЕИМЕНОВАНО] "Тупой" интерфейс доступа к данным, связанным с классами.
 * Ответственность: Только чтение сырых Entity из базы данных без какой-либо логики парсинга или преобразования.
 * Прежнее имя ClassDataSource вызывало конфликт с другим классом.
 */
interface ClassFeatureRepository {
    suspend fun getClassEntity(index: String): ClassEntity?
    suspend fun getSubclassEntity(index: String): SubclassEntity?
    suspend fun getProgressionForLevel(classIndex: String, level: Int): List<ProgressionEntity>
    suspend fun getProgressionForLevels(classIndex: String, levels: List<Int>): List<ProgressionEntity>
    suspend fun getFeaturesByIndexes(indexes: List<String>): List<FeatureEntity>
    suspend fun findFeaturesByContext(classIndex: String? = null, subclassIndex: String? = null, level: Int): List<FeatureEntity>
    suspend fun getDamageTypeByIndex(index: String): DamageTypeEntity?
    suspend fun getEquipmentByCategory(categoryIndex: String): List<EquipmentEntity>
    suspend fun getEquipmentCategoryByIndex(index: String): EquipmentCategoryEntity?
    suspend fun getAllSkills(): List<SkillEntity>
    suspend fun getAllLanguages(): List<LanguageEntity>
    suspend fun getAllFeats(): List<FeatureEntity>

    // [НОВЫЕ МЕТОДЫ v1.26]
    suspend fun getLinksForCategory(categoryIndex: String): List<String>
    suspend fun getWeaponsByIndexes(indexes: List<String>): List<WeaponEntity>
    suspend fun getArmorByIndexes(indexes: List<String>): List<ArmorEntity>
    suspend fun getEquipmentByIndexes(indexes: List<String>): List<EquipmentEntity>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/class_feature_orchestration/ClassFeatureRepository.kt