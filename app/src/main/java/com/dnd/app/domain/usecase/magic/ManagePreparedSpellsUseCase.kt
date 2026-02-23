package com.dnd.app.domain.usecase.magic

import com.dnd.app.domain.model.snapshot.PreparationMode
import com.dnd.app.domain.repository.CharacterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagePreparedSpellsUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke(characterId: Long, sourceId: String, newSpellIds: Set<String>): Result<Unit> {
        return repository.performAtomicMutation(characterId) { snapshot, liveState, _ ->
            val source = snapshot.magic?.sources?.find { it.sourceId == sourceId }
                ?: return@performAtomicMutation Result.failure(
                    Exception("\u0418\u0441\u0442\u043e\u0447\u043d\u0438\u043a '$sourceId' \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d")
                )

            if (source.preparationMode != PreparationMode.PREPARED) {
                return@performAtomicMutation Result.failure(
                    Exception("\u042d\u0442\u043e\u0442 \u043a\u043b\u0430\u0441\u0441 \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442 \u043f\u043e\u0434\u0433\u043e\u0442\u043e\u0432\u043a\u0443")
                )
            }

            val availableInSource = source.spells.map { it.id }.toSet()
            val alwaysPrepared = source.spells.filter { it.isAlwaysPrepared }.map { it.id }.toSet()

            val optionalPool = availableInSource - alwaysPrepared
            val maxPrepared = source.maxPreparedSpells.coerceAtLeast(0)
            val validInput = newSpellIds.intersect(optionalPool)
            val finalPreparedSet = validInput + alwaysPrepared

            val voluntaryCount = (finalPreparedSet - alwaysPrepared).size
            if (voluntaryCount > maxPrepared) {
                return@performAtomicMutation Result.failure(
                    Exception("\u041b\u0438\u043c\u0438\u0442 \u043f\u043e\u0434\u0433\u043e\u0442\u043e\u0432\u043a\u0438 \u043f\u0440\u0435\u0432\u044b\u0448\u0435\u043d ($voluntaryCount/$maxPrepared)")
                )
            }

            val nextMap = liveState.preparedSpellIds.toMutableMap()
            nextMap[sourceId] = finalPreparedSet

            Result.success(liveState.copy(preparedSpellIds = nextMap) to Unit)
        }
    }
}
