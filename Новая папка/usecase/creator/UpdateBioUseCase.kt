// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/UpdateBioUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.creator

import com.dnd.app.domain.model.Background
import com.dnd.app.domain.model.DraftCharacter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [НОВЫЙ USE CASE]
 * Изолированная логика для обновления полей биографии и броска случайных черт.
 */
@Singleton
class UpdateBioUseCase @Inject constructor() {

    fun updateField(
        draft: DraftCharacter,
        fieldType: String,
        value: String
    ): DraftCharacter {
        val base = draft.baseInfo
        val newBase = when (fieldType) {
            "name" -> return draft.copy(name = value)
            "alignment" -> base.copy(alignmentIndex = value)
            "personality" -> base.copy(personalityTrait = value)
            "ideal" -> base.copy(ideal = value)
            "bond" -> base.copy(bond = value)
            "flaw" -> base.copy(flaw = value)
            else -> base
        }
        return draft.copy(baseInfo = newBase)
    }

    fun rollTrait(
        draft: DraftCharacter,
        fieldType: String,
        allBackgrounds: List<Background>
    ): DraftCharacter {
        val bgName = draft.baseInfo.backgroundIndex
        val bg = allBackgrounds.find { it.name == bgName } ?: return draft

        val list = when (fieldType) {
            "personality" -> bg.personalityTraits
            "ideal" -> bg.ideals
            "bond" -> bg.bonds
            "flaw" -> bg.flaws
            else -> emptyList()
        }

        return if (list.isNotEmpty()) {
            updateField(draft, fieldType, list.random())
        } else {
            draft
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/creator/UpdateBioUseCase.kt