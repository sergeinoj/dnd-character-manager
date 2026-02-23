// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\magic\MagicUiModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.magic

import com.dnd.app.domain.model.magic.SlotPreference
import com.dnd.app.domain.model.magic.SpellCastContext

import androidx.compose.runtime.Immutable

@Immutable
data class MagicUiState(
    val globalSlots: GlobalSlotsUiModel?,
    val sources: List<SpellSourceUiModel>,
    val activePreparation: PreparationStateUiModel?,
    val innateSpellIds: Set<String> = emptySet()
)

sealed interface CastAction {
    data class SpendSlot(
        val level: Int,
        val preference: SlotPreference = SlotPreference.AUTO,
        val spellContext: SpellCastContext? = null
    ) : CastAction
    data class SpendCharge(val poolId: String) : CastAction
    data class RitualIntent(val spellId: String) : CastAction
    data class SpendInnateUsage(val spellId: String) : CastAction
}

@Immutable
data class SpellUiModel(
    val id: String,
    val sourceId: String,
    val sourceTag: String?,
    val name: String,
    val level: Int,
    val school: String,
    val castingTime: String,
    val range: String,
    val components: String,
    val duration: String,
    val description: String,
    val isConcentration: Boolean,
    val isRitual: Boolean,
    val isAlwaysPrepared: Boolean,
    val isUpcast: Boolean,
    val isResourceExhausted: Boolean,
    val isPending: Boolean,
    val castAction: CastAction?,
    val castWarning: String?,
    val isWarlockSource: Boolean = false,
    val isCurrentConcentration: Boolean = false
)

@Immutable
data class SpellLevelGroup(
    val label: String,
    val spells: List<SpellUiModel>
)

@Immutable
data class SpellSourceUiModel(
    val id: String,
    val title: String,
    val statsInfo: String,
    val chargesText: String?,
    val exclusiveResourcePoolId: String?,
    val maxCharges: Int,
    val spentCharges: Int,
    val canPrepare: Boolean,
    val groups: List<SpellLevelGroup>
)

@Immutable
data class PreparationStateUiModel(
    val sourceId: String,
    val title: String,
    val preparedSpells: List<SpellUiModel>,
    val availableSpells: List<SpellUiModel>,
    val canLearnSpells: Boolean,
    val counterText: String,
    val canConfirm: Boolean,
    val isModified: Boolean = false
)

@Immutable
data class GlobalSlotsUiModel(
    val classSlots: List<SpellSlotLevelUiModel>,
    val innateSlots: List<InnateSlotUiModel>,
    val pactSlots: SpellSlotLevelUiModel?,
    val showClassSlots: Boolean
) {
    val isVisible: Boolean get() = showClassSlots || pactSlots != null
}

@Immutable
data class InnateSlotUiModel(
    val slotId: String,
    val spellId: String,
    val spellName: String,
    val usedCount: Int,
    val isSpent: Boolean,
    val isPending: Boolean,
    val action: CastAction
)

@Immutable
data class SpellSlotLevelUiModel(
    val level: Int,
    val slots: List<SpellSlotCircleUiModel>
)

@Immutable
data class SpellSlotCircleUiModel(
    val isSpent: Boolean,
    val isPending: Boolean = false
)

data class MagicPreparationDraft(
    val sourceId: String,
    val selectedIds: Set<String>
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\magic\MagicUiModels.kt
