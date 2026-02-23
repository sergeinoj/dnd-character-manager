package com.dnd.app.ui.components.shared.spell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.dnd.app.util.stripHtml

enum class SpellActionType { ADD, REMOVE }

data class UnifiedSpellListItemStyle(
    val container: ContainerStyle,
    val header: HeaderStyle,
    val action: ActionButtonStyle,
    val details: DetailsStyle
) {
    data class ContainerStyle(
        val verticalPadding: Dp,
        val backgroundColor: Color,
        val borderWidth: Dp,
        val borderColor: Color
    )

    data class HeaderStyle(
        val paddingHorizontal: Dp,
        val paddingVertical: Dp,
        val titleFontWeight: FontWeight,
        val titleFontSize: TextUnit,
        val subtitleFontSize: TextUnit,
        val subtitleTextColor: Color,
        val textActionSpacing: Dp
    )

    data class ActionButtonStyle(
        val size: Dp,
        val backgroundColor: Color,
        val borderWidth: Dp,
        val borderColor: Color,
        val iconAddTint: Color,
        val iconRemoveTint: Color,
        val cornerRadius: Dp,
        val lockedIconTint: Color,
        val lockedIconSize: Dp
    )

    data class DetailsStyle(
        val backgroundColor: Color,
        val paddingHorizontal: Dp,
        val paddingVertical: Dp,
        val dividerPadding: Dp,
        val fontSize: TextUnit,
        val labelWidth: Dp
    )
}

@Composable
fun UnifiedSpellListItem(
    name: String,
    level: Int,
    school: String,
    castingTime: String,
    range: String,
    components: String,
    duration: String,
    description: String,
    actionType: SpellActionType,
    onActionClick: () -> Unit,
    isActionEnabled: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    style: UnifiedSpellListItemStyle,
    modifier: Modifier = Modifier
) {
    val isDarkContainer = style.container.backgroundColor.luminance() < 0.45f
    val headerTextColor = if (isDarkContainer) Color(0xFFF2F2F2) else Color(0xFF121212)
    val subtitleTextColor = if (isDarkContainer) Color(0xFFBDBDBD) else style.header.subtitleTextColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = style.container.verticalPadding)
            .background(style.container.backgroundColor)
            .border(style.container.borderWidth, style.container.borderColor)
            .clickable(onClick = onToggleExpand)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = style.header.paddingHorizontal,
                    vertical = style.header.paddingVertical
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = name,
                    fontWeight = style.header.titleFontWeight,
                    fontSize = style.header.titleFontSize,
                    color = headerTextColor
                )
                val levelText = if (level == 0) "\u0417\u0430\u0433\u043E\u0432\u043E\u0440" else "$level \u0443\u0440\u043E\u0432\u0435\u043D\u044C"
                Text(
                    text = "$levelText, $school",
                    fontSize = style.header.subtitleFontSize,
                    color = subtitleTextColor
                )
            }

            if (isActionEnabled) {
                Box(
                    modifier = Modifier
                        .size(style.action.size)
                        .background(
                            style.action.backgroundColor,
                            RoundedCornerShape(style.action.cornerRadius)
                        )
                        .border(
                            style.action.borderWidth,
                            style.action.borderColor,
                            RoundedCornerShape(style.action.cornerRadius)
                        )
                        .clickable(onClick = onActionClick),
                    contentAlignment = Alignment.Center
                ) {
                    val (icon, tint) = when (actionType) {
                        SpellActionType.ADD -> Icons.Default.Add to style.action.iconAddTint
                        SpellActionType.REMOVE -> Icons.Default.Remove to style.action.iconRemoveTint
                    }
                    Icon(icon, contentDescription = actionType.name, tint = tint)
                }
            } else {
                Box(
                    modifier = Modifier.size(style.action.size),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        "Granted spell",
                        tint = style.action.lockedIconTint,
                        modifier = Modifier.size(style.action.lockedIconSize)
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            SpellDetailsBlock(
                castingTime = castingTime,
                range = range,
                components = components,
                duration = duration,
                description = description,
                style = style.details
            )
        }
    }
}

@Composable
fun SpellDetailsBlock(
    castingTime: String,
    range: String,
    components: String,
    duration: String,
    description: String,
    style: UnifiedSpellListItemStyle.DetailsStyle
) {
    val detailsTextColor = if (style.backgroundColor.luminance() < 0.45f) Color(0xFFF2F2F2) else Color(0xFF212121)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.backgroundColor)
            .padding(
                horizontal = style.paddingHorizontal,
                vertical = style.paddingVertical
            )
    ) {
        SpellDetailRow("\u0412\u0440\u0435\u043C\u044F:", castingTime, style, detailsTextColor)
        SpellDetailRow("\u0414\u0438\u0441\u0442\u0430\u043D\u0446\u0438\u044F:", range, style, detailsTextColor)
        SpellDetailRow("\u041A\u043E\u043C\u043F\u043E\u043D\u0435\u043D\u0442\u044B:", components, style, detailsTextColor)
        SpellDetailRow("\u0414\u043B\u0438\u0442\u0435\u043B\u044C\u043D\u043E\u0441\u0442\u044C:", duration, style, detailsTextColor)
        Divider(modifier = Modifier.padding(vertical = style.dividerPadding))
        Text(
            text = description.stripHtml(),
            fontSize = style.fontSize,
            color = detailsTextColor
        )
    }
}

@Composable
private fun SpellDetailRow(
    label: String,
    value: String,
    style: UnifiedSpellListItemStyle.DetailsStyle,
    textColor: Color
) {
    if (value.isNotBlank()) {
        Row {
            Text(
                text = label,
                fontSize = style.fontSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.width(style.labelWidth)
            )
            Text(
                text = value,
                fontSize = style.fontSize,
                color = textColor
            )
        }
    }
}
