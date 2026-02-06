// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/CreatorComponents.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.animation.AnimatedVisibility
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

@Composable
fun FlatWizardSection(
    title: String,
    modifier: Modifier = Modifier,
    applyDefaultContentPadding: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().border(WizardUiConfig.SECTION_BORDER_WIDTH, WizardUiConfig.SECTION_BORDER_COLOR)) {
        if (title.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WizardUiConfig.SECTION_HEADER_BG_COLOR)
                    .padding(
                        horizontal = WizardUiConfig.SECTION_HEADER_PADDING_HORIZONTAL,
                        vertical = WizardUiConfig.SECTION_HEADER_PADDING_VERTICAL
                    )
            ) {
                Text(
                    title,
                    fontSize = WizardUiConfig.SECTION_HEADER_FONT_SIZE,
                    fontWeight = WizardUiConfig.SECTION_HEADER_FONT_WEIGHT,
                    color = WizardUiConfig.SECTION_HEADER_TEXT_COLOR
                )
            }
        }
        val contentModifier = if (applyDefaultContentPadding) {
            Modifier
                .fillMaxWidth()
                .background(WizardUiConfig.SECTION_CONTENT_BG_COLOR)
                .padding(WizardUiConfig.SECTION_CONTENT_PADDING)
        } else {
            Modifier
                .fillMaxWidth()
                .background(WizardUiConfig.SECTION_CONTENT_BG_COLOR)
        }
        Column(modifier = contentModifier) {
            content()
        }
    }
}

@Composable
fun CollapsibleChoiceSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0)) // Specific component color, not in WizardUiConfig
                .border(1.dp, Color.Gray)
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Развернуть/Свернуть"
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                content()
            }
        }
    }
}


@Composable
fun FeatureChoiceBlock(
    choice: FeatureChoiceDomain,
    allSelections: Map<String, ChoiceResult>,
    onSelectionUpdated: (key: String, result: ChoiceResult) -> Unit,
    selectionKey: String,
    expandedStates: MutableMap<String, Boolean>,
    globalExclusions: Set<String> = emptySet(),
    pickedSkills: List<String> = emptyList()
) {
    val currentSelection = allSelections[selectionKey]

    val columnModifier = if (choice is FeatureChoiceDomain.SelectOption && choice.description == "@CONTAINER@") {
        Modifier
    } else {
        Modifier.padding(top = WizardUiConfig.CHOICE_BLOCK_TOP_PADDING)
    }

    Column(modifier = columnModifier) {
        when (choice) {
            is FeatureChoiceDomain.SelectSkill -> {
                repeat(choice.count) { i ->
                    SmartDropdown(
                        options = choice.options,
                        selectedId = (currentSelection as? ChoiceResult.Skills)?.skillIndexes?.getOrNull(i),
                        onSelected = { opt ->
                            val currentList = (currentSelection as? ChoiceResult.Skills)?.skillIndexes ?: emptyList()
                            val newList = currentList.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                            onSelectionUpdated(selectionKey, ChoiceResult.Skills(newList.filter { it.isNotBlank() }))
                        },
                        exclusions = globalExclusions + ((currentSelection as? ChoiceResult.Skills)?.skillIndexes?.filterIndexed { idx, _ -> idx != i } ?: emptyList()),
                        placeholder = "Выберите навык..."
                    )
                    Spacer(Modifier.height(WizardUiConfig.CHOICE_ITEM_VERTICAL_SPACING))
                }
            }

            is FeatureChoiceDomain.SelectStatBonus -> {
                val currentBonuses = (currentSelection as? ChoiceResult.StatBonus)?.bonuses ?: emptyMap()
                val keys = currentBonuses.keys.toList()
                repeat(choice.count) { i ->
                    SmartDropdown(
                        options = choice.options,
                        selectedId = keys.getOrNull(i),
                        onSelected = { opt ->
                            val newMap = currentBonuses.toMutableMap()
                            if(keys.getOrNull(i) != null) newMap.remove(keys[i])
                            newMap[opt.id] = choice.amount
                            onSelectionUpdated(selectionKey, ChoiceResult.StatBonus(newMap))
                        },
                        exclusions = globalExclusions + keys.filterIndexed { idx, _ -> idx != i },
                        placeholder = "Характеристика..."
                    )
                    Spacer(Modifier.height(WizardUiConfig.CHOICE_ITEM_VERTICAL_SPACING))
                }
            }

            is FeatureChoiceDomain.SelectOption -> {
                if (choice.description == "@CONTAINER@") {
                    Column(verticalArrangement = Arrangement.spacedBy(WizardUiConfig.SPELL_GROUP_SUBGROUP_SPACING)) {
                        choice.options.forEach { option ->
                            val subKey = "${selectionKey}_${option.id}"
                            if (option.info == "Автоматически") {
                                FlatWizardSection(title = "Автоматически", applyDefaultContentPadding = true) {
                                    Text("${option.label} (Компетентность)", fontSize = 14.sp)
                                }
                            }
                            option.subChoice?.let { sub ->
                                FlatWizardSection(title = option.label, applyDefaultContentPadding = true) {
                                    FeatureChoiceBlock(
                                        choice = sub, allSelections = allSelections, onSelectionUpdated = onSelectionUpdated,
                                        selectionKey = subKey, expandedStates = expandedStates,
                                        globalExclusions = globalExclusions, pickedSkills = pickedSkills
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val selectedItems = (currentSelection as? ChoiceResult.SelectedOptions)?.items ?: emptyList()
                    repeat(choice.count) { i ->
                        val currentId = selectedItems.getOrNull(i)
                        val selectedOption = choice.options.find { it.id == currentId }

                        Column {
                            SmartDropdown(
                                options = choice.options, selectedId = currentId,
                                onSelected = { opt ->
                                    val newList = selectedItems.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                                    onSelectionUpdated(selectionKey, ChoiceResult.SelectedOptions(newList.filter { it.isNotBlank() }))
                                },
                                exclusions = globalExclusions + selectedItems.filterIndexed { idx, _ -> idx != i },
                                placeholder = "Выберите вариант..."
                            )

                            selectedOption?.info?.let { info ->
                                Text(info, fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                            }

                            selectedOption?.subChoice?.let { subChoice ->
                                Spacer(Modifier.height(8.dp))
                                Box(modifier = Modifier.padding(start = 12.dp).border(1.dp, Color.LightGray).padding(8.dp)) {
                                    val subKey = "${selectionKey}_${selectedOption.id}"
                                    FeatureChoiceBlock(
                                        choice = subChoice, allSelections = allSelections, onSelectionUpdated = onSelectionUpdated,
                                        selectionKey = subKey, expandedStates = expandedStates,
                                        globalExclusions = globalExclusions, pickedSkills = pickedSkills
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(WizardUiConfig.CHOICE_ITEM_VERTICAL_SPACING))
                    }
                }
            }

            is FeatureChoiceDomain.SelectSpell -> {
                SpellSelectionGroup(
                    choice = choice,
                    selectionKey = selectionKey,
                    allSelections = allSelections,
                    onSelectionUpdated = onSelectionUpdated,
                    expandedStates = expandedStates
                )
            }

            is FeatureChoiceDomain.SelectExpertise -> {
                val selected = (currentSelection as? ChoiceResult.Skills)?.skillIndexes ?: emptyList()
                val availableOptionsForExpertise = pickedSkills.map { ChoiceOption(it, DndLocalization.translateSkill(it)) }

                if (choice.options.any { it.subChoice != null }) {
                    val selectedItems = (currentSelection as? ChoiceResult.SelectedOptions)?.items ?: emptyList()
                    repeat(choice.count) { i ->
                        val currentId = selectedItems.getOrNull(i)
                        val selectedOption = choice.options.find { it.id == currentId }
                        Column {
                            SmartDropdown(
                                options = choice.options, selectedId = currentId,
                                onSelected = { opt ->
                                    val newList = selectedItems.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                                    onSelectionUpdated(selectionKey, ChoiceResult.SelectedOptions(newList.filter { it.isNotBlank() }))
                                },
                                exclusions = selectedItems.filterIndexed { idx, _ -> idx != i }.toSet(),
                                placeholder = "Выберите вариант..."
                            )
                            selectedOption?.subChoice?.let { subChoice ->
                                Spacer(Modifier.height(8.dp))
                                Box(modifier = Modifier.padding(start = 12.dp).border(1.dp, Color.LightGray).padding(8.dp)) {
                                    val subKey = "${selectionKey}_${selectedOption.id}"
                                    FeatureChoiceBlock(subChoice, allSelections, onSelectionUpdated, subKey, expandedStates, globalExclusions, pickedSkills)
                                }
                            }
                        }
                    }
                } else {
                    repeat(choice.count) { i ->
                        SmartDropdown(
                            options = availableOptionsForExpertise,
                            selectedId = selected.getOrNull(i),
                            onSelected = { opt ->
                                val newList = selected.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                                onSelectionUpdated(selectionKey, ChoiceResult.Skills(newList.filter { it.isNotBlank() }))
                            },
                            exclusions = selected.filterIndexed { idx, _ -> idx != i }.toSet(),
                            placeholder = "Выберите мастерство..."
                        )
                        Spacer(Modifier.height(WizardUiConfig.CHOICE_ITEM_VERTICAL_SPACING))
                    }
                }
            }
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
    val filteredOptions = options.filter { it.id == selectedId || !exclusions.contains(it.id) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WizardUiConfig.DROPDOWN_HEIGHT)
            .background(WizardUiConfig.DROPDOWN_BG_COLOR)
            .border(1.dp, WizardUiConfig.DROPDOWN_BORDER_COLOR)
            .clickable { if (filteredOptions.isNotEmpty()) expanded = true }
            .padding(horizontal = WizardUiConfig.DROPDOWN_PADDING_HORIZONTAL),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = selectedOption?.label ?: placeholder,
                color = if (selectedOption == null) WizardUiConfig.DROPDOWN_PLACEHOLDER_TEXT_COLOR else WizardUiConfig.DROPDOWN_SELECTED_TEXT_COLOR,
                fontSize = WizardUiConfig.DROPDOWN_FONT_SIZE,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, null, tint = WizardUiConfig.DROPDOWN_ARROW_TINT)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            filteredOptions.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label, fontSize = WizardUiConfig.DROPDOWN_MENU_ITEM_FONT_SIZE) },
                    onClick = { onSelected(opt); expanded = false }
                )
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/CreatorComponents.kt