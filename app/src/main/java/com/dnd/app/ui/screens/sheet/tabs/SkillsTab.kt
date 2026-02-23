package com.dnd.app.ui.screens.sheet.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.snapshot.ProficiencyType
import com.dnd.app.domain.model.snapshot.SkillModel
import com.dnd.app.domain.model.snapshot.StatModel
import com.dnd.app.util.DndLocalization

private val ColorProficientBgLight = Color(0xFFC8E6C9)
private val ColorExpertiseBgLight = Color(0xFFF8BBD0)
private val ColorNoneBgLight = Color(0xFFFFFFFF)
private val ColorHeaderBgLight = Color(0xFFE0E0E0)

private val ColorProficientBgDark = Color(0xFF1E3A2A)
private val ColorExpertiseBgDark = Color(0xFF3A1E34)
private val ColorNoneBgDark = Color(0xFF1F1F1F)
private val ColorHeaderBgDark = Color(0xFF2A2A2A)

private var currentPbForProficiencyRows: Int = 0

private data class ProficiencyBadge(val label: String, val level: Int)

private data class GroupedProficiencies(
    val tools: List<ProficiencyBadge>,
    val weapons: List<ProficiencyBadge>,
    val armor: List<ProficiencyBadge>,
    val saves: List<ProficiencyBadge>,
    val other: List<ProficiencyBadge>
)

@Composable
fun SkillsTab(
    stats: List<StatModel>,
    skillsByStat: Map<String, List<SkillModel>>,
    proficiencyBonus: String,
    toolProficiencies: List<String>,
    proficiencies: Map<String, Int>,
    proficiencyLabels: Map<String, String>
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1C1C1C) else MaterialTheme.colorScheme.surface
    val headerBg = if (isDark) ColorHeaderBgDark else ColorHeaderBgLight
    val headerText = if (isDark) Color(0xFFF0F0F0) else Color(0xFF424242)
    val rowText = if (isDark) Color(0xFFF5F5F5) else Color(0xFF212121)

    val pbValue = proficiencyBonus.replace("+", "").toIntOrNull()?.coerceAtLeast(0) ?: 0
    val statsMap = stats.associateBy { it.code }
    val statOrderLeft = listOf("STR", "DEX", "CON", "INT")
    val statOrderRight = listOf("WIS", "CHA")
    val abilityNames = mapOf(
        "STR" to "Сила",
        "DEX" to "Ловкость",
        "CON" to "Телосложение",
        "INT" to "Интеллект",
        "WIS" to "Мудрость",
        "CHA" to "Харизма"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                statOrderLeft.forEach { code ->
                    statsMap[code]?.let { stat ->
                        AbilitySkillsCard(
                            abilityName = abilityNames[code] ?: code,
                            stat = stat,
                            skills = skillsByStat[code] ?: emptyList(),
                            cardBg = cardBg,
                            headerBg = headerBg,
                            headerText = headerText,
                            rowText = rowText,
                            isDark = isDark
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                statOrderRight.forEach { code ->
                    statsMap[code]?.let { stat ->
                        AbilitySkillsCard(
                            abilityName = abilityNames[code] ?: code,
                            stat = stat,
                            skills = skillsByStat[code] ?: emptyList(),
                            cardBg = cardBg,
                            headerBg = headerBg,
                            headerText = headerText,
                            rowText = rowText,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        currentPbForProficiencyRows = pbValue
        val grouped = buildGroupedProficiencies(proficiencies, proficiencyLabels)
        ToolsSectionCard(grouped.tools, toolProficiencies, cardBg, headerBg, headerText, rowText, isDark)
        ProficiencySectionCard("Оружие", grouped.weapons, cardBg, headerBg, headerText, rowText, isDark)
        ProficiencySectionCard("Броня", grouped.armor, cardBg, headerBg, headerText, rowText, isDark)
        ProficiencySectionCard("Спасброски", grouped.saves, cardBg, headerBg, headerText, rowText, isDark)
        ProficiencySectionCard("Прочие владения", grouped.other, cardBg, headerBg, headerText, rowText, isDark)
    }
}

@Composable
private fun ToolsSectionCard(
    tools: List<ProficiencyBadge>,
    fallbackTools: List<String>,
    cardBg: Color,
    headerBg: Color,
    headerText: Color,
    rowText: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (isDark) Color(0xFF4A4A4A) else Color.LightGray)
            .background(cardBg)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(headerBg).padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Инструменты", fontSize = 18.sp, color = headerText)
        }

        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            if (tools.isEmpty() && fallbackTools.isEmpty()) {
                Text(text = "Нет владений инструментами", fontSize = 14.sp, color = rowText)
            } else {
                if (tools.isNotEmpty()) {
                    tools.forEach { ProficiencyBadgeRow(it, rowText, isDark) }
                } else {
                    fallbackTools.forEach { tool ->
                        Text(text = "• $tool", fontSize = 14.sp, color = rowText)
                    }
                }
            }
        }
    }
}

@Composable
fun AbilitySkillsCard(
    abilityName: String,
    stat: StatModel,
    skills: List<SkillModel>,
    cardBg: Color,
    headerBg: Color,
    headerText: Color,
    rowText: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (isDark) Color(0xFF4A4A4A) else Color.LightGray)
            .background(cardBg)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(headerBg).padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = abilityName, fontSize = 18.sp, color = headerText)
        }

        Column(modifier = Modifier.padding(vertical = 2.dp)) {
            SavingThrowRow(
                name = "Спасбросок",
                modifier = stat.saveModifier,
                isProficient = stat.isProficientSave,
                isDark = isDark,
                rowText = rowText,
                rowBg = if (isDark) ColorNoneBgDark else ColorNoneBgLight
            )
            Divider(
                color = if (isDark) Color(0xFF5A5A5A) else Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            skills.forEach { skill ->
                SkillRow(skill, isDark, rowText)
            }
        }
    }
}

@Composable
fun SavingThrowRow(
    name: String,
    modifier: String,
    isProficient: Boolean,
    isDark: Boolean,
    rowText: Color,
    rowBg: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.dp, if (isDark) Color(0xFFB0B0B0) else Color.Gray, RoundedCornerShape(2.dp))
                    .background(
                        if (isProficient) {
                            if (isDark) Color(0xFFE0E0E0) else Color.DarkGray
                        } else {
                            Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = name, fontSize = 14.sp, color = rowText)
        }
        Text(text = modifier, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = rowText)
    }
}

@Composable
fun SkillRow(skill: SkillModel, isDark: Boolean, rowText: Color) {
    val bgColor = when (skill.profType) {
        ProficiencyType.PROFICIENCY -> if (isDark) ColorProficientBgDark else ColorProficientBgLight
        ProficiencyType.EXPERTISE -> if (isDark) ColorExpertiseBgDark else ColorExpertiseBgLight
        else -> if (isDark) ColorNoneBgDark else ColorNoneBgLight
    }

    val (fontSize, lineHeight) = when (skill.name) {
        "Проницательность", "Внимательность" -> 11.sp to 12.sp
        "Ловкость рук" -> 13.sp to 14.sp
        else -> 14.sp to 16.sp
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = skill.name,
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = rowText,
            modifier = Modifier.weight(1f).padding(start = 26.dp)
        )
        Text(
            text = skill.modifier,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = rowText,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun buildGroupedProficiencies(
    proficiencies: Map<String, Int>,
    labels: Map<String, String>
): GroupedProficiencies {
    val tools = mutableListOf<ProficiencyBadge>()
    val weapons = mutableListOf<ProficiencyBadge>()
    val armor = mutableListOf<ProficiencyBadge>()
    val saves = mutableListOf<ProficiencyBadge>()
    val other = mutableListOf<ProficiencyBadge>()

    proficiencies.forEach { (id, level) ->
        if (id.startsWith("skill-") || id.startsWith("lang-") || id.startsWith("feat-")) return@forEach
        val label = labels[id].orEmpty().ifBlank { DndLocalization.translateProficiency(id) }
        val item = ProficiencyBadge(label, level)
        when {
            id.startsWith("tool-") -> tools += item
            id.startsWith("saving-throw-") -> saves += item
            id.contains("armor") || id == "shields" -> armor += item
            id.contains("weapon") -> weapons += item
            else -> other += item
        }
    }

    val sorter: (ProficiencyBadge) -> String = { it.label.lowercase() }
    return GroupedProficiencies(
        tools.sortedBy(sorter),
        weapons.sortedBy(sorter),
        armor.sortedBy(sorter),
        saves.sortedBy(sorter),
        other.sortedBy(sorter)
    )
}

@Composable
private fun ProficiencySectionCard(
    title: String,
    items: List<ProficiencyBadge>,
    cardBg: Color,
    headerBg: Color,
    headerText: Color,
    rowText: Color,
    isDark: Boolean
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().border(2.dp, if (isDark) Color(0xFF4A4A4A) else Color.LightGray).background(cardBg)) {
        Box(modifier = Modifier.fillMaxWidth().background(headerBg).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Text(text = title, fontSize = 18.sp, color = headerText)
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            items.forEach { ProficiencyBadgeRow(it, rowText, isDark) }
        }
    }
}

@Composable
private fun ProficiencyBadgeRow(item: ProficiencyBadge, rowText: Color, isDark: Boolean) {
    val bgColor = if (item.level >= 2) {
        if (isDark) ColorExpertiseBgDark else ColorExpertiseBgLight
    } else {
        if (isDark) ColorProficientBgDark else ColorProficientBgLight
    }
    val totalBonus = currentPbForProficiencyRows * item.level.coerceAtLeast(1)
    val bonusText = if (totalBonus > 0) "+$totalBonus" else "0"

    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = item.label, fontSize = 14.sp, color = rowText, modifier = Modifier.weight(1f))
        Text(text = bonusText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rowText)
    }
}


