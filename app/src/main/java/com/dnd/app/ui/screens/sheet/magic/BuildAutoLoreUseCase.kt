package com.dnd.app.ui.screens.sheet.magic

import com.dnd.app.domain.model.snapshot.CharacterSnapshot
import com.dnd.app.ui.screens.sheet.AutoLoreBlock
import com.dnd.app.ui.screens.sheet.AutoLoreSection
import com.dnd.app.util.DndLocalization
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildAutoLoreUseCase @Inject constructor() {

    fun build(snapshot: CharacterSnapshot): List<AutoLoreSection> {
        val backgroundBlocks = snapshot.features
            .filter { isAvailableAtLevel(it.level, snapshot.global.level) }
            .filter { it.id.startsWith("bgf-") && it.id.contains("-lore") }
            .filterNot { it.hasChoices }
            .filterNot { isEquipmentOrChoicePrompt(it.id, it.name, it.description) }
            .mapNotNull { f -> f.description.takeIf { it.isNotBlank() }?.let { AutoLoreBlock(f.id, f.name, it) } }

        val raceFeatureBlocks = snapshot.features
            .asSequence()
            .filter { isAvailableAtLevel(it.level, snapshot.global.level) }
            .filter { !it.id.startsWith("bgf-") }
            .filter { it.description.length >= 40 }
            .filter { isRaceSource(it.source) }
            .filterNot { it.hasChoices }
            .filterNot { isEquipmentOrChoicePrompt(it.id, it.name, it.description) }
            .map { AutoLoreBlock(it.id, it.name, it.description) }
            .distinctBy { it.id }
            .toList()

        val raceBlocks = buildList {
            snapshot.global.raceDescription.takeIf { it.isNotBlank() }?.let {
                add(AutoLoreBlock("race-description", snapshot.global.race, it))
            }
            snapshot.global.subraceDescription.takeIf { it.isNotBlank() }?.let {
                val title = snapshot.global.subrace ?: "Подраса"
                add(AutoLoreBlock("subrace-description", title, it))
            }
            addAll(raceFeatureBlocks)
            if (isEmpty() && snapshot.global.race.isNotBlank()) {
                add(
                    AutoLoreBlock(
                        "race-fallback",
                        snapshot.global.race,
                        "Описание расы отсутствует в источнике данных."
                    )
                )
            }
        }

        val classBlocks = snapshot.features
            .asSequence()
            .filter { isAvailableAtLevel(it.level, snapshot.global.level) }
            .filter { !it.id.startsWith("bgf-") }
            .filter { isClassSource(it.source) }
            .filter { it.description.isNotBlank() }
            .filterNot { it.hasChoices }
            .filterNot { isEquipmentOrChoicePrompt(it.id, it.name, it.description) }
            .sortedWith(
                compareBy(
                    { it.level ?: Int.MAX_VALUE },
                    { it.displayPriority },
                    { it.name.lowercase() }
                )
            )
            .map { AutoLoreBlock(it.id, it.name, it.description) }
            .distinctBy { it.id }
            .toList()
            .toMutableList()
            .apply {
                snapshot.global.subclassDescription.takeIf { it.isNotBlank() }?.let {
                    val title = snapshot.global.subclassName.ifBlank { "Подкласс" }
                    add(0, AutoLoreBlock("subclass-description", title, it))
                }
            }

        val alignmentName = DndLocalization.translateAlignment(snapshot.global.alignment)
        val alignmentDesc = snapshot.global.alignmentDescription
        val alignmentBlocks = if (alignmentName.isNotBlank() || alignmentDesc.isNotBlank()) {
            listOf(
                AutoLoreBlock(
                    "alignment",
                    if (alignmentName.isNotBlank()) alignmentName else "Мировоззрение",
                    if (alignmentDesc.isNotBlank()) alignmentDesc else alignmentName
                )
            )
        } else {
            emptyList()
        }

        val raceTitle = buildString {
            append("Раса")
            if (snapshot.global.race.isNotBlank()) append(": ${snapshot.global.race}")
            if (!snapshot.global.subrace.isNullOrBlank()) append(" / ${snapshot.global.subrace}")
        }
        val classTitle = buildString {
            append("Класс")
            if (snapshot.global.classTitle.isNotBlank()) append(": ${snapshot.global.classTitle}")
            if (snapshot.global.subclassName.isNotBlank()) append(" / ${snapshot.global.subclassName}")
        }
        val backgroundTitle = buildString {
            append("Предыстория")
            if (snapshot.global.backgroundName.isNotBlank()) append(": ${snapshot.global.backgroundName}")
        }
        val alignmentTitle = buildString {
            append("Мировоззрение")
            if (alignmentName.isNotBlank()) append(": $alignmentName")
        }

        return listOf(
            AutoLoreSection("class-subclass", classTitle, classBlocks),
            AutoLoreSection("background", backgroundTitle, backgroundBlocks),
            AutoLoreSection("race-subrace", raceTitle, raceBlocks),
            AutoLoreSection("alignment", alignmentTitle, alignmentBlocks)
        ).filter { it.blocks.isNotEmpty() }
    }

    private fun isAvailableAtLevel(featureLevel: Int?, currentLevel: Int): Boolean {
        return featureLevel == null || featureLevel <= currentLevel
    }

    private fun isRaceSource(source: String): Boolean {
        val s = source.lowercase()
        return "раса" in s ||
            "подраса" in s ||
            "race" in s ||
            "species" in s ||
            "рaс" in s ||
            "субраса" in s
    }

    private fun isClassSource(source: String): Boolean {
        val s = source.lowercase()
        return "класс" in s || "подкласс" in s || "class" in s || "subclass" in s
    }

    private fun isEquipmentOrChoicePrompt(id: String, name: String, description: String): Boolean {
        val idText = id.lowercase()
        val nameText = name.lowercase()
        val desc = description.lowercase()

        val equipmentTokens = listOf(
            "equip", "equipment", "gear", "weapon", "armor", "tool", "kit",
            "снаряж", "оруж", "брон", "инструмент", "набор"
        )
        if (equipmentTokens.any { it in idText || it in nameText || it in desc }) return true

        val choicePromptTokens = listOf(
            "choose", "choice", "select", "pick", "one of",
            "выберите", "выбери", "на выбор", "один из", "одно из", "выберите один"
        )
        return choicePromptTokens.any { it in desc || it in nameText }
    }

    private fun isBackgroundSource(source: String): Boolean {
        val s = source.lowercase()
        return "предыст" in s || "background" in s || "бек" in s
    }
}
