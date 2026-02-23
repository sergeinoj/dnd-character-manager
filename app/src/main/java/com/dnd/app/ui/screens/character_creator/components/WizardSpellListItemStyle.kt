package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.components.shared.spell.UnifiedSpellListItemStyle

val WizardSpellListItemStyle = UnifiedSpellListItemStyle(
    container = UnifiedSpellListItemStyle.ContainerStyle(
        verticalPadding = WizardUiConfig.SPELL_LIST_ITEM_VERTICAL_PADDING,
        backgroundColor = WizardUiConfig.SPELL_ITEM_BG_COLOR,
        borderWidth = WizardUiConfig.SECTION_BORDER_WIDTH,
        borderColor = WizardUiConfig.SPELL_ITEM_BORDER_COLOR
    ),
    header = UnifiedSpellListItemStyle.HeaderStyle(
        paddingHorizontal = WizardUiConfig.SPELL_ITEM_PADDING_HORIZONTAL,
        paddingVertical = WizardUiConfig.SPELL_ITEM_PADDING_VERTICAL,
        titleFontWeight = WizardUiConfig.SPELL_ITEM_TITLE_FONT_WEIGHT,
        titleFontSize = WizardUiConfig.SPELL_ITEM_TITLE_FONT_SIZE,
        subtitleFontSize = WizardUiConfig.SPELL_ITEM_SUBTITLE_FONT_SIZE,
        subtitleTextColor = WizardUiConfig.SPELL_ITEM_SUBTITLE_TEXT_COLOR,
        textActionSpacing = WizardUiConfig.SPELL_ITEM_TEXT_ACTION_SPACING
    ),
    action = UnifiedSpellListItemStyle.ActionButtonStyle(
        size = WizardUiConfig.SPELL_ITEM_ACTION_BUTTON_SIZE,
        backgroundColor = WizardUiConfig.SPELL_ITEM_ACTION_BUTTON_BG,
        borderWidth = WizardUiConfig.SECTION_BORDER_WIDTH,
        borderColor = WizardUiConfig.SPELL_ITEM_ACTION_BUTTON_BORDER_COLOR,
        iconAddTint = WizardUiConfig.SPELL_ITEM_ACTION_ADD_TINT_ENABLED,
        iconRemoveTint = WizardUiConfig.SPELL_ITEM_ACTION_REMOVE_TINT_ENABLED,
        cornerRadius = 2.dp,
        lockedIconTint = Color.DarkGray,
        lockedIconSize = 20.dp
    ),
    details = UnifiedSpellListItemStyle.DetailsStyle(
        backgroundColor = WizardUiConfig.SPELL_ITEM_DETAILS_BG_COLOR,
        paddingHorizontal = WizardUiConfig.SPELL_ITEM_DETAILS_PADDING_HORIZONTAL,
        paddingVertical = WizardUiConfig.SPELL_ITEM_DETAILS_PADDING_VERTICAL,
        dividerPadding = WizardUiConfig.SPELL_ITEM_DETAILS_DIVIDER_PADDING,
        fontSize = WizardUiConfig.SPELL_ITEM_DETAILS_FONT_SIZE,
        labelWidth = WizardUiConfig.SPELL_ITEM_DETAILS_LABEL_WIDTH
    )
)
