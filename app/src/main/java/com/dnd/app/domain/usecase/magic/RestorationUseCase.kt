// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\magic\RestorationUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.magic

import com.dnd.app.domain.model.snapshot.DeathSavesState
import com.dnd.app.domain.model.snapshot.ResetRule
import com.dnd.app.domain.repository.CharacterRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class RestorationUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke(characterId: Long, isLongRest: Boolean): Result<Unit> {
        return repository.performAtomicMutation(characterId) { snapshot, liveState, _ ->

            val isShortRest = !isLongRest
            val hasHybridMagic = snapshot.magic?.hasHybridMagic == true
            val pactMagicActive = snapshot.magic?.pactMagic != null
            val shouldResetPactSlots = pactMagicActive && (isShortRest || isLongRest)

            val activePools = snapshot.resourcePools
            val activePoolIds = activePools.map { it.id }.toSet()

            val cleanCharges = liveState.featureCharges.filterKeys { it in activePoolIds }.toMutableMap()

            fun shouldReset(rule: ResetRule): Boolean = when (rule) {
                ResetRule.SHORT_REST -> isShortRest || isLongRest
                ResetRule.LONG_REST -> isLongRest
                ResetRule.DAWN -> false
                ResetRule.NEVER -> false
            }

            activePools.forEach { pool ->
                if (shouldReset(pool.resetRule)) {
                    cleanCharges[pool.id] = 0
                }
            }

            var nextLive = liveState.copy(
                featureCharges = cleanCharges,
                spentPactSlots = if (shouldResetPactSlots) 0 else liveState.spentPactSlots,
                innateUsage = liveState.innateUsage
            )

            if (isLongRest) {
                val recoveredDice = max(1, snapshot.hitDiceCount / 2)
                val nextExhaustion = (liveState.exhaustionLevel - 1).coerceAtLeast(0)
                nextLive = nextLive.copy(
                    hpCurrent = snapshot.maxHp,
                    hpTemp = 0,
                    innateUsage = emptyMap(),
                    spentGlobalSlots = if (hasHybridMagic) emptyMap() else liveState.spentGlobalSlots,
                    deathSaves = DeathSavesState(),
                    hitDiceSpent = (liveState.hitDiceSpent - recoveredDice).coerceAtLeast(0),
                    exhaustionLevel = nextExhaustion
                )
            }

            if (!isLongRest) {
                val entry = "[${currentTime()}] Выполнен короткий отдых"
                val nextLogs = (liveState.systemLogs + entry).takeLast(10)
                nextLive = nextLive.copy(systemLogs = nextLogs)
            }

            Result.success(nextLive to Unit)
        }
    }

    suspend fun performDawnReset(characterId: Long): Result<Unit> {
        return repository.performAtomicMutation(characterId) { snapshot, liveState, _ ->
            val activePools = snapshot.resourcePools
            val activePoolIds = activePools.map { it.id }.toSet()
            val cleanCharges = liveState.featureCharges.filterKeys { it in activePoolIds }.toMutableMap()

            activePools.filter { it.resetRule == ResetRule.DAWN }.forEach { pool ->
                cleanCharges[pool.id] = 0
            }

            val entry = "[${currentTime()}] Наступил рассвет"
            val nextLogs = (liveState.systemLogs + entry).takeLast(10)
            Result.success(liveState.copy(featureCharges = cleanCharges, systemLogs = nextLogs) to Unit)
        }
    }

    private fun currentTime(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\magic\RestorationUseCase.kt
