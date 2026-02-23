package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.magic.SpellCastContext
import com.dnd.app.domain.model.snapshot.CharacterLiveState
import com.dnd.app.domain.model.snapshot.CharacterSnapshot
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConcentrationProtocol @Inject constructor() {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun applySpellCast(
        snapshot: CharacterSnapshot,
        liveState: CharacterLiveState,
        context: SpellCastContext
    ): CharacterLiveState {
        if (!context.isConcentration) return liveState
        val currentSpell = liveState.concentrationSpellId
        if (currentSpell == context.id) return liveState
        val now = timestamp()
        val entries = mutableListOf<String>()
        if (!currentSpell.isNullOrBlank()) {
            val previousName = findSpellName(snapshot, currentSpell) ?: currentSpell
            entries += "[$now] Сброшена концентрация: $previousName (новое ${context.name})"
        }
        entries += "[$now] Начата концентрация: ${context.name}"
        val mergedLogs = (liveState.systemLogs + entries).takeLast(10)
        return liveState.copy(concentrationSpellId = context.id, systemLogs = mergedLogs)
    }

    fun handleDamage(
        snapshot: CharacterSnapshot,
        liveState: CharacterLiveState,
        damage: Int
    ): Pair<CharacterLiveState, String?> {
        if (damage <= 0) return liveState to null
        val spellId = liveState.concentrationSpellId ?: return liveState to null
        val spellName = findSpellName(snapshot, spellId) ?: spellId
        val now = timestamp()
        val entry = "[$now] Урон $damage угрожает концентрации $spellName"
        val mergedLogs = (liveState.systemLogs + entry).takeLast(10)
        val uiMessage = "Концентрация на $spellName под угрозой ($damage урона)"
        return liveState.copy(systemLogs = mergedLogs) to uiMessage
    }

    fun clearConcentration(
        snapshot: CharacterSnapshot,
        liveState: CharacterLiveState,
        reason: String
    ): CharacterLiveState {
        val spellId = liveState.concentrationSpellId ?: return liveState
        val spellName = findSpellName(snapshot, spellId) ?: spellId
        val now = timestamp()
        val entry = "[$now] Концентрация сброшена: $spellName ($reason)"
        val mergedLogs = (liveState.systemLogs + entry).takeLast(10)
        return liveState.copy(
            concentrationSpellId = null,
            systemLogs = mergedLogs
        )
    }

    private fun timestamp(): String = LocalTime.now().format(timeFormatter)

    private fun findSpellName(snapshot: CharacterSnapshot, id: String): String? {
        return snapshot.magic?.sources?.asSequence()
            ?.flatMap { it.spells.asSequence() }
            ?.firstOrNull { it.id == id }
            ?.name
    }
}
