// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\InventoryTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.sheet.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.Money
import com.dnd.app.domain.model.ShopItem
import com.dnd.app.ui.screens.sheet.CharacterSheetUiState
import com.dnd.app.ui.screens.sheet.CharacterSheetViewModel
import com.dnd.app.ui.screens.sheet.DisplayInventoryItem
import com.dnd.app.ui.screens.sheet.components.dialogs.ItemActionDialog
import com.dnd.app.ui.screens.sheet.components.money.MoneyWidget
import com.dnd.app.util.stripHtml

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InventoryTab(
    state: CharacterSheetUiState,
    viewModel: CharacterSheetViewModel,
    onEquipToggle: (String) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onMoneyUpdate: (String, Int) -> Unit,
    onSellItem: (String, Int) -> Unit,
    onOpenShop: () -> Unit,
) {
    val data = state.data?.base ?: return

    var showDescriptionDialog by remember { mutableStateOf<DisplayInventoryItem?>(null) }
    var deleteDialogItem by remember { mutableStateOf<DisplayInventoryItem?>(null) }
    var sellDialogItem by remember { mutableStateOf<DisplayInventoryItem?>(null) }


    var expandedPacks by remember { mutableStateOf(setOf<String>()) }
    val onTogglePack: (String) -> Unit = { id ->
        expandedPacks = if (id in expandedPacks) expandedPacks - id else expandedPacks + id
    }

    if (deleteDialogItem != null) {
        val item = deleteDialogItem!!
        ItemActionDialog(
            title = "Удалить: ${item.name}",
            totalQuantity = item.quantity,
            confirmButtonText = "УДАЛИТЬ",
            onDismiss = { deleteDialogItem = null },
            onConfirm = { amountToRemove ->
                onQuantityChange(item.uniqueId, item.quantity - amountToRemove)
                deleteDialogItem = null
            }
        )
    }

    if (sellDialogItem != null) {
        val item = sellDialogItem!!
        ItemActionDialog(
            title = "Продать: ${item.name}",
            totalQuantity = item.quantity,
            confirmButtonText = "ПРОДАТЬ",
            onDismiss = { sellDialogItem = null },
            onConfirm = { amountToSell ->
                onSellItem(item.uniqueId, amountToSell)
                sellDialogItem = null
            },
            pricePerUnit = item.sellPrice
        )
    }

    showDescriptionDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDescriptionDialog = null },
            title = { Text(item.name) },
            text = {
                Column {
                    if (item.isPack) {
                        Surface(color = Color(0xFFFFF9C4), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text("Контейнер: содержимое отображается вложенным списком.", modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = item.description.stripHtml().ifBlank { "Описание отсутствует." },
                        fontStyle = if (item.description.isBlank()) FontStyle.Italic else FontStyle.Normal
                    )
                }
            },
            confirmButton = { Button(onClick = { showDescriptionDialog = null }) { Text("OK") } }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            InventoryHeader(
                searchQuery = state.lootSearchQuery,
                onSearchQueryChange = viewModel::searchLoot,
                lootResults = state.lootSearchResults,
                onLootItemClick = viewModel::addLootItem,
                coins = data.coins,
                onMoneyUpdate = onMoneyUpdate,
                formattedTotalWeight = data.formattedTotalWeight,
                maxWeight = data.maxCarryWeight,
                isEncumbered = data.isEncumbered,
                onOpenShop = onOpenShop
            )
        }


        renderHierarchicalSection("Оружие", data.weapons, expandedPacks, onTogglePack, onEquipToggle, viewModel::toggleAttunement, { deleteDialogItem = it }, { showDescriptionDialog = it }, { sellDialogItem = it })
        renderHierarchicalSection("Доспехи и Щиты", data.armorAndShields, expandedPacks, onTogglePack, onEquipToggle, viewModel::toggleAttunement, { deleteDialogItem = it }, { showDescriptionDialog = it }, { sellDialogItem = it })
        renderHierarchicalSection("Снаряжение", data.gear, expandedPacks, onTogglePack, onEquipToggle, viewModel::toggleAttunement, { deleteDialogItem = it }, { showDescriptionDialog = it }, { sellDialogItem = it })
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.renderHierarchicalSection(
    title: String,
    items: List<DisplayInventoryItem>,
    expandedPacks: Set<String>,
    onTogglePack: (String) -> Unit,
    onEquipToggle: (String) -> Unit,
    onAttunementToggle: (String) -> Unit,
    onQuantityClick: (DisplayInventoryItem) -> Unit,
    onLongPress: (DisplayInventoryItem) -> Unit,
    onSellClick: (DisplayInventoryItem) -> Unit
) {
    if (items.isEmpty()) return

    stickyHeader {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth().background(Color(0xFF424242)).padding(vertical = 4.dp, horizontal = 8.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }


    val rootItems = items.filter { it.containerId == null }

    rootItems.forEach { root ->
        item(key = root.uniqueId) {
            InventoryItemRow(root, root.uniqueId in expandedPacks, 0, onEquipToggle, onAttunementToggle, onQuantityClick, onLongPress, onSellClick, { onTogglePack(root.uniqueId) })
        }

        if (root.isPack && root.uniqueId in expandedPacks) {
            val children = items.filter { it.containerId == root.uniqueId }
            items(children, key = { it.uniqueId }) { child ->
                InventoryItemRow(child, false, 1, onEquipToggle, onAttunementToggle, onQuantityClick, onLongPress, onSellClick)
            }
        }
    }
}

@Composable
private fun InventoryHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    lootResults: List<ShopItem>,
    onLootItemClick: (ShopItem) -> Unit,
    coins: Money,
    onMoneyUpdate: (String, Int) -> Unit,
    formattedTotalWeight: String,
    maxWeight: Int,
    isEncumbered: Boolean,
    onOpenShop: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onOpenShop, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Icon(Icons.Default.ShoppingCart, null); Spacer(Modifier.width(8.dp)); Text("ОТКРЫТЬ МАГАЗИН")
        }

        val focusManager = LocalFocusManager.current
        Column {
            OutlinedTextField(
                value = searchQuery, onValueChange = onSearchQueryChange, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Найти лут...", color = MaterialTheme.colorScheme.onSurface) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Close, null) } },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            if (lootResults.isNotEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).border(1.dp, Color.Gray, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    LazyColumn {
                        items(lootResults, key = { it.index }) { item ->
                            ListItem(headlineContent = { Text(item.name) }, supportingContent = { item.description?.let { d -> Text(d, maxLines = 1) } }, trailingContent = { Text("ВЗЯТЬ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }, modifier = Modifier.clickable { onLootItemClick(item) })
                            Divider()
                        }
                    }
                }
            }
        }

        MoneyWidget(coins, onMoneyUpdate, Modifier)

        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).border(1.dp, Color.Gray).padding(vertical = 8.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Вес:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "$formattedTotalWeight / $maxWeight фнт.", color = if (isEncumbered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InventoryItemRow(
    item: DisplayInventoryItem,
    isExpanded: Boolean,
    indentLevel: Int,
    onEquip: (String) -> Unit,
    onAttune: (String) -> Unit,
    onQty: (DisplayInventoryItem) -> Unit,
    onLong: (DisplayInventoryItem) -> Unit,
    onSell: (DisplayInventoryItem) -> Unit,
    onToggle: () -> Unit = {}
) {
    val bgColor = if (indentLevel > 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val itemPrimary = MaterialTheme.colorScheme.onSurface
    val itemSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val qtyBg = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = (indentLevel * 20).dp).heightIn(min = 56.dp).background(bgColor).border(1.dp, if (indentLevel > 0) Color(0xFFBDBDBD) else Color(0xFF9E9E9E))
            .combinedClickable(onClick = { if (item.isPack) onToggle() else onLong(item) }, onLongClick = { onLong(item) }).padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        if (item.isPack) {
            Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(24.dp).clickable { onToggle() }, tint = itemPrimary)
        } else if (indentLevel > 0) {
            Icon(Icons.Default.SubdirectoryArrowRight, null, modifier = Modifier.size(16.dp), tint = itemPrimary)
        } else Spacer(Modifier.width(24.dp))


        if (item.isSellable) IconButton(onClick = { onSell(item) }, modifier = Modifier.size(32.dp)) { Text("$", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp) } else Spacer(Modifier.width(32.dp))


        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isPack) Icon(Icons.Default.Inventory, null, modifier = Modifier.size(14.dp).padding(end = 4.dp), tint = Color(0xFFFBC02D))
                Text(text = item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (indentLevel == 0) FontWeight.Bold else FontWeight.Normal, color = itemPrimary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${item.formattedWeight} фнт.", style = MaterialTheme.typography.bodySmall, color = itemSecondary)
                if (item.rarity != null) { Spacer(Modifier.width(8.dp)); Text(text = item.rarity, style = MaterialTheme.typography.bodySmall, color = itemSecondary, fontStyle = FontStyle.Italic) }
            }
        }


        if (item.requiresAttunement) IconButton(onClick = { onAttune(item.uniqueId) }, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = if (item.isAttuned) Icons.Default.Star else Icons.Default.StarOutline, contentDescription = null, tint = if (item.isAttuned) Color(0xFFFFB300) else itemPrimary)
        }


        Box(modifier = Modifier.width(60.dp).background(qtyBg, RoundedCornerShape(4.dp)).clickable(enabled = item.canChangeQuantity, onClick = { onQty(item) }).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Text(text = "x${item.quantity}", textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (item.canChangeQuantity) itemPrimary else itemSecondary)
        }


        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (item.canBeEquipped) Checkbox(checked = item.isEquipped, onCheckedChange = { onEquip(item.uniqueId) }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\sheet\tabs\InventoryTab.kt





