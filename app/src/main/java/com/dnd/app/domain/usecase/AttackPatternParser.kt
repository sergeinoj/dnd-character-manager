package com.dnd.app.domain.usecase

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.domain.model.monster.AttackPattern
import com.dnd.app.domain.model.monster.AttackPatternEntry
import com.dnd.app.domain.model.monster.AttackPatternEntryType
import com.dnd.app.domain.model.monster.PatternLogic
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttackPatternParser @Inject constructor(
    private val dao: ReferenceDao
) {

    suspend fun parseForMonster(monsterIndex: String): List<AttackPattern> {
        val patterns = dao.getMonsterAttackPatterns(monsterIndex)
        if (patterns.isEmpty()) return emptyList()

        return patterns.mapNotNull { pattern ->
            val patternId = pattern.id ?: return@mapNotNull null
            val entries = dao.getAttackPatternEntries(patternId)
            AttackPattern(
                slug = pattern.patternSlug,
                logic = PatternLogic.from(pattern.logicOperator),
                description = pattern.description,
                entries = entries.map { entry ->
                    AttackPatternEntry(
                        type = AttackPatternEntryType.from(entry.entryType),
                        reference = entry.entryIndex,
                        count = entry.count ?: 1
                    )
                }
            )
        }
    }
}
