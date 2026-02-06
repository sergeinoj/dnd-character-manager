// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/InventoryStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
                onModeChange = { viewModel.setInventoryMode(it) }
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
                    onBackToCategories = viewModel::goBackToCategories,
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
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(if (currentMode == InventoryMode.STANDARD_PACKS) Color.White else Color.LightGray)
                .clickable { onModeChange(InventoryMode.STANDARD_PACKS) },
            contentAlignment = Alignment.Center
        ) {
            Text("Стандартный набор", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(if (currentMode == InventoryMode.BUY_WITH_GOLD) Color.White else Color.LightGray)
                .clickable { onModeChange(InventoryMode.BUY_WITH_GOLD) },
            contentAlignment = Alignment.Center
        ) {
            Text("Покупка за золото", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
    onBackToCategories: () -> Unit,
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

        if (state.shopView == ShopView.CATEGORIES) {
            CategoryList(categories = state.shopCategories, onCategorySelected = onCategorySelected)
        } else {
            ItemList(
                items = state.shopItems,
                categoryName = state.selectedShopCategory?.name,
                onBack = onBackToCategories,
                onAddToCart = onAddToCart
            )
        }
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
        modifier = Modifier.fillMaxWidth().background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Монет осталось: ${money}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            BadgedBox(badge = { if (cartSize > 0) Badge { Text("$cartSize") } }) {
                Icon(Icons.Default.ShoppingCart, "Cart", modifier = Modifier.clickable(onClick = onShowCart))
            }
        }
        BasicTextField(
            value = query, onValueChange = { query = it; onSearch(it) },
            modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color.Gray),
            textStyle = TextStyle(fontSize = 14.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            decorationBox = { innerTextField ->
                Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Название...", color = Color.Gray)
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun CategoryList(categories: List<ShopCategory>, onCategorySelected: (ShopCategory) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(categories, key = { it.index }) { category ->
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.White)
                    .border(1.dp, Color.Gray).clickable { onCategorySelected(category) }.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category.name, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ItemList(
    items: List<ShopItem>,
    categoryName: String?,
    onBack: () -> Unit,
    onAddToCart: (ShopItem) -> Unit
) {
    Column {
        if (categoryName != null) {
            Button(onClick = onBack) { Text("Вернуться к категориям") }
            Spacer(Modifier.height(4.dp))
        }
        if (items.isEmpty()){
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){ Text("Ничего не найдено") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(items, key = { it.index }) { item ->
                    ShopItemRow(item = item, onAdd = { onAddToCart(item) })
                }
            }
        }
    }
}

@Composable
private fun ShopItemRow(item: ShopItem, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.White).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(item.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${item.cost.gp} зм", fontSize = 12.sp, modifier = Modifier.width(45.dp), textAlign = TextAlign.End)
        Text(item.weight?.let { "${it} фнт." } ?: "-", fontSize = 12.sp, modifier = Modifier.width(45.dp), textAlign = TextAlign.End, color = Color.Gray)
        IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, "Add")
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
                Text("Пусто")
            } else {
                val totalCost = cart.fold(Money()) { acc, item -> acc + item.cost }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cart) { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name, modifier = Modifier.weight(1f))
                            Text("${item.cost.gp} зм", modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { onRemoveItem(item) }) {
                                Icon(Icons.Default.Close, "Remove", tint = Color.Red)
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
                    Text("${initialGold - totalCost}", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/InventoryStep.kt