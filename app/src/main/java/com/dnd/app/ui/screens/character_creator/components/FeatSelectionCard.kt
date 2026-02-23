// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\components\FeatSelectionCard.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.ChoicePathManager
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.StaticProficiency
import com.dnd.app.util.stripHtml

@Composable
fun FeatSelectionCard(
    grantingFeature: Feature,
    selectedFeatDetails: Feature?,
    allSelections: Map<String, ChoiceResult>,
    proficiencyExclusions: Map<Int, Set<String>>,
    pickedProficiencies: List<StaticProficiency>,
    onSelectionUpdated: (key: String, result: ChoiceResult) -> Unit,
    isExpanded: (String) -> Boolean,
    onToggleExpanded: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (grantingFeature.description.isNotBlank()) {
            Text(
                text = grantingFeature.description.stripHtml(),
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
        }
        grantingFeature.choices.forEach { choice ->
            FeatureChoiceBlock(
                choice = choice,
                allSelections = allSelections,
                onSelectionUpdated = onSelectionUpdated,
                selectionKey = grantingFeature.index,
                proficiencyExclusions = proficiencyExclusions,
                pickedProficiencies = pickedProficiencies,
                isExpanded = isExpanded,
                onToggleExpanded = onToggleExpanded
            )
        }

        selectedFeatDetails?.let { details ->
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = details.description.stripHtml(),
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )

                details.choices.forEachIndexed { index, choice ->
                    val parentSelection = (allSelections[grantingFeature.index] as? ChoiceResult.SelectedOptions)?.items?.firstOrNull()
                    if (parentSelection != null && parentSelection == details.index) {

                        val subSelectionKey = ChoicePathManager.append(grantingFeature.index, details.index)


                        val uniqueChoiceKey = ChoicePathManager.append(subSelectionKey, "", index)

                        FeatureChoiceBlock(
                            choice = choice,
                            allSelections = allSelections,
                            onSelectionUpdated = onSelectionUpdated,
                            selectionKey = uniqueChoiceKey,
                            proficiencyExclusions = proficiencyExclusions,
                            pickedProficiencies = pickedProficiencies,
                            isExpanded = isExpanded,
                            onToggleExpanded = onToggleExpanded
                        )
                    }
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\components\FeatSelectionCard.kt