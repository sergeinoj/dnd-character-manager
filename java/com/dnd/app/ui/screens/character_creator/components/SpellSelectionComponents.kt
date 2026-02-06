// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/SpellSelectionComponents.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.FeatureChoiceDomain

@Composable
fun SpellSelectionGroup(
    choice: FeatureChoiceDomain.SelectSpell,
    selectionKey: String,
    allSelections: Map<String, ChoiceResult>,
    onSelectionUpdated: (key: String, result: ChoiceResult) -> Unit,
    expandedStates: MutableMap<String, Boolean>
) {
    val currentlySelectedIds = (allSelections[selectionKey] as? ChoiceResult.Spells)?.spellIndexes ?: emptyList()
    val totalToChoose = choice.count

    val selectedOptions = choice.options.filter { it.id in currentlySelectedIds }.sortedBy { it.label }
    val availableOptions = choice.options.filterNot { it.id in currentlySelectedIds }.sortedBy { it.label }

    val canAddMore = currentlySelectedIds.size < totalToChoose

    Column(verticalArrangement = Arrangement.spacedBy(WizardUiConfig.SPELL_GROUP_ACTION_ROW_SPACING)) {
        // --- Секция "Добавленные" ---
        val addedExpandedKey = "${selectionKey}_added"
        val isAddedExpanded = expandedStates.getOrPut(addedExpandedKey) { false }
        CollapsibleActionRow(
            title = "Добавленные (${selectedOptions.size}/${totalToChoose})",
            isExpanded = isAddedExpanded,
            onToggle = { expandedStates[addedExpandedKey] = !isAddedExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(WizardUiConfig.SPELL_LIST_ITEM_SPACING)) {
                selectedOptions.forEach { option ->
                    option.spell?.let { spell ->
                        val spellExpandedKey = "${selectionKey}_${spell.index}"
                        val isSpellExpanded = expandedStates.getOrPut(spellExpandedKey) { false }
                        UnifiedSpellListItem(
                            spell = spell,
                            actionType = SpellActionType.REMOVE,
                            onActionClick = {
                                val newList = currentlySelectedIds.toMutableList().apply { remove(spell.index) }
                                onSelectionUpdated(selectionKey, ChoiceResult.Spells(newList))
                            },
                            isActionEnabled = true,
                            isExpanded = isSpellExpanded,
                            onToggleExpand = { expandedStates[spellExpandedKey] = !isSpellExpanded }
                        )
                    }
                }
            }
        }

        // --- Секция "Выбрать" ---
        val availableExpandedKey = "${selectionKey}_available"
        val isAvailableExpanded = expandedStates.getOrPut(availableExpandedKey) { true }
        CollapsibleActionRow(
            title = "Выбрать (${totalToChoose - selectedOptions.size})",
            isExpanded = isAvailableExpanded,
            onToggle = { expandedStates[availableExpandedKey] = !isAvailableExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(WizardUiConfig.SPELL_LIST_ITEM_SPACING)) {
                availableOptions.forEach { option ->
                    option.spell?.let { spell ->
                        val spellExpandedKey = "${selectionKey}_${spell.index}"
                        val isSpellExpanded = expandedStates.getOrPut(spellExpandedKey) { false }
                        UnifiedSpellListItem(
                            spell = spell,
                            actionType = SpellActionType.ADD,
                            onActionClick = {
                                if (canAddMore) {
                                    val newList = currentlySelectedIds.toMutableList().apply { add(spell.index) }
                                    onSelectionUpdated(selectionKey, ChoiceResult.Spells(newList))
                                }
                            },
                            isActionEnabled = canAddMore,
                            isExpanded = isSpellExpanded,
                            onToggleExpand = { expandedStates[spellExpandedKey] = !isSpellExpanded }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleActionRow(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WizardUiConfig.ACTION_ROW_BG_COLOR)
                .border(WizardUiConfig.SECTION_BORDER_WIDTH, WizardUiConfig.ACTION_ROW_BORDER_COLOR)
                .clickable(onClick = onToggle)
                .padding(
                    horizontal = WizardUiConfig.ACTION_ROW_PADDING_HORIZONTAL,
                    vertical = WizardUiConfig.ACTION_ROW_PADDING_VERTICAL
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontWeight = WizardUiConfig.ACTION_ROW_FONT_WEIGHT,
                fontSize = WizardUiConfig.ACTION_ROW_FONT_SIZE,
                color = WizardUiConfig.ACTION_ROW_TEXT_COLOR
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Развернуть/Свернуть",
                tint = WizardUiConfig.ACTION_ROW_TEXT_COLOR
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = WizardUiConfig.ACTION_ROW_CONTENT_TOP_PADDING)) {
                content()
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/SpellSelectionComponents.kt