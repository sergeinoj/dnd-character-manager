// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/GetStructuredRacesUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ParentRace
import com.dnd.app.domain.repository.LibraryRepository
import javax.inject.Inject

class GetStructuredRacesUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(): List<ParentRace> {
        val structuredRaces = mutableListOf<ParentRace>()
        val parentRaces = repository.getAllParentRaces()

        for (parent in parentRaces) {
            val dbSubraces = repository.getSubracesFromDb(parent.id)

            // ИСПРАВЛЕНИЕ ДЛЯ ДРАКОНОРОЖДЕННОГО:
            // Мы берем ТОЛЬКО реальные подрасы из БД.
            // Если dbSubraces пусто (как у Дракона), то subraceOptions будет пустым.
            // UI не покажет дропдаун подрас, и выбор цвета Дракона останется внутри карточки фичи.
            val subraceOptions = dbSubraces.map { it.name }

            structuredRaces.add(ParentRace(parent.id, parent.name, subraceOptions))
        }

        return structuredRaces.sortedBy { it.name }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/GetStructuredRacesUseCase.kt