// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/RaceStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.components.*
import com.dnd.app.util.stripHtml

@Composable
fun RaceStep(
    availableRaces: List<Race>,
    selectedRaceIndex: String,
    onRaceSelect: (String) -> Unit,
    availableSubraces: List<Race>,
    selectedSubraceIndex: String?,
    onSubraceSelect: (String) -> Unit,
    features: List<Feature>,
    currentSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    globalExclusions: Set<String>
) {
    // Сохраняем состояние скролла между рекомпозициями
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. СЕЛЕКТОР ОСНОВНОЙ РАСЫ
        item(key = "main_race_dropdown_selector") {
            FlatWizardSection(title = "Раса") {
                val options = availableRaces.map { ChoiceOption(it.index, it.name) }
                SmartDropdown(options, selectedRaceIndex, onSelected = { onRaceSelect(it.id) })
            }
        }

        // 2. ДИНАМИЧЕСКИЙ СПИСОК ФИЧ
        // Ключ включает ID фичи, что гарантирует стабильность позиции существующих элементов
        items(features, key = { "feat_${it.index}_${it.id}" }) { feature ->
            if (feature.isSubraceSelector) {
                // СЕЛЕКТОР ПОДРАСЫ: Появляется ровно там, где его поставил репозиторий
                FlatWizardSection(title = feature.name) {
                    Column {
                        if (feature.description.isNotBlank()) {
                            Text(
                                text = feature.description,
                                fontSize = 13.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        val options = availableSubraces.map { ChoiceOption(it.index, it.name) }
                        SmartDropdown(
                            options = options,
                            selectedId = selectedSubraceIndex,
                            onSelected = { onSubraceSelect(it.id) },
                            placeholder = "Выберите разновидность..."
                        )
                    }
                }
            } else {
                // ОБЫЧНАЯ ФИЧА
                FlatWizardSection(title = feature.name) {
                    Column {
                        if (feature.description.isNotBlank()) {
                            Text(
                                text = feature.description.stripHtml(),
                                fontSize = 14.sp,
                                color = Color.Black,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(bottom = if (feature.choices.isNotEmpty() || feature.embeddedSpells.isNotEmpty()) 8.dp else 0.dp)
                            )
                        }

                        feature.embeddedSpells.forEach { EmbeddedSpellRow(it) }

                        feature.choices.forEach { choice ->
                            FeatureChoiceBlock(
                                choice = choice,
                                currentSelection = currentSelections[feature.index],
                                onSelectionChanged = { res -> onSelectionChanged(feature.index, res) },
                                globalExclusions = globalExclusions
                            )
                        }
                    }
                }
            }
        }

        item(key = "race_bottom_padding") { Spacer(Modifier.height(32.dp)) }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/RaceStep.kt