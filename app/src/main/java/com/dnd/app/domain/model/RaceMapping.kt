// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\RaceMapping.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model


object RaceMapping {


    val PRIMARY_RACES = setOf(
        "Dragonborn",
        "Dwarf",
        "Elf",
        "Gnome",
        "Half-Elf",
        "Half-Orc",
        "Halfling",
        "Human",
        "Tiefling"
    )


    val subraceMap: Map<String, String> = mapOf(

        "Драконорожденный" to "Dragonborn",
        "Дварф" to "Dwarf",
        "Эльф" to "Elf",
        "Гном" to "Gnome",
        "Полуэльф" to "Half-Elf",
        "Полуорк" to "Half-Orc",

        "Полурослик" to "Halfling",
        "Человек" to "Human",
        "Тифлинг" to "Tiefling",


        "Горный дварф" to "RockDwarf",
        "Холмовой дварф" to "HillDwarf",


        "Высший эльф" to "HighElf",
        "Лесной эльф" to "ForestElf",
        "Дроу" to "Drow",
        "Тёмный эльф (Дроу)" to "Drow",


        "Лесной гном" to "ForestGnome",
        "Скальный гном" to "RockGnome",



        "Легконогий полурослик" to "LightLeged",
        "Коренастый полурослик" to "Stocky",


        "Альтернативный" to "HumanAlt",
        "Классический" to "HumanClassic",
        "Альтернативный человек" to "HumanAlt",
        "Классический человек" to "HumanClassic"
    )

    fun getDbName(uiName: String): String? {

        if (subraceMap.containsKey(uiName)) return subraceMap[uiName]


        return subraceMap.entries.find { uiName.contains(it.key, ignoreCase = true) }?.value
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\RaceMapping.kt