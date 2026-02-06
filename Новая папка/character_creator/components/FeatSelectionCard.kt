// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/FeatSelectionCard.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.Feature
import com.dnd.app.util.stripHtml

@Composable
fun FeatSelectionCard(
    grantingFeature: Feature,
    selectedFeatDetails: Feature?,
    allSelections: Map<String, ChoiceResult>,
    proficiencyExclusions: Set<String>,
    onSelectionUpdated: (key: String, result: ChoiceResult) -> Unit,
    expandedStates: MutableMap<String, Boolean>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // --- Основное описание и выбор самой черты ---
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
                selectionKey = grantingFeature.index, // Ключ для выбора самой черты
                globalExclusions = proficiencyExclusions,
                expandedStates = expandedStates
            )
        }

        // --- БЛОК ДЕТАЛЕЙ И ПОД-ВЫБОРОВ ВЫБРАННОЙ ЧЕРТЫ ---
        // ИСПРАВЛЕНО: AnimatedVisibility удален в пользу простой проверки `let`.
        // Блок будет появляться/исчезать мгновенно и не будет иметь собственного состояния "свернут/развернут".
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
                details.choices.forEach { choice ->
                    // Ключ для под-выбора строится на основе родительского ключа и индекса самой черты
                    val subSelectionKey = "${grantingFeature.index}_${details.index}"
                    FeatureChoiceBlock(
                        choice = choice,
                        allSelections = allSelections,
                        onSelectionUpdated = onSelectionUpdated,
                        selectionKey = subSelectionKey,
                        globalExclusions = proficiencyExclusions,
                        expandedStates = expandedStates
                    )
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/FeatSelectionCard.kt