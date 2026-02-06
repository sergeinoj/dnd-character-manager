// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/ClassStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun ClassStep(
    availableClasses: List<ClassInfo>,
    selectedClassIndex: String,
    onClassSelect: (String) -> Unit,
    onSubclassSelect: (String) -> Unit,
    classFeatures: List<Feature>,
    currentSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    globalExclusions: Set<String>,
    currentSubclassIndex: String?,
    pickedSkills: List<String>
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1. ВЫБОР КЛАССА
        item {
            FlatWizardSection(title = "Класс") {
                val opts = availableClasses.map { ChoiceOption(it.index, it.name) }
                SmartDropdown(opts, selectedClassIndex, onSelected = { onClassSelect(it.id) })
            }
        }

        val selClass = availableClasses.find { it.index == selectedClassIndex }
        if (selClass != null) {
            // 2. ХИТЫ
            item {
                FlatWizardSection(title = "Хиты") {
                    Column {
                        Text("Кость здоровья: 1к${selClass.hitDie}", fontSize = 14.sp)
                        Text("Начальное здоровье: ${selClass.hitDie} + Мод. Телосложения", fontSize = 14.sp, color = Color.DarkGray)
                    }
                }
            }

            // 3. СПЕЦИАЛИЗАЦИЯ (Только те, кто выбирает на 1-м уровне)
            // ПАТЧ: Убраны bard и wizard, т.к. они выбирают позже.
            val classesWithLvl1Subclass = listOf("cleric", "sorcerer", "warlock")
            if (selClass.subclasses.isNotEmpty() && (selectedClassIndex in classesWithLvl1Subclass)) {
                item {
                    FlatWizardSection(title = "Специализация") {
                        val subOpts = selClass.subclasses.map { ChoiceOption(it.index, it.name) }
                        SmartDropdown(subOpts, currentSubclassIndex, onSelected = { onSubclassSelect(it.id) }, placeholder = "Выберите путь...")
                    }
                }
            }

            // 4. ДИНАМИЧЕСКИЕ ФИЧИ (Навыки, Экспертиза, Магия)
            items(classFeatures, key = { "f_${it.id}_${it.index}" }) { feat ->
                FlatWizardSection(title = feat.name) {
                    Column {
                        if (feat.description.isNotBlank()) {
                            Text(feat.description.stripHtml(), fontSize = 14.sp, lineHeight = 17.sp)
                        }
                        feat.embeddedSpells.forEach { EmbeddedSpellRow(it) }
                        FeatureChoiceBlock(
                            choice = feat.choices.firstOrNull() ?: return@Column, // Поддерживаем один основной выбор на фичу
                            currentSelection = currentSelections[feat.index],
                            onSelectionChanged = { onSelectionChanged(feat.index, it) },
                            globalExclusions = globalExclusions,
                            pickedSkills = pickedSkills
                        )
                        // Если выборов несколько (редко, но бывает)
                        if (feat.choices.size > 1) {
                            feat.choices.drop(1).forEach { additionalChoice ->
                                FeatureChoiceBlock(additionalChoice, currentSelections[feat.index], { onSelectionChanged(feat.index, it) }, globalExclusions, pickedSkills)
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/ClassStep.kt