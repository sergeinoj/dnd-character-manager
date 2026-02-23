// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\GetBackgroundDataUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import android.util.Log
import com.dnd.app.domain.model.Background
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.repository.BackgroundRepository
import com.dnd.app.ui.screens.character_creator.EquipmentOptionDetails
import javax.inject.Inject
import javax.inject.Singleton


data class BackgroundData(
    val background: Background?,
    val unpackedEquipment: Map<String, EquipmentOptionDetails>
)


@Singleton
class GetBackgroundDataUseCase @Inject constructor(
    private val backgroundRepository: BackgroundRepository,
    private val unpackEquipmentUseCase: UnpackEquipmentUseCase
) {
    private val TAG = "DND_LOG_BG_DATA_UC"

    suspend operator fun invoke(backgroundIndex: String): BackgroundData {
        if (backgroundIndex.isBlank()) {
            return BackgroundData(null, emptyMap())
        }
        Log.d(TAG, "Executing for background index: '$backgroundIndex'")

        val background = backgroundRepository.getBackgroundByIndex(backgroundIndex)

        if (background == null) {
            Log.w(TAG, "Background not found for index: $backgroundIndex")
            return BackgroundData(null, emptyMap())
        }


        val staticEquipment = background.equipment


        val equipmentFromChoices = background.features
            .flatMap { it.choices }
            .filterIsInstance<FeatureChoiceDomain.SelectOption>()
            .flatMap { it.options }
            .map { it.id }

        val allIndexes = (staticEquipment + equipmentFromChoices).distinct().filter { it.isNotBlank() }
        Log.d(TAG, "Found ${allIndexes.size} total equipment indexes to unpack for '${background.name}'.")

        val unpackedMap = unpackEquipmentUseCase(allIndexes)
        Log.d(TAG, "Unpacked into ${unpackedMap.size} unique items.")

        return BackgroundData(background, unpackedMap)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\GetBackgroundDataUseCase.kt