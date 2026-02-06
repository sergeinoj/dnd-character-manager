// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/UnifiedSpellListItem.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.Spell
import com.dnd.app.util.stripHtml

enum class SpellActionType { ADD, REMOVE }

@Composable
fun UnifiedSpellListItem(
    spell: Spell,
    actionType: SpellActionType,
    onActionClick: () -> Unit,
    isActionEnabled: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WizardUiConfig.SPELL_LIST_ITEM_VERTICAL_PADDING)
            .background(WizardUiConfig.SPELL_ITEM_BG_COLOR)
            .border(WizardUiConfig.SECTION_BORDER_WIDTH, WizardUiConfig.SPELL_ITEM_BORDER_COLOR)
            .clickable(onClick = onToggleExpand)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = WizardUiConfig.SPELL_ITEM_PADDING_HORIZONTAL,
                    vertical = WizardUiConfig.SPELL_ITEM_PADDING_VERTICAL
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spell.name,
                    fontWeight = WizardUiConfig.SPELL_ITEM_TITLE_FONT_WEIGHT,
                    fontSize = WizardUiConfig.SPELL_ITEM_TITLE_FONT_SIZE,
                    color = Color.Black
                )
                val levelText = if (spell.level == 0) "Заговор" else "${spell.level} уровень"
                Text(
                    text = "$levelText, ${spell.school}",
                    fontSize = WizardUiConfig.SPELL_ITEM_SUBTITLE_FONT_SIZE,
                    color = WizardUiConfig.SPELL_ITEM_SUBTITLE_TEXT_COLOR
                )
            }
            Spacer(Modifier.width(WizardUiConfig.SPELL_ITEM_TEXT_ACTION_SPACING))
            Box(
                modifier = Modifier
                    .size(WizardUiConfig.SPELL_ITEM_ACTION_BUTTON_SIZE)
                    .background(WizardUiConfig.SPELL_ITEM_ACTION_BUTTON_BG, RoundedCornerShape(2.dp))
                    .border(WizardUiConfig.SECTION_BORDER_WIDTH, WizardUiConfig.SPELL_ITEM_ACTION_BUTTON_BORDER_COLOR, RoundedCornerShape(2.dp))
                    .run {
                        if (isActionEnabled) clickable(onClick = onActionClick) else this
                    },
                contentAlignment = Alignment.Center
            ) {
                val (icon, color) = when(actionType) {
                    SpellActionType.ADD -> Icons.Default.Add to if(isActionEnabled) WizardUiConfig.SPELL_ITEM_ACTION_ADD_TINT_ENABLED else WizardUiConfig.SPELL_ITEM_ACTION_ADD_TINT_DISABLED
                    SpellActionType.REMOVE -> Icons.Default.Remove to if(isActionEnabled) WizardUiConfig.SPELL_ITEM_ACTION_REMOVE_TINT_ENABLED else WizardUiConfig.SPELL_ITEM_ACTION_REMOVE_TINT_DISABLED
                }
                Icon(icon, contentDescription = actionType.name, tint = color)
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WizardUiConfig.SPELL_ITEM_DETAILS_BG_COLOR)
                    .padding(
                        horizontal = WizardUiConfig.SPELL_ITEM_DETAILS_PADDING_HORIZONTAL,
                        vertical = WizardUiConfig.SPELL_ITEM_DETAILS_PADDING_VERTICAL
                    )
            ) {
                SpellDetailRow("Время:", spell.castingTime)
                SpellDetailRow("Дистанция:", spell.range)
                SpellDetailRow("Компоненты:", spell.components)
                SpellDetailRow("Длительность:", spell.duration)
                Divider(modifier = Modifier.padding(vertical = WizardUiConfig.SPELL_ITEM_DETAILS_DIVIDER_PADDING))
                Text(
                    spell.description.stripHtml(),
                    fontSize = WizardUiConfig.SPELL_ITEM_DETAILS_FONT_SIZE,
                    lineHeight = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun SpellDetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row {
            Text(
                text = label,
                fontSize = WizardUiConfig.SPELL_ITEM_DETAILS_FONT_SIZE,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(WizardUiConfig.SPELL_ITEM_DETAILS_LABEL_WIDTH)
            )
            Text(value, fontSize = WizardUiConfig.SPELL_ITEM_DETAILS_FONT_SIZE)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/UnifiedSpellListItem.kt