// Имя файла: ui/screens/character_sheet/tabs/StatsTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.model.Stats
import com.dnd.app.ui.theme.DndBonusGreen
import com.dnd.app.ui.theme.DndMalusRed

// ==========================================
// ===== НАСТРОЙКИ (LAYOUT SETTINGS) ========
// ==========================================
private object LayoutSettings {
    // Веса колонок (ширина)
    // Левая колонка теперь фиксирована математически (квадраты), поэтому здесь веса правой части

    // Вертикальные веса правой колонки (Сумма не обязана быть 1.0, это пропорции)
    const val WEIGHT_HEADER = 0.18f    // Шапка (Имя/Класс)
    const val WEIGHT_COMBAT = 0.20f    // КД и БМ
    const val WEIGHT_HP = 0.22f        // Здоровье
    const val WEIGHT_SEC_STATS = 0.14f // Инициатива/Скорость
    const val WEIGHT_MONEY = 0.16f     // Деньги
    const val WEIGHT_REST = 0.18f      // Отдых

    // Размеры шрифтов
    val FONT_STAT_NAME = 10.sp
    val FONT_STAT_VALUE = 32.sp
    val FONT_MONEY_LABEL = 12.sp
    val FONT_INPUT = 14.sp

    // Отступы
    val GAP_DEFAULT = 4.dp
    val GAP_SMALL = 2.dp
}

@Composable
fun StatsTab(
    character: CharacterDomain,
    calculator: DndCalculator,
    onHpChange: (Int) -> Unit,
    // ВАЖНО: Теперь это обязательный параметр, а не nullable ViewModel
    onMoneyChange: (String, Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(LayoutSettings.GAP_DEFAULT)
    ) {
        val totalHeight = maxHeight
        // Расчет размера квадрата: (Высота - Отступы) / 6 элементов
        val statSize = (totalHeight - 24.dp) / 6

        Row(modifier = Modifier.fillMaxSize()) {
            // --- ЛЕВАЯ КОЛОНКА ---
            Column(
                modifier = Modifier
                    .width(statSize)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val itemMod = Modifier.height(statSize).fillMaxWidth()
                StatBoxStrict(itemMod, "Сила", character.stats.strength, calculator)
                StatBoxStrict(itemMod, "Ловкость", character.stats.dexterity, calculator)
                StatBoxStrict(itemMod, "Телос.", character.stats.constitution, calculator)
                StatBoxStrict(itemMod, "Интеллект", character.stats.intelligence, calculator)
                StatBoxStrict(itemMod, "Мудрость", character.stats.wisdom, calculator)
                StatBoxStrict(itemMod, "Харизма", character.stats.charisma, calculator)
            }

            Spacer(modifier = Modifier.width(LayoutSettings.GAP_DEFAULT))

            // --- ПРАВАЯ КОЛОНКА ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. ШАПКА
                Column(
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_HEADER),
                    verticalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f).fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(character.name.ifBlank{"Герой"}, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                    }

                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).padding(6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) { Text("Плут", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                        Box(
                            modifier = Modifier.width(statSize).fillMaxHeight().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(character.level.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }

                // 2. БОЕВОЙ БЛОК
                Row(
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_COMBAT).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                ) {
                    val ac = calculator.calculateBaseAC(calculator.calculateModifier(character.stats.dexterity))
                    val prof = calculator.calculateProficiencyBonus(character.level)
                    SquareInfoBox("КД", ac.toString(), Modifier.weight(1f).fillMaxHeight())
                    SquareInfoBox("БМ", "+$prof", Modifier.weight(1f).fillMaxHeight(), isBonus = true)
                }

                // 3. ЗДОРОВЬЕ
                HealthControlWidget(
                    current = character.hpCurrent,
                    max = character.hpMax,
                    onChange = onHpChange,
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_HP)
                )

                // 4. ВТОРИЧНЫЕ СТАТЫ
                Row(
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_SEC_STATS).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                ) {
                    val init = calculator.calculateModifier(character.stats.dexterity)
                    val passPerc = 10 + calculator.calculateModifier(character.stats.wisdom)
                    CompactInfoBox("Инициатива", calculator.formatModifier(init), Modifier.weight(1f).fillMaxHeight())
                    CompactInfoBox("Пас. вним.", passPerc.toString(), Modifier.weight(1f).fillMaxHeight())
                    CompactInfoBox("Скорость", "30", Modifier.weight(1f).fillMaxHeight())
                }

                // 5. ДЕНЬГИ
                MoneyWidgetCalculator(
                    stats = character.stats,
                    onUpdate = onMoneyChange, // Передаем callback напрямую
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_MONEY)
                )

                // 6. ОТДЫХ
                Column(
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_REST),
                    verticalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_SMALL)
                ) {
                    Text("Отдых", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                    ) {
                        RestButton("Короткий", Modifier.weight(1f).fillMaxHeight())
                        RestButton("Длинный", Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

// --- КОМПОНЕНТЫ ---

@Composable
fun StatBoxStrict(modifier: Modifier, name: String, value: Int, calculator: DndCalculator) {
    val mod = calculator.calculateModifier(value)
    Column(
        modifier = modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontSize = LayoutSettings.FONT_STAT_NAME, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = calculator.formatModifier(mod),
                fontSize = LayoutSettings.FONT_STAT_VALUE,
                fontWeight = FontWeight.Black,
                color = if (mod > 0) DndBonusGreen else if (mod < 0) DndMalusRed else Color.Black,
                textAlign = TextAlign.Center
            )
        }
        Box(modifier = Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp)) {
            Text(value.toString(), fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MoneyWidgetCalculator(stats: Stats, onUpdate: (String, Int) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        MoneyRowCalculator("ЗМ", stats.gold, onUpdate)
        MoneyRowCalculator("СМ", stats.silver, onUpdate)
        MoneyRowCalculator("ММ", stats.copper, onUpdate)
    }
}

@Composable
fun MoneyRowCalculator(label: String, currentValue: Int, onUpdate: (String, Int) -> Unit) {
    val type = when(label) { "ЗМ" -> "GP"; "СМ" -> "SP"; else -> "CP" }
    var calcValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            fontSize = LayoutSettings.FONT_MONEY_LABEL,
            modifier = Modifier.width(32.dp)
        )

        // ПОЛЕ 1: Абсолютный ввод (Текущее значение)
        BasicTextField(
            value = currentValue.toString(),
            onValueChange = { newValue ->
                if (newValue.isEmpty()) {
                    // Если стерли всё, считаем что хотим 0
                    val delta = 0 - currentValue
                    onUpdate(type, delta)
                } else if (newValue.all { it.isDigit() }) {
                    val newInt = newValue.toIntOrNull() ?: 0
                    val delta = newInt - currentValue
                    if (delta != 0) {
                        onUpdate(type, delta)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            textStyle = TextStyle(fontSize = LayoutSettings.FONT_INPUT, color = Color.Black),
            modifier = Modifier
                .weight(1f)
                .background(Color.White)
                .border(1.dp, Color.LightGray)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(Modifier.width(LayoutSettings.GAP_DEFAULT))

        // ПОЛЕ 2: Калькулятор
        BasicTextField(
            value = calcValue,
            onValueChange = { if (it.all { char -> char.isDigit() }) calcValue = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            textStyle = TextStyle(fontSize = LayoutSettings.FONT_INPUT, textAlign = TextAlign.Center),
            modifier = Modifier
                .width(40.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(2.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
                .padding(vertical = 2.dp)
        )

        Spacer(Modifier.width(LayoutSettings.GAP_DEFAULT))

        // КНОПКИ
        Row(horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_SMALL)) {
            Box(
                Modifier
                    .border(1.dp, Color.Gray)
                    .clickable {
                        val amount = calcValue.toIntOrNull() ?: 0
                        if (amount > 0) {
                            onUpdate(type, -amount)
                            calcValue = ""
                            focusManager.clearFocus()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) { Text("-", fontWeight = FontWeight.Bold) }

            Box(
                Modifier
                    .border(1.dp, Color.Gray)
                    .clickable {
                        val amount = calcValue.toIntOrNull() ?: 0
                        if (amount > 0) {
                            onUpdate(type, amount)
                            calcValue = ""
                            focusManager.clearFocus()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) { Text("+", fontWeight = FontWeight.Bold) }
        }
    }
}

// --- СТАРЫЕ КОМПОНЕНТЫ (Без изменений) ---
@Composable
fun SquareInfoBox(title: String, value: String, modifier: Modifier, isBonus: Boolean = false) {
    Column(
        modifier = modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 14.sp, color = Color.DarkGray)
        Text(value, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = if(isBonus) DndBonusGreen else Color.Black)
    }
}

@Composable
fun CompactInfoBox(title: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 9.sp, lineHeight = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HealthControlWidget(current: Int, max: Int, onChange: (Int) -> Unit, modifier: Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(Modifier.weight(0.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HealthButton("-1", DndMalusRed, Modifier.weight(1f)) { onChange(-1) }
            HealthButton("-10", DndMalusRed, Modifier.weight(1f)) { onChange(-10) }
        }
        Column(Modifier.weight(0.6f).fillMaxHeight().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Здоровье", fontSize = 12.sp, color = Color.DarkGray)
            Text("$current/$max", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(0.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HealthButton("+1", DndBonusGreen, Modifier.weight(1f)) { onChange(1) }
            HealthButton("+10", DndBonusGreen, Modifier.weight(1f)) { onChange(10) }
        }
    }
}

@Composable
fun HealthButton(text: String, textColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
    }
}

@Composable
fun RestButton(text: String, modifier: Modifier) {
    Box(modifier = modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).clickable{}, contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/StatsTab.kt