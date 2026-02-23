// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\SkillRegistryAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.data.local.entity.SkillEntity
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.snapshot.ProficiencyType
import com.dnd.app.domain.model.snapshot.SkillModel
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SkillRegistryAssembler @Inject constructor(
    private val calculator: DndCalculator
) {

    fun assemble(
        allSkills: List<SkillEntity>,
        statRegistry: StatRegistry,
        profBonus: Int,
        pickedProficiencies: Map<String, Int>,
        modifierRegistry: ModifierRegistry = ModifierRegistry()
    ): List<SkillModel> {
        return allSkills.map { skillEntity ->
            val statCode = skillEntity.abilityScoreIndex?.uppercase() ?: "INT"
            val score = statRegistry.scores[statCode] ?: 10
            val proficiencyLevel = pickedProficiencies[skillEntity.indexName] ?: 0


            val baseBonus = calculator.calculateSkillBonus(
                score = score,
                profBonus = profBonus,
                multiplier = proficiencyLevel
            )


            val itemSkillBonus = modifierRegistry.skillBonuses[skillEntity.indexName] ?: 0
            val finalTotal = baseBonus + itemSkillBonus

            SkillModel(
                code = skillEntity.indexName,
                name = skillEntity.name,
                modifier = calculator.formatModifier(finalTotal),
                statCode = statCode,
                profType = when (proficiencyLevel) {
                    1 -> ProficiencyType.PROFICIENCY
                    2 -> ProficiencyType.EXPERTISE
                    else -> ProficiencyType.NONE
                }
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\SkillRegistryAssembler.kt