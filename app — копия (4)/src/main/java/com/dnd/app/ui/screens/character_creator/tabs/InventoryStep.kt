// Имя файла: [app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/InventoryStep.kt]
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.CharacterCreatorViewModel
import com.dnd.app.ui.screens.character_creator.CreatorUiState
import com.dnd.app.ui.screens.character_creator.components.*
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.capitalizeFirst
import java.util.Locale

@Composable
fun InventoryStep(
    state: CreatorUiState,
    viewModel: CharacterCreatorViewModel,
    expandedStates: MutableMap<String, Boolean>
) {
    var showCart by remember { mutableStateOf(false) }

    if (showCart) {
        ShoppingCartSheet(
            cart = state.shoppingCart,
            initialGold = state.initialGold,
            onDismiss = { showCart = false },
            onRemoveItem = viewModel::removeItemFromCart
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val classSelected = state.draft.levelStack.firstOrNull()?.classIndex?.isNotBlank() == true
        if (classSelected) {
            ModeSelector(
                currentMode = state.inventoryMode,
                onModeChange = {
                    // ПРИМЕЧАНИЕ: Здесь должен быть диалог подтверждения перед вызовом ViewModel.
                    // "Смена режима приведет к потере выбранного снаряжения. Продолжить?"
                    viewModel.setInventoryMode(it)
                }
            )
        }

        when (state.inventoryMode) {
            InventoryMode.STANDARD_PACKS -> {
                StandardPacksScreen(
                    staticEquipment = state.draft.baseInfo.staticEquipment,
                    equipmentChoices = state.inventoryStepFeatures,
                    currentSelections = state.draft.baseInfo.inventorySelections,
                    onSelectionChanged = viewModel::onInventorySelectionChange,
                    expandedStates = expandedStates
                )
            }
            InventoryMode.BUY_WITH_GOLD -> {
                ShopScreen(
                    state = state,
                    onSearch = viewModel::searchShop,
                    onCategorySelected = viewModel::selectShopCategory,
                    onBack = viewModel::goBackInShop,
                    onAddToCart = viewModel::addItemToCart,
                    onShowCart = { showCart = true }
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(currentMode: InventoryMode, onModeChange: (InventoryMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(if (currentMode == InventoryMode.STANDARD_PACKS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                .clickable { onModeChange(InventoryMode.STANDARD_PACKS) },
            contentAlignment = Alignment.Center
        ) {
            Text("Стандартный набор", color = if (currentMode == InventoryMode.STANDARD_PACKS) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(if (currentMode == InventoryMode.BUY_WITH_GOLD) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                .clickable { onModeChange(InventoryMode.BUY_WITH_GOLD) },
            contentAlignment = Alignment.Center
        ) {
            Text("Покупка за золото", color = if (currentMode == InventoryMode.BUY_WITH_GOLD) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun StandardPacksScreen(
    staticEquipment: List<String>,
    equipmentChoices: List<Feature>,
    currentSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    expandedStates: MutableMap<String, Boolean>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = WizardUiConfig.WIZARD_STEP_SCREEN_PADDING),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(WizardUiConfig.WIZARD_STEP_SECTION_GAP)
    ) {
        if (staticEquipment.isNotEmpty()) {
            item {
                FlatWizardSection(title = "Гарантированное снаряжение") {
                    Column {
                        staticEquipment.forEach { itemIndex ->
                            Text("• ${DndLocalization.translateSkill(itemIndex).capitalizeFirst()}")
                        }
                    }
                }
            }
        }

        items(equipmentChoices, key = { it.index }) { feature ->
            FlatWizardSection(title = feature.name) {
                feature.choices.forEach { choice ->
                    FeatureChoiceBlock(
                        choice = choice,
                        allSelections = currentSelections,
                        onSelectionUpdated = onSelectionChanged,
                        selectionKey = feature.index,
                        expandedStates = expandedStates
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopScreen(
    state: CreatorUiState,
    onSearch: (String) -> Unit,
    onCategorySelected: (ShopCategory) -> Unit,
    onBack: () -> Unit,
    onAddToCart: (ShopItem) -> Unit,
    onShowCart: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        ShopTopBar(
            money = state.remainingGold,
            cartSize = state.shoppingCart.size,
            onSearch = onSearch,
            onShowCart = onShowCart
        )
        Spacer(modifier = Modifier.height(8.dp))

        ShopHeader(
            title = state.currentShopTitle,
            onBack = onBack,
            showBack = state.shopView == ShopView.ITEMS || state.currentShopTitle != "Магазин"
        )

        if (state.shopView == ShopView.CATEGORIES) {
            CategoryList(categories = state.shopCategories, onCategorySelected = onCategorySelected)
        } else {
            ItemList(items = state.shopItems, onAddToCart = onAddToCart, remainingGold = state.remainingGold)
        }
    }
}

@Composable
private fun ShopHeader(title: String, onBack: () -> Unit, showBack: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(0.15f)) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            }
        }
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.7f)
        )
        Spacer(modifier = Modifier.weight(0.15f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopTopBar(
    money: Money,
    cartSize: Int,
    onSearch: (String) -> Unit,
    onShowCart: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Баланс: ${money}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BadgedBox(badge = { if (cartSize > 0) Badge { Text("$cartSize") } }) {
                Icon(Icons.Default.ShoppingCart, "Корзина", modifier = Modifier.clickable(onClick = onShowCart), tint = MaterialTheme.colorScheme.primary)
            }
        }
        BasicTextField(
            value = query, onValueChange = { query = it; onSearch(it) },
            modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            decorationBox = { innerTextField ->
                Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Название предмета...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        innerTextField()
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryList(categories: List<ShopCategory>, onCategorySelected: (ShopCategory) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        items(categories, key = { it.index }) { category ->
            Card(
                onClick = { onCategorySelected(category) },
                modifier = Modifier.height(60.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                    Text(category.name, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ItemList(
    items: List<ShopItem>,
    onAddToCart: (ShopItem) -> Unit,
    remainingGold: Money
) {
    Column {
        if (items.isEmpty()){
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){ Text("Ничего не найдено") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(items, key = { it.index }) { item ->
                    val canAfford = remainingGold >= item.cost
                    ShopItemRow(item = item, onAdd = { onAddToCart(item) }, enabled = canAfford)
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun ShopItemRow(item: ShopItem, onAdd: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(item.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = if(enabled) LocalContentColor.current else Color.Gray)
        Text("${item.cost}", fontSize = 12.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.End, color = if(enabled) LocalContentColor.current else Color.Gray)
        Text(item.weight?.let { "${it} фнт." } ?: "-", fontSize = 12.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.End, color = Color.Gray)
        IconButton(onClick = onAdd, modifier = Modifier.size(32.dp), enabled = enabled) {
            Icon(Icons.Default.AddShoppingCart, "Добавить", tint = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingCartSheet(
    cart: List<ShopItem>,
    initialGold: Money,
    onDismiss: () -> Unit,
    onRemoveItem: (ShopItem) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Text("Корзина", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            if (cart.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Ваша корзина пуста")
                }
            } else {
                val totalCost = cart.fold(Money()) { acc, item -> acc + item.cost }
                LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max=300.dp)) {
                    items(cart.groupBy { it.index }.values.map { it.first() to it.size } ) { (item, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("${item.name} ${if (count > 1) "x$count" else ""}", modifier = Modifier.weight(1f))
                            Text("${item.cost * count}", modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { onRemoveItem(item) }) {
                                Icon(Icons.Default.RemoveCircleOutline, "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Итого:", fontWeight = FontWeight.Bold)
                    Text("${totalCost}", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Останется:", fontWeight = FontWeight.Bold)
                    Text("${initialGold - totalCost}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: [app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/InventoryStep.kt]