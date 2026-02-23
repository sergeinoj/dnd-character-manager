package com.dnd.app.domain.usecase.magic

import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.Money
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearnWizardSpellUseCase @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(characterId: Long, spellIndex: String): Result<Unit> {
        val draft = characterRepository.getDraftById(characterId)
            ?: return Result.failure(IllegalArgumentException("\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d"))

        val wizardStepIndex = draft.levelStack.indexOfLast { it.classIndex.equals("wizard", ignoreCase = true) }
        if (wizardStepIndex == -1) {
            return Result.failure(IllegalStateException("\u0418\u0437\u0443\u0447\u0435\u043d\u0438\u0435 \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u0442\u043e\u043b\u044c\u043a\u043e \u0432\u043e\u043b\u0448\u0435\u0431\u043d\u0438\u043a\u0443"))
        }

        val wizardSpellsByIndex = libraryRepository.getAllSpellsByClass("wizard").associateBy { it.index }
        val spell = wizardSpellsByIndex[spellIndex]
        if (spell == null) {
            return Result.failure(IllegalArgumentException("\u0417\u0430\u043a\u043b\u0438\u043d\u0430\u043d\u0438\u0435 \u043d\u0435 \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430 \u0432\u043e\u043b\u0448\u0435\u0431\u043d\u0438\u043a\u0430"))
        }
        val learnCostCp = spell.level.coerceAtLeast(1) * 5000
        val sheet = characterRepository.getCharacterForSheet(characterId).firstOrNull()
            ?: return Result.failure(IllegalArgumentException("\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d"))
        if (sheet.liveState.coins.toCopper() < learnCostCp.toLong()) {
            return Result.failure(IllegalStateException("\u041d\u0435\u0434\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u043e \u0437\u043e\u043b\u043e\u0442\u0430 \u0434\u043b\u044f \u0438\u0437\u0443\u0447\u0435\u043d\u0438\u044f"))
        }

        if (spellIndex in draft.getAllLearnedSpells()) {
            return Result.failure(IllegalStateException("\u0417\u0430\u043a\u043b\u0438\u043d\u0430\u043d\u0438\u0435 \u0443\u0436\u0435 \u0438\u0437\u0443\u0447\u0435\u043d\u043e"))
        }

        val key = "wizard_spellbook_learned"
        val oldStep = draft.levelStack[wizardStepIndex]
        val oldKnown = (oldStep.selections[key] as? ChoiceResult.Spells)?.spellIndexes.orEmpty()
        val updatedStep = oldStep.copy(
            selections = oldStep.selections + (key to ChoiceResult.Spells(oldKnown + spellIndex))
        )
        val updatedStack = draft.levelStack.toMutableList().apply { this[wizardStepIndex] = updatedStep }
        val updatedDraft = draft.copy(levelStack = updatedStack)

        return characterRepository.commitFullCharacter(updatedDraft).fold(
            onSuccess = {
                characterRepository.syncLiveState(characterId) { live ->
                    val remainingCp = (live.coins.toCopper() - learnCostCp.toLong()).coerceAtLeast(0L).toInt()
                    live.copy(coins = Money.fromCp(remainingCp))
                }.fold(
                    onSuccess = { Result.success(Unit) },
                    onFailure = { Result.failure(it) }
                )
            },
            onFailure = { Result.failure(it) }
        )
    }
}
