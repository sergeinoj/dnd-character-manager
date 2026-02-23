package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.domain.model.snapshot.CharacterLiveState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DamageProcessor @Inject constructor() {

    data class Outcome(
        val liveState: CharacterLiveState,
        val overflow: Int,
        val reverted: Boolean
    )

    fun processDamage(
        liveState: CharacterLiveState,
        damage: Int,
        sourceLabel: String? = null
    ): Outcome {
        if (damage <= 0) return Outcome(liveState, overflow = 0, reverted = false)
        val logLine = sourceLabel?.let { "Damage from $it" } ?: "Damage received"
        var builder = liveState.systemLogs.toMutableList()
        var current = liveState
        var remainder = damage
        var overflow = 0
        var reverted = false

        if (!current.transformationId.isNullOrBlank() && current.transformationHp > 0) {
            val transformHp = current.transformationHp
            val left = transformHp - remainder
            if (left > 0) {
                current = current.copy(transformationHp = left)
                remainder = 0
                builder.add("$logLine -> absorbed $damage in transformation")
            } else {
                overflow = (-left).coerceAtLeast(0)
                remainder = overflow
                builder.add("$logLine -> transformation ended with overflow $overflow")
                current = current.copy(
                    transformationId = null,
                    transformationHp = 0
                )
                reverted = true
            }
        }

        if (remainder > 0) {
            val newHp = (current.hpCurrent - remainder).coerceAtLeast(0)
            current = current.copy(hpCurrent = newHp)
            builder.add("$logLine -> applied $remainder to base HP")
        }

        return Outcome(
            liveState = current.copy(systemLogs = builder.toList()),
            overflow = overflow,
            reverted = reverted
        )
    }
}
