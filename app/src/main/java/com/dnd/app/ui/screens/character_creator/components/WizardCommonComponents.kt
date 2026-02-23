// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\components\WizardCommonComponents.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.ChoiceOption
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.FeatureChoiceDomain
import com.dnd.app.domain.model.ProficiencyKind
import com.dnd.app.ui.screens.character_creator.EquipmentOptionDetails
import com.dnd.app.util.DndLocalization
import com.dnd.app.domain.model.ChoicePathManager
import com.dnd.app.domain.model.Feature
import com.dnd.app.util.stripHtml

@Composable
fun FlatWizardSection(
    title: String,
    applyDefaultContentPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(WizardUiConfig.SECTION_BORDER_WIDTH, WizardUiConfig.SECTION_BORDER_COLOR)
    ) {
        if (title.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WizardUiConfig.SECTION_HEADER_BG_COLOR)
                    .padding(
                        horizontal = WizardUiConfig.SECTION_HEADER_PADDING_HORIZONTAL,
                        vertical = WizardUiConfig.SECTION_HEADER_PADDING_VERTICAL
                    )
            ) {
                Text(
                    text = title,
                    color = WizardUiConfig.SECTION_HEADER_TEXT_COLOR,
                    fontSize = WizardUiConfig.SECTION_HEADER_FONT_SIZE,
                    fontWeight = WizardUiConfig.SECTION_HEADER_FONT_WEIGHT
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WizardUiConfig.SECTION_CONTENT_BG_COLOR)
                .padding(if (applyDefaultContentPadding) WizardUiConfig.SECTION_CONTENT_PADDING else 0.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SmartDropdown(
    options: List<ChoiceOption>,
    selectedId: String?,
    onSelected: (ChoiceOption) -> Unit,
    exclusions: Set<String> = emptySet(),
    placeholder: String = "Выберите..."
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.id == selectedId }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WizardUiConfig.DROPDOWN_HEIGHT)
            .background(WizardUiConfig.DROPDOWN_BG_COLOR)
            .border(WizardUiConfig.SECTION_BORDER_WIDTH, WizardUiConfig.DROPDOWN_BORDER_COLOR)
            .clickable { expanded = true }
            .padding(horizontal = WizardUiConfig.DROPDOWN_PADDING_HORIZONTAL),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedOption?.label ?: placeholder,
                    color = if (selectedOption != null) WizardUiConfig.DROPDOWN_SELECTED_TEXT_COLOR else WizardUiConfig.DROPDOWN_PLACEHOLDER_TEXT_COLOR,
                    fontSize = WizardUiConfig.DROPDOWN_FONT_SIZE,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (selectedOption?.info != null) {
                    Text(
                        text = selectedOption.info,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Open dropdown",
                tint = WizardUiConfig.DROPDOWN_ARROW_TINT
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                val isExcluded = option.id in exclusions
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = option.label,
                                color = if (isExcluded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                fontSize = WizardUiConfig.DROPDOWN_MENU_ITEM_FONT_SIZE
                            )
                            option.info?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    enabled = !isExcluded
                )
            }
        }
    }
}

@Composable
fun BioRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun CollapsibleChoiceSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    CollapsibleActionRow(title, expanded, onToggle, content)
}

@Composable
fun EquipmentGroupHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(WizardUiConfig.EQUIP_GROUP_HEADER_BG_COLOR)
            .padding(vertical = WizardUiConfig.EQUIP_GROUP_HEADER_V_PADDING),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = WizardUiConfig.EQUIP_GROUP_HEADER_FONT_WEIGHT,
            fontSize = WizardUiConfig.EQUIP_GROUP_HEADER_FONT_SIZE,
            color = Color.Black
        )
    }
}


@Composable
fun EquipmentFeatureRenderer(
    feature: Feature,
    selectionSource: com.dnd.app.domain.model.SelectionSource,
    currentSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    unpackedEquipment: Map<String, EquipmentOptionDetails>
) {

    val fullKey = ChoicePathManager.createIndexedKey(
        source = selectionSource,
        featureIndex = feature.index,
        choiceIndex = 0
    )

    val choice = feature.choices.firstOrNull() as? FeatureChoiceDomain.SelectOption ?: return
    val selectionResult = currentSelections[fullKey] as? ChoiceResult.SelectedOptions
    val selectedId = selectionResult?.items?.firstOrNull()

    val rowSize = if (choice.options.size == 3) 3 else 2

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WizardUiConfig.EQUIP_CHOICE_ROW_SPACING)
    ) {
        choice.options.chunked(rowSize).forEach { rowOptions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(WizardUiConfig.EQUIP_CHOICE_ROW_SPACING),
                verticalAlignment = Alignment.Top
            ) {
                rowOptions.forEach { option ->
                    EquipmentChoiceCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        option = option,
                        isSelected = option.id == selectedId,
                        unpackedDetails = unpackedEquipment[option.id],
                        parentKey = fullKey,
                        currentSelections = currentSelections,
                        onSelectionChanged = onSelectionChanged,
                        onSelect = {
                            onSelectionChanged(
                                fullKey,
                                ChoiceResult.SelectedOptions(items = listOf(option.id), proficiencyKind = ProficiencyKind.NONE)
                            )
                        }
                    )
                }
                if (rowOptions.size < rowSize) {
                    Spacer(modifier = Modifier.weight((rowSize - rowOptions.size).toFloat()))
                }
            }
        }
    }
}

@Composable
private fun EquipmentChoiceCard(
    modifier: Modifier = Modifier,
    option: ChoiceOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    unpackedDetails: EquipmentOptionDetails?,
    parentKey: String,
    currentSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit
) {
    val borderColor = if (isSelected) WizardUiConfig.EQUIP_SELECT_BTN_ACTIVE_BG else WizardUiConfig.EQUIP_CARD_BORDER_COLOR

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(WizardUiConfig.EQUIP_CARD_CORNER_RADIUS),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor),
            width = WizardUiConfig.EQUIP_CARD_BORDER_WIDTH
        ),
        colors = CardDefaults.cardColors(
            containerColor = WizardUiConfig.EQUIP_CARD_BG,
            contentColor = Color.Black
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WizardUiConfig.EQUIP_CARD_PADDING)
                .defaultMinSize(minHeight = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WizardUiConfig.EQUIP_CARD_ITEM_LIST_SPACER)
            ) {
                Text(
                    text = option.label,
                    fontWeight = WizardUiConfig.EQUIP_CARD_TITLE_FONT_WEIGHT,
                    fontSize = WizardUiConfig.EQUIP_CARD_TITLE_FONT_SIZE,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )

                if (!unpackedDetails?.description.isNullOrBlank()) {
                    Text(
                        text = unpackedDetails!!.description!!.stripHtml(),
                        fontSize = 12.sp,
                        color = Color.Black,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                unpackedDetails?.contents?.forEach { item ->
                    Text(
                        text = "• $item",
                        fontSize = WizardUiConfig.EQUIP_CARD_ITEM_LIST_SIZE,
                        lineHeight = WizardUiConfig.EQUIP_CARD_ITEM_LIST_LINE_HEIGHT,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            }

            if (option.subChoice != null && option.subChoice is FeatureChoiceDomain.SelectOption) {

                val subKey = ChoicePathManager.append(parentKey, option.id)
                val selection = currentSelections[subKey] as? ChoiceResult.SelectedOptions
                val subSelections = selection?.items ?: emptyList()
                val count = option.subChoice.count

                Spacer(modifier = Modifier.height(8.dp))

                repeat(count) { index ->
                    val currentSubSelectionId = subSelections.getOrNull(index)
                    val exclusionsForThisDropdown = subSelections.filter { it != currentSubSelectionId }.toSet()

                    SmartDropdown(
                        options = option.subChoice.options,
                        selectedId = currentSubSelectionId,
                        onSelected = { selectedItem ->
                            onSelectionChanged(
                                parentKey,
                                ChoiceResult.SelectedOptions(listOf(option.id), proficiencyKind = ProficiencyKind.NONE)
                            )

                            val newList = subSelections.toMutableList()
                            while (newList.size <= index) { newList.add("") }
                            newList[index] = selectedItem.id

                            onSelectionChanged(
                                subKey,
                                ChoiceResult.SelectedOptions(newList.filter { it.isNotBlank() }, proficiencyKind = ProficiencyKind.NONE)
                            )
                        },
                        placeholder = "Предмет ${index + 1}",
                        exclusions = exclusionsForThisDropdown
                    )
                    if (index < count - 1) {
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val allSubChoicesMade = subSelections.filter { it.isNotBlank() }.size == count
                val isEffectivelySelected = isSelected && allSubChoicesMade

                Button(
                    onClick = onSelect,
                    modifier = Modifier.height(WizardUiConfig.EQUIP_SELECT_BTN_HEIGHT),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEffectivelySelected) WizardUiConfig.EQUIP_SELECT_BTN_ACTIVE_BG else WizardUiConfig.EQUIP_SELECT_BTN_BG,
                        contentColor = if (isEffectivelySelected) WizardUiConfig.EQUIP_SELECT_BTN_ACTIVE_TEXT_COLOR else WizardUiConfig.EQUIP_SELECT_BTN_TEXT_COLOR
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    enabled = false
                ) {
                    Text(
                        text = if (isEffectivelySelected) "Выбрано" else if (isSelected) "Выберите предмет" else "Выбрать",
                        fontWeight = WizardUiConfig.EQUIP_SELECT_BTN_FONT_WEIGHT
                    )
                }

            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSelect,
                    modifier = Modifier.height(WizardUiConfig.EQUIP_SELECT_BTN_HEIGHT),
                    shape = RoundedCornerShape(2.dp),
                    border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) WizardUiConfig.EQUIP_SELECT_BTN_ACTIVE_BG else WizardUiConfig.EQUIP_SELECT_BTN_BG,
                        contentColor = if (isSelected) WizardUiConfig.EQUIP_SELECT_BTN_ACTIVE_TEXT_COLOR else WizardUiConfig.EQUIP_SELECT_BTN_TEXT_COLOR
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    enabled = !isSelected
                ) {
                    Text(
                        text = if (isSelected) "Выбрано" else "Выбрать",
                        fontWeight = WizardUiConfig.EQUIP_SELECT_BTN_FONT_WEIGHT
                    )
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\components\WizardCommonComponents.kt
