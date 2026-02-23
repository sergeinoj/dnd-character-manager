// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\FeatDataSource.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.domain.model.Feature


interface FeatDataSource {

    suspend fun getFeatureByIndex(index: String): Feature?


    suspend fun getFeatureById(id: Int): Feature?


    suspend fun getFeaturesByIndexes(indexes: List<String>): List<Feature>


    suspend fun getAllFeats(): List<Feature>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\FeatDataSource.kt