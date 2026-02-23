// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\components\CreatorComponents.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.util.stripHtml
import com.dnd.app.ui.components.shared.spell.SpellActionType
import com.dnd.app.ui.components.shared.spell.UnifiedSpellListItem


@Composable
fun FeatureChoiceBlock(
    choice: FeatureChoiceDomain,
    allSelections: Map<String, ChoiceResult>,
    onSelectionUpdated: (key: String, result: ChoiceResult) -> Unit,
    selectionKey: String,
    isExpanded: (String) -> Boolean,
    onToggleExpanded: (String) -> Unit,
    proficiencyExclusions: Map<Int, Set<String>> = emptyMap(),
    pickedProficiencies: List<StaticProficiency> = emptyList(),
    parentLabel: String? = null,
    suppressTopPadding: Boolean = false,
    featRegistry: Map<String, Feature> = emptyMap()
) {
    val weight = (choice as? ProficiencyChoice)?.targetProficiencyLevel ?: 1
    val relevantExclusions = proficiencyExclusions[weight] ?: emptySet()
    val isTransparent = (choice as? FeatureChoiceDomain.SelectOption)?.isTransparent == true

    Column(modifier = Modifier.padding(top = if (suppressTopPadding || isTransparent) 0.dp else WizardUiConfig.CHOICE_BLOCK_TOP_PADDING)) {

        val headerText = if (!isTransparent) (parentLabel ?: (choice as? FeatureChoiceDomain.SelectOption)?.description) else null
        if (!headerText.isNullOrBlank()) {
            Text(text = headerText, modifier = Modifier.padding(bottom = 4.dp), fontSize = 13.sp, fontStyle = FontStyle.Italic)
        }

        when (choice) {
            is FeatureChoiceDomain.InvalidChoice -> {
                Row(modifier = Modifier.fillMaxWidth().background(Color.Red.copy(alpha = 0.1f)).border(1.dp, Color.Red).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color.Red); Spacer(Modifier.width(8.dp))
                    Text("Ошибка данных: ${choice.reason}", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            is FeatureChoiceDomain.SelectOption -> {
                if (isTransparent) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        choice.options.forEach { option ->

                            val uniqueSubKey = ChoicePathManager.append(selectionKey, option.id)
                            val selectionValue = allSelections[uniqueSubKey]
                            val isSelected = selectionValue != null && selectionValue !is ChoiceResult.Note

                            if (option.subChoice != null) {
                                FeatureChoiceBlock(
                                    choice = option.subChoice,
                                    allSelections = allSelections,
                                    onSelectionUpdated = onSelectionUpdated,
                                    selectionKey = uniqueSubKey,
                                    isExpanded = isExpanded,
                                    onToggleExpanded = onToggleExpanded,
                                    proficiencyExclusions = proficiencyExclusions,
                                    pickedProficiencies = pickedProficiencies,
                                    parentLabel = option.label,
                                    suppressTopPadding = true,
                                    featRegistry = featRegistry
                                )
                            } else {

                                val allKnownProficiencyIds = remember(pickedProficiencies) { pickedProficiencies.map { it.id }.toSet() }
                                val isExpertiseChoice = weight == 2
                                val isExcluded = if (isExpertiseChoice) option.id !in allKnownProficiencyIds else option.id in relevantExclusions


                                val branchSelectionCount = remember(allSelections, selectionKey) {
                                    allSelections.entries.count { (k, v) ->
                                        ChoicePathManager.isChildOf(selectionKey, k) && v !is ChoiceResult.Note
                                    }
                                }
                                val canSelectMore = branchSelectionCount < choice.count
                                val isEnabled = (!isExcluded || isSelected) && (isSelected || canSelectMore)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .background(if (isSelected) Color(0xFFE0E0E0) else Color.White)
                                        .border(1.dp, if (isSelected) Color.DarkGray else Color.LightGray)
                                        .clickable(enabled = isEnabled) {
                                            val nextRes = if (isSelected) ChoiceResult.Note("REMOVED")
                                            else ChoiceResult.SelectedOptions(listOf(option.id), weight, choice.proficiencyKind)
                                            onSelectionUpdated(uniqueSubKey, nextRes)
                                        }
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = isSelected, onCheckedChange = { checked ->
                                        val nextRes = if (!checked) ChoiceResult.Note("REMOVED")
                                        else ChoiceResult.SelectedOptions(listOf(option.id), weight, choice.proficiencyKind)
                                        onSelectionUpdated(uniqueSubKey, nextRes)
                                    }, enabled = isEnabled)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = option.label, fontSize = 14.sp, color = if (!isEnabled && !isSelected) Color(0xFF616161) else Color.Black)
                                }
                            }
                        }
                    }
                } else {
                    val currentSelection = allSelections[selectionKey]
                    val selectedItems = (currentSelection as? ChoiceResult.SelectedOptions)?.items ?: emptyList()
                    repeat(choice.count) { i ->
                        val currentId = selectedItems.getOrNull(i)
                        SmartDropdown(
                            options = choice.options, selectedId = currentId,
                            onSelected = { opt ->
                                val newList = selectedItems.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                                onSelectionUpdated(selectionKey, ChoiceResult.SelectedOptions(newList.filter { it.isNotBlank() }, weight, choice.proficiencyKind))
                            },
                            exclusions = relevantExclusions + selectedItems.filter { it != currentId }.toSet(), placeholder = "Выберите вариант..."
                        )
                        Spacer(Modifier.height(WizardUiConfig.CHOICE_ITEM_VERTICAL_SPACING))
                    }

                    selectedItems.forEach { selectedId ->
                                val staticSub = choice.options.find { it.id == selectedId }?.subChoice

                                if (staticSub != null) {
                                        val subKey = ChoicePathManager.append(selectionKey, selectedId)
                                        val isSubTransparent = (staticSub as? FeatureChoiceDomain.SelectOption)?.isTransparent == true

                                        Spacer(Modifier.height(8.dp))
                                        Box(modifier = Modifier
                                            .run { if (isSubTransparent) this else padding(start = 12.dp).border(1.dp, Color.Gray) }
                                            .padding(if (isSubTransparent) 0.dp else 8.dp)
                                        ) {
                                                FeatureChoiceBlock(staticSub, allSelections, onSelectionUpdated, subKey, isExpanded, onToggleExpanded, proficiencyExclusions, pickedProficiencies, null, true, featRegistry)
                                        }
                                } else {
                                        val normalizedSelectedId = selectedId.trim().lowercase()
                                        val featDetails = featRegistry[normalizedSelectedId]
                                        Log.d("RALPH_DEBUG", "Feat registry lookup for $normalizedSelectedId -> ${featDetails?.index ?: "missing"}")

                                        if (featDetails != null) {
                                                val subKey = ChoicePathManager.append(selectionKey, selectedId)

                                                Spacer(Modifier.height(8.dp))
                                                Box(modifier = Modifier
                                                    .padding(start = 12.dp)
                                                    .border(1.dp, Color.Gray)
                                                    .padding(8.dp)
                                                ) {
                                                        Column {
                                                                Text(featDetails.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                                if (featDetails.description.isNotBlank()) Text(featDetails.description.stripHtml(), fontSize = 12.sp, lineHeight = 16.sp)
                                                                featDetails.choices.forEachIndexed { idx, c ->
                                                                        val uniqueKey = ChoicePathManager.append(subKey, "", idx)
                                                                        FeatureChoiceBlock(c, allSelections, onSelectionUpdated, uniqueKey, isExpanded, onToggleExpanded, proficiencyExclusions, pickedProficiencies, null, true, featRegistry)
                                                                }
                                                        }
                                                }
                                        }
                                }
                    }
                }
            }
            is FeatureChoiceDomain.SelectSpell -> SpellSelectionGroup(choice, selectionKey, allSelections, onSelectionUpdated, isExpanded, onToggleExpanded)
            is FeatureChoiceDomain.SelectStatBonus -> {
                val existingStats = (allSelections[selectionKey] as? ChoiceResult.StatBonus)?.stats ?: emptyList()
                val slotSelections = MutableList(choice.count) { "" }
                var consumed = 0
                for (slotIndex in 0 until choice.count) {
                    if (consumed >= existingStats.size) break
                    val chunkEnd = (consumed + choice.amount).coerceAtMost(existingStats.size)
                    slotSelections[slotIndex] = existingStats.subList(consumed, chunkEnd)
                        .firstOrNull()?.uppercase()?.trim() ?: ""
                    consumed = chunkEnd
                }

                repeat(choice.count) { i ->
                    val currentSelection = slotSelections.getOrNull(i)?.takeIf { it.isNotBlank() }
                    val statExclusions = if (choice.allowDuplicateSelections) {
                        emptySet()
                    } else {
                        slotSelections
                            .mapIndexedNotNull { index, value ->
                                if (index == i) null else value.trim().uppercase().takeIf { it.isNotBlank() }
                            }
                            .toSet()
                    }
                    SmartDropdown(
                        options = choice.options,
                        selectedId = currentSelection,
                        onSelected = { opt ->
                            val updatedSlots = slotSelections.toMutableList()
                            updatedSlots[i] = opt.id
                            val multiplier = choice.amount.coerceAtLeast(1)
                            val statsList = updatedSlots
                                .map { it.trim().uppercase() }
                                .filter { it.isNotBlank() }
                                .flatMap { stat -> List(multiplier) { stat } }
                            onSelectionUpdated(selectionKey, ChoiceResult.StatBonus(statsList))
                        },
                        exclusions = statExclusions,
                        placeholder = "Характеристика..."
                    )
                    Spacer(Modifier.height(WizardUiConfig.CHOICE_ITEM_VERTICAL_SPACING))
                }
            }
            is FeatureChoiceDomain.SelectSkill -> {
                val selected = (allSelections[selectionKey] as? ChoiceResult.SelectedOptions)?.items ?: emptyList()
                repeat(choice.count) { i ->
                    val cur = selected.getOrNull(i)
                    SmartDropdown(choice.options, cur, { opt ->
                        val list = selected.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                        onSelectionUpdated(selectionKey, ChoiceResult.SelectedOptions(list.filter { it.isNotBlank() }, 1, ProficiencyKind.SKILL))
                    }, relevantExclusions + selected.filter { it != cur }, "Навык...")
                }
            }
            is FeatureChoiceDomain.SelectExpertise -> {
                val selected = (allSelections[selectionKey] as? ChoiceResult.SelectedOptions)?.items ?: emptyList()
                repeat(choice.count) { i ->
                    val cur = selected.getOrNull(i)
                    SmartDropdown(choice.options, cur, { opt ->
                        val list = selected.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                        onSelectionUpdated(selectionKey, ChoiceResult.SelectedOptions(list.filter { it.isNotBlank() }, 2, choice.proficiencyKind))
                    }, relevantExclusions + selected.filter { it != cur }, "Мастерство...")
                }
            }
        }
    }
}

@Composable
fun FeatureSection(
    feature: Feature,
    selectionSource: SelectionSource,
    allSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    proficiencyExclusions: Map<Int, Set<String>>,
    pickedProficiencies: List<StaticProficiency>,
    isExpanded: (String) -> Boolean,
    onToggleExpanded: (String) -> Unit,
    featRegistry: Map<String, Feature> = emptyMap(),
    extraContent: @Composable (() -> Unit)? = null
) {
    val isAggregatedMagic = feature.index == DndConstants.VirtualKeys.AGGREGATED_SPELL_CHOICE
    val isContainerOnly = !isAggregatedMagic && feature.description.isBlank() && feature.embeddedSpells.isEmpty() &&
            feature.choices.size == 1 && (feature.choices.firstOrNull() as? FeatureChoiceDomain.SelectOption)?.isTransparent == true

    val titleToShow = if (feature.index.startsWith("virtual-") || isContainerOnly) "" else feature.name

    FlatWizardSection(title = titleToShow, applyDefaultContentPadding = !isAggregatedMagic) {
        Column {
            if (feature.description.isNotBlank() && !isContainerOnly) {
                Text(feature.description.stripHtml(), fontSize = WizardUiConfig.FONT_SIZE_CONTENT, lineHeight = 17.sp)
            }
            extraContent?.invoke()
            feature.choices.forEachIndexed { index, choice ->
                FeatureChoiceBlock(choice, allSelections, onSelectionChanged, ChoicePathManager.createIndexedKey(selectionSource, feature.index, index), isExpanded, onToggleExpanded, proficiencyExclusions, pickedProficiencies, null, feature.description.isBlank() && index == 0, featRegistry)
            }
            if (feature.embeddedSpells.isNotEmpty()) {
                val key = "spells_${feature.index}"
                CollapsibleChoiceSection(title = "Добавленные заклинания", expanded = isExpanded(key), onToggle = { onToggleExpanded(key) }) {
                    Column { feature.embeddedSpells.forEach { spell ->
                        val sKey = "granted_${feature.index}_${spell.index}"
                        UnifiedSpellListItem(
                            name = spell.name,
                            level = spell.level,
                            school = spell.school,
                            castingTime = spell.castingTime,
                            range = spell.range,
                            components = spell.components,
                            duration = spell.duration,
                            description = spell.description,
                            actionType = SpellActionType.ADD,
                            onActionClick = {},
                            isActionEnabled = false,
                            isExpanded = isExpanded(sKey),
                            onToggleExpand = { onToggleExpanded(sKey) },
                            style = WizardSpellListItemStyle
                        )
                    } }
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\components\CreatorComponents.kt
