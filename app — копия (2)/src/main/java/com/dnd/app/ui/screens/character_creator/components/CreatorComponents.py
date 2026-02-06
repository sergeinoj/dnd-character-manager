// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/CreatorComponents.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.stripHtml
import kotlinx.serialization.json.Json

@Composable
fun FlatWizardSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth().border(1.dp, Color(0xFF424242))) {
        if (title.isNotBlank()) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF424242)).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Color.White)
            }
        }
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFC0C0C0)).padding(8.dp)) {
            content()
        }
    }
}

@Composable
fun SmartDropdown(
    options: List<ChoiceOption>,
    selectedId: String?,
    onSelected: (ChoiceOption) -> Unit,
    placeholder: String = "Пусто",
    exclusions: Set<String> = emptySet()
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.id == selectedId }
    val displayText = selectedOption?.label ?: placeholder

    val filteredOptions = options.filter { it.id == selectedId || !exclusions.contains(it.id) }

    Column {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color.White, RoundedCornerShape(2.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
            .clickable { if (filteredOptions.isNotEmpty()) expanded = true }
            .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = displayText,
                    color = if (selectedOption == null) Color.Gray else Color.Black,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = Color.Black)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
                filteredOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.label, color = Color.Black, fontSize = 14.sp) },
                        onClick = { onSelected(opt); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureChoiceBlock(
    choice: FeatureChoiceDomain,
    currentSelection: ChoiceResult?,
    onSelectionChanged: (ChoiceResult) -> Unit,
    globalExclusions: Set<String> = emptySet(),
    pickedSkills: List<String> = emptyList()
) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        when (choice) {
            is FeatureChoiceDomain.SelectSkill -> {
                val selected = (currentSelection as? ChoiceResult.Skills)?.skillIndexes ?: emptyList()
                repeat(choice.count) { i ->
                    SmartDropdown(
                        options = choice.options,
                        selectedId = selected.getOrNull(i),
                        onSelected = { opt ->
                            val newList = selected.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                            onSelectionChanged(ChoiceResult.Skills(newList.filter { it.isNotBlank() }))
                        },
                        exclusions = globalExclusions + selected.filterIndexed { index, _ -> index != i },
                        placeholder = "Выберите навык..."
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            is FeatureChoiceDomain.SelectOption -> {
                val selected = (currentSelection as? ChoiceResult.SelectedOptions)?.items ?: emptyList()
                repeat(choice.count) { i ->
                    val currentId = selected.getOrNull(i)
                    val selectedOption = choice.options.find { it.id == currentId }

                    Column {
                        SmartDropdown(
                            options = choice.options,
                            selectedId = currentId,
                            onSelected = { opt ->
                                val newList = selected.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                                onSelectionChanged(ChoiceResult.SelectedOptions(newList.filter { it.isNotBlank() }))
                            },
                            exclusions = globalExclusions + selected.filterIndexed { index, _ -> index != i },
                            placeholder = "Выберите вариант..."
                        )

                        // РЕКУРСИЯ: Если у выбранного пункта есть вложенный выбор
                        if (selectedOption?.subChoice != null) {
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier
                                .padding(start = 12.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                            ) {
                                FeatureChoiceBlock(
                                    choice = selectedOption.subChoice,
                                    currentSelection = null, // Вложенные выборы требуют отдельной логики сохранения
                                    onSelectionChanged = { /* Обработка вложенности */ },
                                    pickedSkills = pickedSkills
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            is FeatureChoiceDomain.SelectSpell -> {
                val selected = (currentSelection as? ChoiceResult.Spells)?.spellIndexes ?: emptyList()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    choice.options.forEach { opt ->
                        val isSelected = selected.contains(opt.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0xFFE8F5E9) else Color.White)
                                .border(1.dp, Color.LightGray)
                                .clickable {
                                    val newList = if (isSelected) selected - opt.id else (selected + opt.id).take(choice.count)
                                    onSelectionChanged(ChoiceResult.Spells(newList))
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF2E7D32) else Color.Gray
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(opt.label, fontSize = 14.sp)
                        }
                    }
                }
            }
            is FeatureChoiceDomain.SelectStatBonus -> {
                val sb = (currentSelection as? ChoiceResult.StatBonus)?.bonuses ?: emptyMap()
                val keys = sb.keys.toList()
                repeat(choice.count) { i ->
                    SmartDropdown(
                        options = choice.options,
                        selectedId = keys.getOrNull(i),
                        onSelected = { opt ->
                            val nm = sb.toMutableMap()
                            if(keys.getOrNull(i) != null) nm.remove(keys[i])
                            nm[opt.id] = choice.amount
                            onSelectionChanged(ChoiceResult.StatBonus(nm))
                        },
                        exclusions = globalExclusions + keys.filterIndexed { index, _ -> index != i }
                    )
                }
            }
            is FeatureChoiceDomain.SelectExpertise -> {
                val selected = (currentSelection as? ChoiceResult.Skills)?.skillIndexes ?: emptyList()
                val options = pickedSkills.map { ChoiceOption(it, DndLocalization.translateSkill(it)) }

                repeat(choice.count) { i ->
                    SmartDropdown(
                        options = options,
                        selectedId = selected.getOrNull(i),
                        onSelected = { opt ->
                            val newList = selected.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                            onSelectionChanged(ChoiceResult.Skills(newList.filter { it.isNotBlank() }))
                        },
                        exclusions = selected.filterIndexed { index, _ -> index != i }.toSet(),
                        placeholder = "Выберите мастерство..."
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun EmbeddedSpellRow(spell: Spell) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).background(Color.White).border(1.dp, Color.Gray)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(spell.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${spell.level} уровень, ${spell.school}", fontSize = 11.sp, color = Color.Gray)
            }
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
        }
        if (expanded) {
            Column(modifier = Modifier.padding(8.dp).background(Color(0xFFF5F5F5)).fillMaxWidth().padding(8.dp)) {
                Text("Время: ${spell.castingTime}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Дистанция: ${spell.range}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Компоненты: ${spell.components.stripHtml()}", fontSize = 12.sp)
                Text("Длительность: ${spell.duration}", fontSize = 12.sp)
                Divider(Modifier.padding(vertical = 4.dp))
                Text(spell.description.stripHtml(), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/CreatorComponents.kt