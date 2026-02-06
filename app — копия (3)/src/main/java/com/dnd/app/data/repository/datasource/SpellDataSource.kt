// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/SpellDataSource.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.datasource

import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.model.Spell
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

interface SpellDataSource {
    suspend fun getGrantedSpells(spellShowJson: String?): List<Spell>
    suspend fun parseSpellChoice(choiceJson: JsonObject): FeatureChoiceDomain.SelectSpell
    fun getAllSpells(): Flow<List<Spell>>
    suspend fun getSpellsByIds(ids: List<Int>): List<Spell>
    suspend fun getSpellsByLevelAndClass(level: Int, classIndex: String): List<Spell>
    suspend fun getAllSpellsByClass(classIndex: String): List<Spell>

    /**
     * [НОВЫЙ МЕТОД]
     * Парсит сложные, многоуровневые выборы заклинаний, характерные для черт.
     * Например, "выберите класс, затем выберите заговоры из списка этого класса".
     * Возвращает общий FeatureChoiceDomain, так как верхний уровень может быть не выбором заклинания.
     */
    suspend fun parseFeatSpellChoice(choiceJson: JsonObject): FeatureChoiceDomain
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/datasource/SpellDataSource.kt