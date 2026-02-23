package com.dnd.app.ui.screens.sheet.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.Money
import com.dnd.app.domain.model.ShopItem
import com.dnd.app.ui.screens.sheet.CharacterSheetUiState
import com.dnd.app.ui.screens.sheet.MerchantManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopModal(
    state: CharacterSheetUiState,
    onDismiss: () -> Unit,
    merchantManager: MerchantManager,
    onPurchase: (ShopItem) -> Unit
) {
    val shopState by merchantManager.state.collectAsState()
    val coins = state.data?.base?.coins ?: Money()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(shopState.currentTitle, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))

                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = coins.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = 6.dp))

            var query by remember { mutableStateOf("") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; merchantManager.search(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("\u041f\u043e\u0438\u0441\u043a...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.padding(top = 8.dp))

            if (shopState.categories.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(shopState.categories) { cat ->
                        ListItem(
                            headlineContent = { Text(cat.name) },
                            leadingContent = { Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.clickable { merchantManager.selectCategory(cat) }
                        )
                        Divider()
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shopState.items) { item ->
                        ShopItemCard(
                            item = item,
                            canAfford = coins.toCopper() >= item.cost.toCopper(),
                            isBusy = state.isBusy,
                            onPurchase = onPurchase
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    canAfford: Boolean,
    isBusy: Boolean,
    onPurchase: (ShopItem) -> Unit
) {
    var expanded by remember(item.index) { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("${item.cost}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (expanded) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    item.weight?.let {
                        Text("\u0412\u0435\u0441: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { onPurchase(item) }, enabled = canAfford && !isBusy) {
                    Text("\u041a\u0443\u043f\u0438\u0442\u044c")
                }
            }
        }
    }
}
