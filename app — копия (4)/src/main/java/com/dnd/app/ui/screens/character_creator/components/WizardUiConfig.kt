// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/WizardUiConfig.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Центральный объект для управления внешним видом всех компонентов в Мастере Создания Персонажа.
 */
object WizardUiConfig {

    // === ОБЩИЕ ДЛЯ ШАГА (ЭКРАНА) ===
    /** Внешний отступ для всего экрана/шага (например, RaceStep, ClassStep). */
    val WIZARD_STEP_SCREEN_PADDING: Dp = 0.dp
    /** Вертикальный промежуток между основными секциями (карточками) на экране. */
    val WIZARD_STEP_SECTION_GAP: Dp = 6.dp

    // === СЕКЦИИ (FlatWizardSection) ===
    /** Толщина рамки для основных секций. */
    val SECTION_BORDER_WIDTH: Dp = 1.dp
    /** Цвет рамки для основных секций. */
    val SECTION_BORDER_COLOR: Color = Color(0xFF424242)
    /** Цвет фона для заголовков секций (например, "Раса", "Класс"). */
    val SECTION_HEADER_BG_COLOR: Color = Color(0xFF424242)
    /** Цвет текста для заголовков секций. */
    val SECTION_HEADER_TEXT_COLOR: Color = Color.White
    /** Размер шрифта для заголовков секций. */
    val SECTION_HEADER_FONT_SIZE: TextUnit = 16.sp
    /** Насыщенность шрифта для заголовков секций. */
    val SECTION_HEADER_FONT_WEIGHT: FontWeight = FontWeight.Normal
    /** Горизонтальный отступ внутри заголовка секции. */
    val SECTION_HEADER_PADDING_HORIZONTAL: Dp = 8.dp
    /** Вертикальный отступ внутри заголовка секции. */
    val SECTION_HEADER_PADDING_VERTICAL: Dp = 6.dp
    /** Цвет фона для основной контентной части секций. */
    val SECTION_CONTENT_BG_COLOR: Color = Color(0xFFC0C0C0)
    /** Внутренний отступ для контентной части секций. */
    val SECTION_CONTENT_PADDING: Dp = 8.dp

    // === БЛОКИ ВЫБОРА (Dropdowns, Info Text) ===
    /** Отступ сверху для контейнера блока выбора (FeatureChoiceBlock). */
    val CHOICE_BLOCK_TOP_PADDING: Dp = 4.dp
    /** Вертикальный отступ между элементами выбора (например, между двумя выпадающими списками навыков). */
    val CHOICE_ITEM_VERTICAL_SPACING: Dp = 4.dp
    /** Высота выпадающего списка (SmartDropdown). */
    val DROPDOWN_HEIGHT: Dp = 38.dp
    /** Цвет фона выпадающего списка. */
    val DROPDOWN_BG_COLOR: Color = Color.White
    /** Цвет рамки выпадающего списка. */
    val DROPDOWN_BORDER_COLOR: Color = Color.Gray
    /** Горизонтальный отступ внутри выпадающего списка. */
    val DROPDOWN_PADDING_HORIZONTAL: Dp = 8.dp
    /** Цвет текста-заглушки ("Выберите вариант..."). */
    val DROPDOWN_PLACEHOLDER_TEXT_COLOR: Color = Color.Gray
    /** Цвет текста для выбранного элемента. */
    val DROPDOWN_SELECTED_TEXT_COLOR: Color = Color.Black
    /** Размер шрифта текста в выпадающем списке. */
    val DROPDOWN_FONT_SIZE: TextUnit = 14.sp
    /** Цвет стрелки в выпадающем списке. */
    val DROPDOWN_ARROW_TINT: Color = Color.Black
    /** Размер шрифта для элементов в меню выпадающего списка. */
    val DROPDOWN_MENU_ITEM_FONT_SIZE: TextUnit = 14.sp

    // === ВЫБОР ЗАКЛИНАНИЙ ===
    /** Вертикальный отступ между вложенными группами заклинаний ("Заговоры" и "Заклинания 1 уровня"). */
    val SPELL_GROUP_SUBGROUP_SPACING: Dp = 6.dp
    /** Вертикальный отступ между строками "Добавленные" и "Выбрать". */
    val SPELL_GROUP_ACTION_ROW_SPACING: Dp = 2.dp

    // --- Сворачиваемая строка ("Добавленные", "Выбрать") ---
    /** Цвет фона для сворачиваемой строки. */
    val ACTION_ROW_BG_COLOR: Color = Color.White
    /** Цвет текста для сворачиваемой строки. */
    val ACTION_ROW_TEXT_COLOR: Color = Color.Black
    /** Цвет рамки для сворачиваемой строки. */
    val ACTION_ROW_BORDER_COLOR: Color = Color.Gray
    /** Размер шрифта для сворачиваемой строки. */
    val ACTION_ROW_FONT_SIZE: TextUnit = 14.sp
    /** Насыщенность шрифта для сворачиваемой строки. */
    val ACTION_ROW_FONT_WEIGHT: FontWeight = FontWeight.Normal
    /** Горизонтальный отступ внутри сворачиваемой строки. */
    val ACTION_ROW_PADDING_HORIZONTAL: Dp = 8.dp
    /** Вертикальный отступ внутри сворачиваемой строки. */
    val ACTION_ROW_PADDING_VERTICAL: Dp = 6.dp
    /** Отступ сверху для раскрывающегося списка заклинаний. */
    val ACTION_ROW_CONTENT_TOP_PADDING: Dp = 2.dp

    // --- Элемент списка заклинаний (UnifiedSpellListItem) ---
    /** Вертикальный отступ между элементами в списке заклинаний. */
    val SPELL_LIST_ITEM_SPACING: Dp = 2.dp
    /** Вертикальный отступ для самого элемента списка заклинаний (для рамки). */
    val SPELL_LIST_ITEM_VERTICAL_PADDING: Dp = 2.dp
    /** Цвет фона для элемента списка заклинаний. */
    val SPELL_ITEM_BG_COLOR: Color = Color(0xFFE0E0E0)
    /** Цвет рамки для элемента списка заклинаний. */
    val SPELL_ITEM_BORDER_COLOR: Color = Color(0xFF888888)
    /** Горизонтальный отступ внутри элемента списка заклинаний. */
    val SPELL_ITEM_PADDING_HORIZONTAL: Dp = 8.dp
    /** Вертикальный отступ внутри элемента списка заклинаний. */
    val SPELL_ITEM_PADDING_VERTICAL: Dp = 6.dp
    /** Размер шрифта для названия заклинания. */
    val SPELL_ITEM_TITLE_FONT_SIZE: TextUnit = 14.sp
    /** Насыщенность шрифта для названия заклинания. */
    val SPELL_ITEM_TITLE_FONT_WEIGHT: FontWeight = FontWeight.Bold
    /** Размер шрифта для подзаголовка (уровень, школа). */
    val SPELL_ITEM_SUBTITLE_FONT_SIZE: TextUnit = 11.sp
    /** Цвет текста для подзаголовка. */
    val SPELL_ITEM_SUBTITLE_TEXT_COLOR: Color = Color.DarkGray
    /** Промежуток между текстом заклинания и кнопкой действия (+/-). */
    val SPELL_ITEM_TEXT_ACTION_SPACING: Dp = 8.dp

    // --- Кнопка действия (+/-) в списке заклинаний ---
    /** Размер кнопки (+/-). */
    val SPELL_ITEM_ACTION_BUTTON_SIZE: Dp = 28.dp
    /** Цвет фона кнопки (+/-). */
    val SPELL_ITEM_ACTION_BUTTON_BG: Color = Color.White
    /** Цвет рамки кнопки (+/-). */
    val SPELL_ITEM_ACTION_BUTTON_BORDER_COLOR: Color = Color.Gray
    /** Цвет иконки "+" в активном состоянии. */
    val SPELL_ITEM_ACTION_ADD_TINT_ENABLED: Color = Color.DarkGray
    /** Цвет иконки "+" в неактивном состоянии. */
    val SPELL_ITEM_ACTION_ADD_TINT_DISABLED: Color = Color.LightGray
    /** Цвет иконки "-" в активном состоянии. */
    val SPELL_ITEM_ACTION_REMOVE_TINT_ENABLED: Color = Color.Red
    /** Цвет иконки "-" в неактивном состоянии. */
    val SPELL_ITEM_ACTION_REMOVE_TINT_DISABLED: Color = Color.LightGray

    // --- Детали заклинания (при раскрытии) ---
    /** Цвет фона для детальной информации о заклинании. */
    val SPELL_ITEM_DETAILS_BG_COLOR: Color = Color(0xFFF5F5F5)
    /** Горизонтальный отступ в блоке с деталями заклинания. */
    val SPELL_ITEM_DETAILS_PADDING_HORIZONTAL: Dp = 8.dp
    /** Вертикальный отступ в блоке с деталями заклинания. */
    val SPELL_ITEM_DETAILS_PADDING_VERTICAL: Dp = 6.dp
    /** Вертикальный отступ для разделителя в деталях заклинания. */
    val SPELL_ITEM_DETAILS_DIVIDER_PADDING: Dp = 4.dp
    /** Размер шрифта для текста в деталях заклинания. */
    val SPELL_ITEM_DETAILS_FONT_SIZE: TextUnit = 12.sp
    /** Ширина колонки для меток ("Время:", "Дистанция:") в деталях заклинания. */
    val SPELL_ITEM_DETAILS_LABEL_WIDTH: Dp = 90.dp

    // === ТИПОГРАФИКА (ОБЩАЯ) ===
    /** Базовый размер шрифта для контента (описания фич и т.д.). */
    val FONT_SIZE_CONTENT: TextUnit = 14.sp
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/WizardUiConfig.kt