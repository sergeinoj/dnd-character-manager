// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\components\WizardUiConfig.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


object WizardUiConfig {



    val WIZARD_STEP_SCREEN_PADDING: Dp = 8.dp

    val WIZARD_STEP_SECTION_GAP: Dp = 6.dp



    val SECTION_BORDER_WIDTH: Dp = 1.dp

    val SECTION_BORDER_COLOR: Color = Color(0xFF424242)

    val SECTION_HEADER_BG_COLOR: Color = Color(0xFF424242)

    val SECTION_HEADER_TEXT_COLOR: Color = Color.White

    val SECTION_HEADER_FONT_SIZE: TextUnit = 16.sp

    val SECTION_HEADER_FONT_WEIGHT: FontWeight = FontWeight.Normal

    val SECTION_HEADER_PADDING_HORIZONTAL: Dp = 8.dp

    val SECTION_HEADER_PADDING_VERTICAL: Dp = 6.dp

    val SECTION_CONTENT_BG_COLOR: Color = Color(0xFFC0C0C0)

    val SECTION_CONTENT_PADDING: Dp = 8.dp



    val CHOICE_BLOCK_TOP_PADDING: Dp = 4.dp

    val CHOICE_ITEM_VERTICAL_SPACING: Dp = 4.dp

    val DROPDOWN_HEIGHT: Dp = 38.dp

    val DROPDOWN_BG_COLOR: Color = Color.White

    val DROPDOWN_BORDER_COLOR: Color = Color.Gray

    val DROPDOWN_PADDING_HORIZONTAL: Dp = 8.dp

    val DROPDOWN_PLACEHOLDER_TEXT_COLOR: Color = Color(0xFF212121)

    val DROPDOWN_SELECTED_TEXT_COLOR: Color = Color.Black

    val DROPDOWN_FONT_SIZE: TextUnit = 14.sp

    val DROPDOWN_ARROW_TINT: Color = Color.Black

    val DROPDOWN_MENU_ITEM_FONT_SIZE: TextUnit = 14.sp



    val SPELL_GROUP_SUBGROUP_SPACING: Dp = 6.dp

    val SPELL_GROUP_ACTION_ROW_SPACING: Dp = 2.dp


    val ACTION_ROW_BG_COLOR: Color = Color.White

    val ACTION_ROW_TEXT_COLOR: Color = Color.Black

    val ACTION_ROW_BORDER_COLOR: Color = Color.Gray

    val ACTION_ROW_FONT_SIZE: TextUnit = 14.sp

    val ACTION_ROW_FONT_WEIGHT: FontWeight = FontWeight.Normal

    val ACTION_ROW_PADDING_HORIZONTAL: Dp = 8.dp

    val ACTION_ROW_PADDING_VERTICAL: Dp = 6.dp

    val ACTION_ROW_CONTENT_TOP_PADDING: Dp = 2.dp


    val SPELL_LIST_ITEM_SPACING: Dp = 2.dp

    val SPELL_LIST_ITEM_VERTICAL_PADDING: Dp = 2.dp

    val SPELL_ITEM_BG_COLOR: Color = Color(0xFFE0E0E0)

    val SPELL_ITEM_BORDER_COLOR: Color = Color(0xFF888888)

    val SPELL_ITEM_PADDING_HORIZONTAL: Dp = 8.dp

    val SPELL_ITEM_PADDING_VERTICAL: Dp = 6.dp

    val SPELL_ITEM_TITLE_FONT_SIZE: TextUnit = 14.sp

    val SPELL_ITEM_TITLE_FONT_WEIGHT: FontWeight = FontWeight.Bold

    val SPELL_ITEM_SUBTITLE_FONT_SIZE: TextUnit = 11.sp

    val SPELL_ITEM_SUBTITLE_TEXT_COLOR: Color = Color(0xFF212121)

    val SPELL_ITEM_TEXT_ACTION_SPACING: Dp = 8.dp


    val SPELL_ITEM_ACTION_BUTTON_SIZE: Dp = 28.dp

    val SPELL_ITEM_ACTION_BUTTON_BG: Color = Color.White

    val SPELL_ITEM_ACTION_BUTTON_BORDER_COLOR: Color = Color.Gray

    val SPELL_ITEM_ACTION_ADD_TINT_ENABLED: Color = Color.Black

    val SPELL_ITEM_ACTION_ADD_TINT_DISABLED: Color = Color(0xFF616161)

    val SPELL_ITEM_ACTION_REMOVE_TINT_ENABLED: Color = Color.Red

    val SPELL_ITEM_ACTION_REMOVE_TINT_DISABLED: Color = Color(0xFF616161)


    val SPELL_ITEM_DETAILS_BG_COLOR: Color = Color(0xFFF5F5F5)

    val SPELL_ITEM_DETAILS_PADDING_HORIZONTAL: Dp = 8.dp

    val SPELL_ITEM_DETAILS_PADDING_VERTICAL: Dp = 6.dp

    val SPELL_ITEM_DETAILS_DIVIDER_PADDING: Dp = 4.dp

    val SPELL_ITEM_DETAILS_FONT_SIZE: TextUnit = 12.sp

    val SPELL_ITEM_DETAILS_LABEL_WIDTH: Dp = 90.dp



    val EQUIP_MODE_SELECTOR_HEIGHT: Dp = 40.dp
    val EQUIP_MODE_SELECTOR_PADDING: Dp = 8.dp
    val EQUIP_MODE_SELECTOR_CORNER_RADIUS: Dp = 8.dp
    val EQUIP_MODE_SELECTOR_BORDER_WIDTH: Dp = 1.dp
    val EQUIP_MODE_SELECTOR_FONT_SIZE: TextUnit = 14.sp
    val EQUIP_MODE_SELECTOR_FONT_WEIGHT: FontWeight = FontWeight.SemiBold

    val EQUIP_GROUP_HEADER_BG_COLOR: Color = Color(0xFFCCCCCC)
    val EQUIP_GROUP_HEADER_V_PADDING: Dp = 4.dp
    val EQUIP_GROUP_HEADER_FONT_WEIGHT: FontWeight = FontWeight.SemiBold
    val EQUIP_GROUP_HEADER_FONT_SIZE: TextUnit = 14.sp


    val EQUIP_FEATURE_RENDERER_SPACING: Dp = 8.dp

    val EQUIP_CHOICE_ROW_SPACING: Dp = 8.dp

    val EQUIP_CARD_BG: Color = Color(0xFFE0E0E0)
    val EQUIP_CARD_BORDER_COLOR: Color = Color(0xFF888888)
    val EQUIP_CARD_CORNER_RADIUS: Dp = 2.dp
    val EQUIP_CARD_BORDER_WIDTH: Dp = 1.dp
    val EQUIP_CARD_PADDING: Dp = 8.dp
    val EQUIP_CARD_OR_TEXT_SIZE: TextUnit = 14.sp

    val EQUIP_CARD_TITLE_FONT_SIZE: TextUnit = 13.sp
    val EQUIP_CARD_TITLE_FONT_WEIGHT: FontWeight = FontWeight.Bold
    val EQUIP_CARD_ITEM_LIST_SPACER: Dp = 4.dp
    val EQUIP_CARD_ITEM_LIST_SIZE: TextUnit = 11.sp
    val EQUIP_CARD_ITEM_LIST_LINE_HEIGHT: TextUnit = 14.sp
    val EQUIP_DROPDOWN_IN_CARD_TITLE_BOTTOM_PADDING: Dp = 8.dp

    val EQUIP_SELECT_BTN_HEIGHT: Dp = 32.dp
    val EQUIP_SELECT_BTN_BORDER_WIDTH: Dp = 1.dp
    val EQUIP_SELECT_BTN_BG: Color = Color.White
    val EQUIP_SELECT_BTN_BORDER_COLOR: Color = Color.Gray
    val EQUIP_SELECT_BTN_TEXT_COLOR: Color = Color.Black
    val EQUIP_SELECT_BTN_FONT_WEIGHT: FontWeight = FontWeight.SemiBold
    val EQUIP_SELECT_BTN_ACTIVE_BG: Color = Color(0xFF424242)
    val EQUIP_SELECT_BTN_ACTIVE_TEXT_COLOR: Color = Color.White



    val FONT_SIZE_CONTENT: TextUnit = 14.sp
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\components\WizardUiConfig.kt
