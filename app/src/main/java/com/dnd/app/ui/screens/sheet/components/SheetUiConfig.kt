package com.dnd.app.ui.screens.sheet.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SheetUiConfig {
    val WIZARD_STEP_SCREEN_PADDING: Dp = 8.dp
    val WIZARD_STEP_SECTION_GAP: Dp = 6.dp

    val SECTION_BORDER_WIDTH: Dp = 1.dp
    val SECTION_BORDER_COLOR: Color = Color(0xFF424242)
    val SECTION_CONTENT_BG_COLOR: Color = Color(0xFFC0C0C0)
    val SECTION_CONTENT_PADDING: Dp = 8.dp

    val SECTION_HEADER_BG_COLOR: Color = Color(0xFF424242)
    val SECTION_HEADER_PADDING_HORIZONTAL: Dp = 8.dp
    val SECTION_HEADER_PADDING_VERTICAL: Dp = 6.dp
    val SECTION_HEADER_TEXT_COLOR: Color = Color.White
    val SECTION_HEADER_FONT_SIZE: TextUnit = 16.sp
    val SECTION_HEADER_FONT_WEIGHT: FontWeight = FontWeight.Normal
}
