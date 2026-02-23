// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\MonstersDataSource.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.domain.model.MonsterRecord

interface MonstersDataSource {
    suspend fun getMonster(index: String): MonsterRecord?
    suspend fun getMonsters(indexes: List<String>): List<MonsterRecord>
    suspend fun listMonsters(): List<MonsterRecord>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\datasource\MonstersDataSource.kt
