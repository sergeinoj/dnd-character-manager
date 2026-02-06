// Имя файла: app/src/main/java/com/dnd/app/util/DndLocalization.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.util

import java.util.Locale

object DndLocalization {

    val ALL_SKILLS = mapOf(
        "acrobatics" to "Акробатика", "animal-handling" to "Уход за животными",
        "arcana" to "Магия", "athletics" to "Атлетика", "deception" to "Обман",
        "history" to "История", "insight" to "Проницательность", "intimidation" to "Запугивание",
        "investigation" to "Анализ", "medicine" to "Медицина", "nature" to "Природа",
        "perception" to "Внимательность", "performance" to "Выступление", "persuasion" to "Убеждение",
        "religion" to "Религия", "sleight-of-hand" to "Ловкость рук", "stealth" to "Скрытность",
        "survival" to "Выживание"
    )

    private val statTranslations = mapOf(
        "STR" to "Сила", "DEX" to "Ловкость", "CON" to "Телосложение",
        "INT" to "Интеллект", "WIS" to "Мудрость", "CHA" to "Харизма"
    )

    /**
     * Перевод специфических заголовков для фич-выборов.
     */
    fun translateFeatureChoiceHeader(index: String): String {
        return when {
            index.contains("fighting-style") -> "Боевой стиль"
            index.contains("favored-enemy") -> "Избранный враг"
            index.contains("natural-explorer") -> "Знание местности"
            index.contains("sorcerous-origin") -> "Происхождение чародея"
            index.contains("draconic-ancestry") -> "Драконье наследие"
            else -> ""
        }
    }

    fun translateStat(code: String): String = statTranslations[code.take(3).uppercase()] ?: code

    fun translateSkill(id: String): String {
        val cleanId = id.replace("skill-", "").lowercase().trim()
        return ALL_SKILLS[cleanId] ?: id.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun cleanLabel(label: String): String {
        return label.replace("Навык: ", "").replace("Skill: ", "").replace("Proficiency: ", "").replace("Saving Throw: ", "Спасбросок: ").trim()
    }

    fun translateProficiency(name: String): String {
        val cleaned = cleanLabel(name)
        if (cleaned.startsWith("Спасбросок: ")) {
            val stat = cleaned.substringAfter(": ")
            return "Спасбросок: ${translateStat(stat)}"
        }
        return cleaned
    }

    fun getSpeciesHeader(parentRaceIndex: String): String {
        val speciesGenitive = mapOf("dwarf" to "дварфов", "elf" to "эльфов", "gnome" to "гномов", "halfling" to "полуросликов", "human" to "людей", "dragonborn" to "драконорожденных", "tiefling" to "тифлингов")
        return "Виды ${speciesGenitive[parentRaceIndex.lowercase()] ?: parentRaceIndex}"
    }

    fun getStatIncreaseSummary(bonuses: Map<String, Int>): String {
        if (bonuses.isEmpty()) return ""
        return "Значение вашей " + bonuses.entries.joinToString { "${translateStat(it.key)} увеличивается на ${it.value}" }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/util/DndLocalization.kt