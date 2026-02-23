package com.dnd.app.util

import java.util.Locale

object DndLocalization {

    val ALL_SKILLS = mapOf(
        "acrobatics" to "Акробатика",
        "animal-handling" to "Уход за животными",
        "arcana" to "Магия",
        "athletics" to "Атлетика",
        "deception" to "Обман",
        "history" to "История",
        "insight" to "Проницательность",
        "intimidation" to "Запугивание",
        "investigation" to "Анализ",
        "medicine" to "Медицина",
        "nature" to "Природа",
        "perception" to "Внимательность",
        "performance" to "Выступление",
        "persuasion" to "Убеждение",
        "religion" to "Религия",
        "sleight-of-hand" to "Ловкость рук",
        "stealth" to "Скрытность",
        "survival" to "Выживание"
    )

    private val statTranslations = mapOf(
        "STR" to "Сила",
        "DEX" to "Ловкость",
        "CON" to "Телосложение",
        "INT" to "Интеллект",
        "WIS" to "Мудрость",
        "CHA" to "Харизма"
    )

    private val statAbbreviations = mapOf(
        "DEX" to "ЛОВ",
        "CON" to "ТЕЛ"
    )

    private val magicSchoolTranslations = mapOf(
        "abjuration" to "Ограждение",
        "conjuration" to "Вызов",
        "divination" to "Прорицание",
        "enchantment" to "Очарование",
        "evocation" to "Воплощение",
        "illusion" to "Иллюзия",
        "necromancy" to "Некромантия",
        "transmutation" to "Преобразование"
    )

    private val resourceMap = mapOf(
        "Ki Points" to "Очки Ци",
        "KI" to "Очки Ци",
        "Rage Count" to "Ярость",
        "Rage" to "Ярость",
        "RAGE" to "Ярость",
        "Sorcery Points" to "Очки чародейства",
        "Superiority Dice" to "Кости превосходства",
        "Channel Divinity" to "Божественный канал",
        "Lay On Hands Pool" to "Наложение рук",
        "Action Surge Uses" to "Всплеск действий",
        "Action Surge" to "Всплеск действий",
        "ACTION SURGE" to "Всплеск действий",
        "Indomitable Uses" to "Неукротимость",
        "Wild Shape Uses" to "Дикий облик"
    )

    private val traitTranslations = mapOf(
        "aberration" to "Аберрация",
        "beast" to "Зверь",
        "celestial" to "Небожитель",
        "construct" to "Конструкт",
        "dragon" to "Дракон",
        "elemental" to "Элементаль",
        "fey" to "Фея",
        "fiend" to "Исчадие",
        "giant" to "Великан",
        "humanoid" to "Гуманоид",
        "monstrosity" to "Чудовище",
        "ooze" to "Слизь",
        "plant" to "Растение",
        "undead" to "Нежить",
        "aberrations" to "Аберрации",
        "beasts" to "Звери",
        "celestials" to "Небожители",
        "constructs" to "Конструкты",
        "dragons" to "Драконы",
        "elementals" to "Элементали",
        "fiends" to "Исчадия",
        "giants" to "Великаны",
        "monstrosities" to "Чудовища",
        "oozes" to "Слизи",
        "plants" to "Растения",
        "humanoids" to "Гуманоиды",
        "арктика" to "Арктика",
        "arctic" to "Арктика",
        "coast" to "Побережье",
        "desert" to "Пустыня",
        "forest" to "Лес",
        "grassland" to "Луга",
        "mountain" to "Горы",
        "swamp" to "Болото",
        "imp" to "Бес",
        "pseudodragon" to "Псевдодракон",
        "quasit" to "Квазит",
        "sprite" to "Спрайт"
    )

    fun translateProficiency(id: String): String {
        if (id.isBlank()) return ""

        resourceMap[id]?.let { return it }

        if (id.startsWith("saving-throw-")) {
            val stat = id.substring(13).uppercase()
            return "Спасбросок: ${translateStat(stat)}"
        }

        val clean = id
            .replace("skill-", "")
            .replace("tool-", "")
            .replace("lang-", "")
            .lowercase()
            .trim()

        resourceMap.entries.find { it.key.lowercase() == clean }?.value?.let { return it }
        ALL_SKILLS[clean]?.let { return it }
        traitTranslations[clean]?.let { return it }

        return clean.replace("-", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun translateRarity(rarity: String?): String? = when (rarity?.lowercase()?.trim()) {
        "common" -> "Обычный"
        "uncommon" -> "Необычный"
        "rare" -> "Редкий"
        "very rare" -> "Очень редкий"
        "legendary" -> "Легендарный"
        "artifact" -> "Артефакт"
        "varies" -> "Варьируется"
        else -> rarity?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private val speedTranslations = mapOf(
        "walk" to "ходьба",
        "fly" to "полёт",
        "climb" to "лазание",
        "swim" to "плавание",
        "burrow" to "рытьё",
        "hover" to "парение",
        "crawl" to "ползком"
    )

    fun translateSpeed(mode: String): String {
        val key = mode.lowercase().trim()
        return speedTranslations[key]
            ?: key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun assembleEnrichedDescription(rarity: String?, stats: String?, description: String?): String {
        val parts = mutableListOf<String>()
        rarity?.let { parts += it }
        stats?.takeIf { it.isNotBlank() }?.let { parts += it }
        description?.let { parts += it }
        return parts.joinToString("\n").trim()
    }

    fun translateSenseKey(key: String): String = when (key.lowercase().trim()) {
        "darkvision" -> "Тёмное зрение"
        "blindsight" -> "Слепое зрение"
        "tremorsense" -> "Чувство вибрации"
        "truesight" -> "Истинное зрение"
        "passive_perception" -> "Пассивная внимательность"
        else -> key.replace('_', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun translateDamageType(type: String): String = when (type.lowercase().trim()) {
        "acid" -> "Кислота"
        "bludgeoning" -> "Дробящий"
        "cold" -> "Холод"
        "fire" -> "Огонь"
        "force" -> "Силовой"
        "lightning" -> "Молния"
        "necrotic" -> "Некротический"
        "piercing" -> "Колющий"
        "poison" -> "Яд"
        "psychic" -> "Психический"
        "radiant" -> "Сияние"
        "slashing" -> "Рубящий"
        "thunder" -> "Гром"
        else -> type
    }

    fun translateMonsterSize(size: String): String = when (size.lowercase().trim()) {
        "tiny", "крошечный" -> "Крошечный"
        "small", "маленький" -> "Маленький"
        "medium", "средний" -> "Средний"
        "large", "большой" -> "Большой"
        "huge", "огромный" -> "Огромный"
        "gargantuan", "громадный" -> "Громадный"
        else -> size.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun translateMonsterType(type: String): String = when (type.lowercase().trim()) {
        "aberration", "аберрация" -> "Аберрация"
        "beast", "зверь" -> "Зверь"
        "celestial", "небожитель" -> "Небожитель"
        "construct", "конструкт" -> "Конструкт"
        "dragon", "дракон" -> "Дракон"
        "elemental", "элементаль" -> "Элементаль"
        "fey", "фея" -> "Фея"
        "fiend", "исчадье" -> "Исчадие"
        "giant", "великан" -> "Великан"
        "humanoid", "гуманоид" -> "Гуманоид"
        "monstrosity", "чудовище" -> "Чудовище"
        "ooze", "слизь" -> "Слизь"
        "plant", "растение" -> "Растение"
        "undead", "нежить" -> "Нежить"
        else -> type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun translateAlignment(value: String): String = when (value.lowercase().trim()) {
        "lawful-good", "lawful good", "законопослушный-добрый", "законопослуный-добрый" -> "Законопослушно-добрый"
        "neutral-good", "neutral good" -> "Нейтрально-добрый"
        "chaotic-good", "chaotic good" -> "Хаотично-добрый"
        "lawful-neutral", "lawful neutral" -> "Законопослушно-нейтральный"
        "neutral" -> "Нейтральный"
        "chaotic-neutral", "chaotic neutral" -> "Хаотично-нейтральный"
        "lawful-evil", "lawful evil", "законопослушный-злой" -> "Законопослушно-злой"
        "neutral-evil", "neutral evil" -> "Нейтрально-злой"
        "chaotic-evil", "chaotic evil", "хаотично-злой" -> "Хаотично-злой"
        "unaligned" -> "Без мировоззрения"
        "any alignment" -> "Любое мировоззрение"
        else -> value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun formatWeaponInfo(damage: String?, damageTypeName: String?): String {
        if (damage == null) return ""
        return "Урон: $damage ${damageTypeName ?: ""}".trim()
    }

    fun formatArmorInfo(ac: Int?): String {
        if (ac == null || ac == 0) return ""
        return "КД: $ac"
    }

    fun translateSchool(index: String?): String {
        if (index.isNullOrBlank()) return "Неизвестно"
        val normalized = index.lowercase().trim()
        return magicSchoolTranslations[normalized]
            ?: normalized.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun getSpellPoolName(poolType: String, classIndex: String): String {
        val className = translateProficiency(classIndex)
        val lowerPool = poolType.lowercase()
        if (lowerPool.contains("necromancy") && lowerPool.contains("_0")) return "Заговоры некромантии ($className)"
        if (lowerPool.contains("necromancy")) return "Заклинания некромантии ($className)"
        if (lowerPool.contains("druid") && lowerPool.contains("_0")) return "Заговоры друида ($className)"
        if (lowerPool.contains("wizard") && lowerPool.contains("_0")) return "Заговоры волшебника ($className)"
        if (lowerPool.contains("cantrip") || lowerPool.endsWith("_0") || lowerPool.contains("level_0")) return "Заговоры ($className)"
        if (lowerPool.contains("ritual")) return "Ритуалы ($className)"
        return "Заклинания ($className)"
    }

    fun translateFeatureChoiceHeader(index: String): String = when {
        index.contains("fighting-style") -> "Боевой стиль"
        index.contains("favored-enemy") -> "Избранный враг"
        index.contains("natural-explorer") -> "Знание местности"
        index.contains("sorcerous-origin") -> "Происхождение чародея"
        index.contains("draconic-ancestry") -> "Драконье наследие"
        else -> ""
    }

    fun translateStat(code: String): String = statTranslations[code.take(3).uppercase()] ?: code

    fun translateSkill(id: String): String = translateProficiency(id)

    fun cleanLabel(label: String): String = label
        .replace("Навык: ", "")
        .replace("Skill: ", "")
        .replace("Proficiency: ", "")
        .replace("Saving Throw: ", "Спасбросок: ")
        .trim()

    fun getSpeciesHeader(parentRaceIndex: String): String {
        val speciesGenitive = mapOf(
            "dwarf" to "дварфов",
            "elf" to "эльфов",
            "gnome" to "гномов",
            "halfling" to "полуросликов",
            "human" to "людей",
            "dragonborn" to "драконорождённых",
            "tiefling" to "тифлингов"
        )
        return "Виды ${speciesGenitive[parentRaceIndex.lowercase()] ?: parentRaceIndex}"
    }

    fun getStatIncreaseSummary(bonuses: Map<String, Int>): String {
        if (bonuses.isEmpty()) return ""
        return "Значение вашей " + bonuses.entries.joinToString { "${translateStat(it.key)} увеличивается на ${it.value}" }
    }

    fun translateBreathType(type: String): String = when (type) {
        "line" -> "линия"
        "cone" -> "конус"
        else -> type
    }

    fun getBreathSaveStatAbbr(damageTypeIndex: String): String {
        val key = if (damageTypeIndex == "poison") "CON" else "DEX"
        val abbr = statAbbreviations[key] ?: key
        return "спас. $abbr"
    }

    fun translateClassSlotsHeader(): String = "КЛАСС"
    fun translateRaceSlotsHeader(): String = "РАСА"
    fun translatePactMagicHeader(): String = "МАГИЯ ПАКТА"
}
