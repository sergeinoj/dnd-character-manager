// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\CoreStatAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.DndConstants
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.snapshot.StatModel
import javax.inject.Inject
import javax.inject.Singleton


data class StatRegistry(
    val scores: Map<String, Int>,
    val modifiers: Map<String, Int>,
    val models: Map<String, StatModel>
)


@Singleton
class CoreStatAssembler @Inject constructor(
    private val calculator: DndCalculator
) {


    suspend fun assemble(
        draft: DraftCharacter,
        modifierRegistry: ModifierRegistry = ModifierRegistry(),
        overrides: Map<String, Int> = emptyMap()
    ): StatRegistry {

        val profs = draft.getAllProficienciesWithLevels()
        val totalLevel = draft.levelStack.size.coerceAtLeast(1)


        val baseProfBonus = calculator.calculateProficiencyBonus(totalLevel)
        val finalProfBonus = baseProfBonus + modifierRegistry.profBonusMod

        val statCodes = listOf("STR", "DEX", "CON", "INT", "WIS", "CHA")

        val scoresMap = mutableMapOf<String, Int>()
        val modifiersMap = mutableMapOf<String, Int>()
        val modelsMap = mutableMapOf<String, StatModel>()

        statCodes.forEach { code ->

            val base = draft.baseInfo.baseAbilityScores[code] ?: 10
            val draftBonus = draft.baseInfo.aggregateStatBonuses[code] ?: 0
            val itemBonus = modifierRegistry.statBonuses[code] ?: 0

            var calculatedScore = overrides[code] ?: (base + draftBonus + itemBonus)



            modifierRegistry.statOverrides[code]?.let { overrideValue ->
                if (overrideValue > calculatedScore) {
                    calculatedScore = overrideValue
                }
            }


            val modValue = calculator.calculateModifier(calculatedScore)


            val saveKey = "${DndConstants.VirtualKeys.SAVING_THROW_PREFIX}${code.lowercase()}"
            val isProficientSave = profs.containsKey(saveKey)

            val totalSaveBonus = modValue +
                    (if (isProficientSave) finalProfBonus else 0) +
                    modifierRegistry.saveBonus


            scoresMap[code] = calculatedScore
            modifiersMap[code] = modValue
            modelsMap[code] = StatModel(
                code = code,
                value = calculatedScore,
                modifier = calculator.formatModifier(modValue),
                saveModifier = calculator.formatModifier(totalSaveBonus),
                isProficientSave = isProficientSave
            )
        }

        return StatRegistry(
            scores = scoresMap,
            modifiers = modifiersMap,
            models = modelsMap
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\CoreStatAssembler.kt