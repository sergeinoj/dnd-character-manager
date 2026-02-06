// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/BioStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.components.*
import com.dnd.app.util.stripHtml

@Composable
fun BioStep(
    name: String, onNameChange: (String) -> Unit, availableAlignments: List<AlignmentEntity>,
    selectedAlignment: String, onAlignmentSelect: (String) -> Unit, availableBackgrounds: List<Background>,
    selectedBackground: String, onBackgroundSelect: (Background) -> Unit, backgroundFeatures: List<Feature>,
    currentSelections: Map<String, ChoiceResult>, onSelectionChanged: (String, ChoiceResult) -> Unit,
    personalityTrait: String, ideal: String, bond: String, flaw: String,
    onRollTrait: (String) -> Unit, onManualBioChange: (String, String) -> Unit,
    expandedStates: MutableMap<String, Boolean> // ИСПРАВЛЕНО: Принимаем состояние
) {
    val selectedBg = availableBackgrounds.find { it.name == selectedBackground }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlatWizardSection(title = "Личность") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BioRow("Имя", name, onNameChange)
                Column {
                    Text("Мировоззрение", fontSize = 12.sp, color = Color.DarkGray)
                    val opts = availableAlignments.map { ChoiceOption(it.indexName, it.name) }
                    SmartDropdown(opts, selectedAlignment, onSelected = { onAlignmentSelect(it.id) })
                }
            }
        }
        FlatWizardSection(title = "Предыстория") {
            val opts = availableBackgrounds.map { ChoiceOption(it.name, it.name) }
            SmartDropdown(opts, selectedBackground, onSelected = { o -> availableBackgrounds.find { it.name == o.id }?.let { onBackgroundSelect(it) } })
        }
        if (selectedBg != null) {
            FlatWizardSection(title = "Характер") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RandomTraitRow("Черта", personalityTrait, { onRollTrait("personality") }, { onManualBioChange("personality", it) })
                    RandomTraitRow("Идеал", ideal, { onRollTrait("ideal") }, { onManualBioChange("ideal", it) })
                    RandomTraitRow("Привязанность", bond, { onRollTrait("bond") }, { onManualBioChange("flaw", it) })
                    RandomTraitRow("Изъян", flaw, { onRollTrait("flaw") }, { onManualBioChange("flaw", it) })
                }
            }
        }
        backgroundFeatures.forEach { f ->
            FlatWizardSection(title = f.name) {
                Column {
                    if (f.description.isNotBlank()) Text(f.description.stripHtml(), fontSize = 14.sp)
                    f.choices.forEach { c ->
                        FeatureChoiceBlock(
                            choice = c,
                            allSelections = currentSelections,
                            onSelectionUpdated = onSelectionChanged,
                            selectionKey = f.index,
                            expandedStates = expandedStates
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun RandomTraitRow(label: String, value: String, onRoll: () -> Unit, onManualChange: (String) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424242))
            IconButton(onClick = onRoll, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, null, tint = Color(0xFF1B5E20)) }
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color.Gray).padding(8.dp)) {
            BasicTextField(value = value, onValueChange = onManualChange, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp), textStyle = TextStyle(fontSize = 13.sp, color = Color.Black), decorationBox = { if(value.isEmpty()) Text("Нажми 🎲 или впиши...", color = Color.Gray, fontSize = 13.sp); it() })
        }
    }
}

@Composable
fun BioRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(36.dp).border(1.dp, Color.Gray), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color(0xFFE0E0E0)).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) { Text(label, fontSize = 13.sp) }
        Box(modifier = Modifier.weight(0.6f).fillMaxHeight().background(Color.White).padding(8.dp), contentAlignment = Alignment.CenterStart) {
            BasicTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(fontSize = 13.sp))
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/BioStep.kt