package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.Race
import com.dnd.app.domain.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton

data class GlobalLoreData(
    val raceDescription: String = "",
    val subraceDescription: String = "",
    val subclassName: String = "",
    val subclassDescription: String = "",
    val alignmentDescription: String = ""
)

@Singleton
class ResolveGlobalLoreUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(draft: DraftCharacter, race: Race?): GlobalLoreData {
        val subraceDescription = draft.baseInfo.subraceIndex
            ?.takeIf { it.isNotBlank() }
            ?.let { libraryRepository.getSubraceModelByIndex(it)?.description }
            .orEmpty()

        var subclassName = ""
        var subclassDescription = ""
        for (step in draft.levelStack.asReversed()) {
            val idx = step.subclassIndex ?: continue
            val subclasses = libraryRepository.getSubclassesForClass(step.classIndex)
            val normalizedIdx = idx.trim().lowercase().replace('_', '-')
            val subclass = subclasses.firstOrNull { info ->
                val s = info.index.trim().lowercase().replace('_', '-')
                s == normalizedIdx || s.endsWith("-$normalizedIdx") || normalizedIdx.endsWith("-$s")
            }
            val desc = subclass?.description.orEmpty()
            if (subclassName.isBlank()) {
                subclassName = subclass?.name.orEmpty().ifBlank { idx }
            }
            if (desc.isNotBlank()) {
                subclassDescription = desc
                break
            }
        }

        val alignmentDescription = libraryRepository.getAllAlignments()
            .firstOrNull { it.indexName == draft.baseInfo.alignmentIndex }
            ?.desc
            .orEmpty()

        return GlobalLoreData(
            raceDescription = race?.description.orEmpty(),
            subraceDescription = subraceDescription,
            subclassName = subclassName,
            subclassDescription = subclassDescription,
            alignmentDescription = alignmentDescription
        )
    }
}
