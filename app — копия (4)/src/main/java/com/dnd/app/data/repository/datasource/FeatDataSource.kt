// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/FeatDataSource.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.domain.model.Feature

/**
 * Изолированный источник данных для получения и парсинга способностей (Features/Feats).
 * Является единственным источником истины для преобразования FeatureEntity в доменную модель Feature.
 */
interface FeatDataSource {
    /**
     * Получает и преобразует одну способность по ее уникальному строковому индексу.
     * Возвращает null, если способность не является общей чертой (не начинается с 'feat-').
     */
    suspend fun getFeatureByIndex(index: String): Feature?

    /**
     * Получает и преобразует одну способность по ее ID.
     * Возвращает null, если способность не является общей чертой (не начинается с 'feat-').
     */
    suspend fun getFeatureById(id: Int): Feature?

    /**
     * Получает и преобразует список способностей по их индексам.
     * Возвращает только те способности, которые являются общими чертами.
     */
    suspend fun getFeaturesByIndexes(indexes: List<String>): List<Feature>

    /**
     * Получает все способности, которые являются "чертами" (feats).
     */
    suspend fun getAllFeats(): List<Feature>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/FeatDataSource.kt