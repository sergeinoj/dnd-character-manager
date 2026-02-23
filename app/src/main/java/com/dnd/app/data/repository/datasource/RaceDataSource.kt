// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\RaceDataSource.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.Race
import com.dnd.app.domain.model.RaceFullData

interface RaceDataSource {
    suspend fun getAllParentRaces(): List<Race>
    suspend fun getSubracesFromDb(parentId: Int): List<Race>
    suspend fun getBaseRaceFeatures(raceId: Int): List<Feature>
    suspend fun getSubraceFeatures(subraceIndex: String): List<Feature>
    suspend fun getRaceByIndex(index: String): Race?
    suspend fun getRaceFullData(index: String): RaceFullData?
    suspend fun getSubraceModelByIndex(index: String): Race?
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\RaceDataSource.kt