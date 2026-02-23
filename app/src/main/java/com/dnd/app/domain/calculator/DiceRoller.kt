// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\calculator\DiceRoller.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.calculator

import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random


@Singleton
class DiceRoller @Inject constructor() {

    private val segmentPattern: Pattern = Pattern.compile("([+-]?)\\s*([^+-]+)")
    private val dicePattern: Pattern = Pattern.compile("(\\d+)[dkк](\\d+)", Pattern.CASE_INSENSITIVE)

    data class RollResult(
        val total: Int,
        val details: String,
        val formula: String,
        val isCritical: Boolean
    )


    fun rollComplex(formula: String, isCritical: Boolean = false): RollResult {
        var total = 0
        val rollsDisplay = mutableListOf<String>()


        val cleanFormula = formula.replace(" ", "")
        if (cleanFormula.isBlank()) return RollResult(0, "0", formula, false)

        val matcher = segmentPattern.matcher(cleanFormula)

        while (matcher.find()) {
            val signChar = matcher.group(1) ?: ""
            val sign = if (signChar == "-") -1 else 1

            val content = matcher.group(2) ?: continue
            val diceMatcher = dicePattern.matcher(content)

            if (diceMatcher.matches()) {
                val countStr = diceMatcher.group(1) ?: "1"
                val sidesStr = diceMatcher.group(2) ?: "6"

                val count = countStr.toInt()
                val sides = sidesStr.toInt()

                val finalCount = if (isCritical) count * 2 else count
                val rolls = List(finalCount) { Random.nextInt(1, sides + 1) }
                val segmentSum = rolls.sum()

                total += sign * segmentSum
                rollsDisplay.add("${if (sign < 0) "-" else ""}[${rolls.joinToString(",")}]")
            } else {
                val constant = content.toIntOrNull() ?: 0
                total += sign * constant
                if (constant != 0) {
                    val prefix = if (sign < 0) "-" else "+"
                    rollsDisplay.add("$prefix$constant")
                }
            }
        }

        return RollResult(
            total = total,
            details = rollsDisplay.joinToString(" "),
            formula = formula,
            isCritical = isCritical
        )
    }

    fun rollD20(bonus: Int = 0): Pair<Int, Int> {
        val natural = Random.nextInt(1, 21)
        return natural to (natural + bonus)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\calculator\DiceRoller.kt