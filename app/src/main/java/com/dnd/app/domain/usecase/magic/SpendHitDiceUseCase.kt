// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\magic\SpendHitDiceUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.magic

import com.dnd.app.domain.calculator.DiceRoller
import com.dnd.app.domain.model.snapshot.CharacterSnapshot
import com.dnd.app.domain.repository.CharacterRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SpendHitDiceUseCase @Inject constructor(
    private val repository: CharacterRepository,
    private val diceRoller: DiceRoller
) {

    private val supportedDieTypes = listOf(6, 8, 10, 12)

    suspend operator fun invoke(characterId: Long, dieType: Int): Result<Int> {
        if (dieType !in supportedDieTypes) {
            return Result.failure(Exception("Тип кости d$dieType не поддерживается"))
        }

        return repository.performAtomicMutation(characterId) { snapshot, liveState, _ ->
            val availableDice = snapshot.hitDiceCount - liveState.hitDiceSpent
            if (availableDice <= 0) {
                return@performAtomicMutation Result.failure(Exception("Костей хитов больше не осталось"))
            }

            val conModifier = resolveConModifier(snapshot)

            val rollDetails = diceRoller.rollComplex("1d$dieType")
            val healed = max(rollDetails.total + conModifier, 1)
            val updatedHp = (liveState.hpCurrent + healed).coerceAtMost(snapshot.maxHp)

            var nextLive = liveState.copy(
                hpCurrent = updatedHp,
                hitDiceSpent = liveState.hitDiceSpent + 1
            )
            val entry = "[${currentTime()}] Потрачена кость d$dieType, восстановлено $healed HP"
            val nextLogs = (liveState.systemLogs + entry).takeLast(10)
            nextLive = nextLive.copy(systemLogs = nextLogs)

            Result.success(nextLive to healed)
        }
    }

    private fun resolveConModifier(snapshot: CharacterSnapshot): Int {
        return snapshot.statsMap["CON"]?.modifier?.toIntOrNull()
            ?: snapshot.stats.firstOrNull { it.code == "CON" }?.modifier?.toIntOrNull()
            ?: 0
    }

    private fun currentTime(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
// --- КОНЕЦ ФАЙЛА ---
