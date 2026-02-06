// Имя файла: app/src/main/java/com/dnd/app/domain/model/RaceMapping.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

/**
 * СЛОВАРЬ СООТВЕТСТВИЯ (MAPPING LAYER)
 */
object RaceMapping {

    // БЕЛЫЙ СПИСОК: Строгое совпадение с именами в таблице `races`
    val PRIMARY_RACES = setOf(
        "Dragonborn",
        "Dwarf",
        "Elf",
        "Gnome",
        "Half-Elf", // С дефисом!
        "Half-Orc", // С дефисом!
        "Halfling",
        "Human",
        "Tiefling"
    )

    // Словарь: "Название в UI" -> "Name в БД (Техническое)"
    val subraceMap: Map<String, String> = mapOf(
        // Дварфы
        "Горный дварф" to "RockDwarf",
        "Холмовой дварф" to "HillDwarf",

        // Эльфы
        "Высший эльф" to "HighElf",
        "Лесной эльф" to "ForestElf",
        "Дроу" to "Drow",
        "Тёмный эльф (Дроу)" to "Drow",

        // Гномы
        "Лесной гном" to "ForestGnome",
        "Скальный гном" to "RockGnome",

        // Полурослики
        "Легконогий" to "LightLeged", // Как в базе (ID 14)
        "Коренастый" to "Stocky",     // Как в базе (ID 17)

        // Люди
        "Альтернативный" to "HumanAlt",
        "Классический" to "HumanClassic",
        "Альтернативный человек" to "HumanAlt",
        "Классический человек" to "HumanClassic"
    )

    fun getDbName(uiName: String): String? {
        // Сначала ищем точное совпадение
        if (subraceMap.containsKey(uiName)) return subraceMap[uiName]

        // Если не нашли, ищем частичное (например "Белый (Холод...)" не найдет, и это нормально для Драконов)
        return subraceMap.entries.find { uiName.contains(it.key, ignoreCase = true) }?.value
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/RaceMapping.kt