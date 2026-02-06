// Имя файла: ui/screens/character_sheet/tabs/SkillsTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.CharacterDomain

// Цвета
private val ColorProficientBg = Color(0xFFC8E6C9) // Светло-зеленый
private val ColorExpertiseBg = Color(0xFFF8BBD0)  // Светло-розовый
private val ColorNoneBg = Color(0xFFFFFFFF)       // Белый
private val ColorHeaderBg = Color(0xFFE0E0E0)     // Серый заголовок

@Composable
fun SkillsTab(
    character: CharacterDomain,
    calculator: DndCalculator
) {
    val profBonus = calculator.calculateProficiencyBonus(character.level)
    val stats = character.stats
    val skillsMap = character.skillProficiencies

    // Данные для колонок
    val strSkills = listOf("Атлетика")
    val dexSkills = listOf("Акробатика", "Ловкость рук", "Скрытность")
    val conSkills = emptyList<String>()
    val intSkills = listOf("Анализ", "История", "Магия", "Природа", "Религия")

    val wisSkills = listOf("Внимательность", "Выживание", "Медицина", "Проницательность", "Уход за животными")
    val chaSkills = listOf("Выступление", "Запугивание", "Обман", "Убеждение")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // --- ЛЕВАЯ КОЛОНКА ---
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AbilitySkillsCard("Сила", stats.strength, strSkills, skillsMap, profBonus, calculator)
            AbilitySkillsCard("Ловкость", stats.dexterity, dexSkills, skillsMap, profBonus, calculator)
            AbilitySkillsCard("Телосложение", stats.constitution, conSkills, skillsMap, profBonus, calculator)
            AbilitySkillsCard("Интеллект", stats.intelligence, intSkills, skillsMap, profBonus, calculator)
        }

        // --- ПРАВАЯ КОЛОНКА ---
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AbilitySkillsCard("Мудрость", stats.wisdom, wisSkills, skillsMap, profBonus, calculator)
            AbilitySkillsCard("Харизма", stats.charisma, chaSkills, skillsMap, profBonus, calculator)
        }
    }
}

@Composable
fun AbilitySkillsCard(
    abilityName: String,
    abilityScore: Int,
    skillNames: List<String>,
    proficiencies: Map<String, Int>,
    profBonus: Int,
    calculator: DndCalculator
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.LightGray)
            .background(Color.White)
    ) {
        // Заголовок
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorHeaderBg)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = abilityName,
                fontSize = 18.sp,
                color = Color(0xFF424242)
            )
        }

        Column(modifier = Modifier.padding(vertical = 2.dp)) {
            // 1. Спасбросок (Всегда отображаем)
            val saveKey = "Спасбросок ($abilityName)"
            val saveMult = proficiencies[saveKey] ?: 0

            SavingThrowRow("Спасбросок", abilityScore, saveMult, profBonus, calculator)

            // Линия-разделитель (Всегда отображаем)
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // 2. Обычные навыки
            skillNames.forEach { name ->
                val mult = proficiencies[name] ?: 0
                SkillRow(name, abilityScore, mult, profBonus, calculator)
            }

            // Если навыков нет (например, Телосложение), пустого места не будет,
            // так как спасбросок и разделитель теперь есть всегда.
        }
    }
}

@Composable
fun SavingThrowRow(
    name: String,
    score: Int,
    multiplier: Int,
    profBonus: Int,
    calculator: DndCalculator
) {
    val totalBonus = calculator.calculateSkillBonus(score, profBonus, multiplier)
    val sign = if (totalBonus >= 0) "+" else ""

    // Спасбросок не красим в зеленый фон полностью, красится только чекбокс (логика ниже)
    // Но вы можете изменить bgColor, если хотите подсвечивать всю строку
    val bgColor = ColorNoneBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Квадрат чекбокса
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
                    .background(if (multiplier > 0) Color.DarkGray else Color.Transparent), // Закрашиваем если есть владение
                contentAlignment = Alignment.Center
            ) {
                // Можно добавить галочку, но простая заливка тоже понятна
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = name,
                fontSize = 14.sp,
                color = Color.Black
            )
        }

        Text(
            text = "$sign$totalBonus",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun SkillRow(
    name: String,
    score: Int,
    multiplier: Int,
    profBonus: Int,
    calculator: DndCalculator
) {
    val totalBonus = calculator.calculateSkillBonus(score, profBonus, multiplier)
    val sign = if (totalBonus >= 0) "+" else ""

    val bgColor = when (multiplier) {
        1 -> ColorProficientBg
        2 -> ColorExpertiseBg
        else -> ColorNoneBg
    }

    // ЛОГИКА ШРИФТОВ:
    // Длинные слова уменьшаем, остальные оставляем 14sp
    val (fontSize, lineHeight) = when (name) {
        "Проницательность" -> 11.sp to 12.sp
        "Внимательность" -> 11.sp to 12.sp
        "Ловкость рук" -> 13.sp to 14.sp // Чуть меньше, на всякий случай
        else -> 14.sp to 16.sp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Текст навыка
        Text(
            text = name,
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = Color.Black,
            // modifier.weight(1f) заставляет текст занимать всё место слева от бонуса.
            // padding(start = 26.dp) выравнивает его с текстом спасброска (18 квадрат + 8 пробел)
            modifier = Modifier
                .weight(1f)
                .padding(start = 26.dp)
        )

        // Бонус
        Text(
            text = "$sign$totalBonus",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/SkillsTab.kt