package com.dnd.app.ui.screens.sheet.components.combat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.DndConstants
import com.dnd.app.domain.model.snapshot.ActionType
import com.dnd.app.domain.model.snapshot.CombatAction
import com.dnd.app.domain.model.snapshot.ResourcePoolSnapshot
import com.dnd.app.util.DndLocalization

@Composable
fun ClassCombatConsole(
    resourcePools: List<ResourcePoolSnapshot>,
    resourceCharges: Map<String, Int>,
    classActions: List<CombatAction>,
    activeEffects: Set<String>,
    onSpendResource: (String) -> Unit,
    onActionClick: (CombatAction) -> Unit,
    onTransform: (() -> Unit)? = null,
    isTransformed: Boolean = false,
    transformationName: String? = null,
    onResetTransformation: (() -> Unit)? = null,
    maxHeightDp: Int? = null
) {
    var expandedPoolId by remember { mutableStateOf<String?>(null) }
    var isPanelExpanded by remember { mutableStateOf(true) }
    var invocationsExpanded by remember { mutableStateOf(false) }
    val wildShapeLabel = DndLocalization.translateProficiency("Wild Shape Uses").lowercase()
    val wildShapePoolId = resourcePools.firstOrNull { pool ->
        val lowerName = pool.name.lowercase()
        val lowerId = pool.id.lowercase()
        val matchesLabel = wildShapeLabel.isNotBlank() && lowerName.contains(wildShapeLabel, ignoreCase = true)
        matchesLabel || (lowerName.contains("wild") && lowerName.contains("shape")) ||
            (lowerName.contains("\u0434\u0438\u043A") && lowerName.contains("\u043E\u0431\u043B\u0438\u043A")) ||
            (lowerId.contains("wild") && lowerId.contains("shape"))
    }?.id
    val canShowWildShapeAction = onTransform != null && wildShapePoolId != null
    val consoleBg by animateColorAsState(
        targetValue = when {
            DndConstants.MechanicKeys.EFFECT_RAGE in activeEffects -> Color(0xFF421010)
            DndConstants.MechanicKeys.EFFECT_SNEAK_ATTACK in activeEffects -> Color(0xFF102042)
            else -> Color(0xFF212121)
        },
        label = "console_bg"
    )
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(consoleBg)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .then(if (isPanelExpanded && maxHeightDp != null) Modifier.heightIn(max = maxHeightDp.dp) else Modifier)
            .then(if (isPanelExpanded) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ExpandHeader(title = "\u0414\u0415\u0419\u0421\u0422\u0412\u0418\u042F / \u0420\u0415\u0421\u0423\u0420\u0421\u042B", isExpanded = isPanelExpanded, onToggle = { isPanelExpanded = !isPanelExpanded })
        if (!isPanelExpanded) return@Column
        @Composable
        fun renderAction(action: CombatAction, level: Int) {
            ManeuverRow(action, onActionClick, resourcePools, indentLevel = level)
            val canShowChildren = action.nestedActions.isNotEmpty() && (!action.isToggle || action.isActive)
            if (canShowChildren) action.nestedActions.forEach { child -> renderAction(child, level + 1) }
        }
        resourcePools.forEach { pool ->
            val actionsInPool = classActions.filter { it.resourceId == pool.id }
            val isExpanded = expandedPoolId == pool.id
            val hasContent = actionsInPool.isNotEmpty()
            val spent = resourceCharges[pool.id] ?: 0
            val remaining = (pool.max - spent).coerceAtLeast(0)
            val showWildShapeButton = pool.id == wildShapePoolId && canShowWildShapeAction
            val showResetButton = showWildShapeButton && isTransformed && onResetTransformation != null
            InteractiveResourceBar(
                pool = pool,
                spent = spent,
                isExpanded = isExpanded,
                hasNestedActions = hasContent,
                onToggleExpand = {
                    if (!isPanelExpanded) {
                        isPanelExpanded = true
                        expandedPoolId = pool.id
                    } else if (isExpanded) expandedPoolId = null else expandedPoolId = pool.id
                },
                onSpend = onSpendResource,
                actionLabel = when {
                    showResetButton -> transformationName?.let { "\u0421\u0431\u0440\u043E\u0441\u0438\u0442\u044C: $it" } ?: "\u0421\u0431\u0440\u043E\u0441\u0438\u0442\u044C \u043E\u0431\u043B\u0438\u043A"
                    showWildShapeButton -> "\u0412\u044B\u0431\u0440\u0430\u0442\u044C \u0437\u0432\u0435\u0440\u044F"
                    else -> null
                },
                actionIcon = when {
                    showResetButton -> Icons.Default.Close
                    showWildShapeButton -> Icons.Default.Pets
                    else -> null
                },
                actionEnabled = if (showResetButton) true else remaining > 0,
                onAction = when {
                    showResetButton -> onResetTransformation
                    showWildShapeButton -> onTransform
                    else -> null
                }
            )
            AnimatedVisibility(visible = isPanelExpanded && isExpanded) {
                Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    actionsInPool.forEach { action -> renderAction(action, 0) }
                }
            }
        }
        if (isPanelExpanded) {
            val poolIds = resourcePools.map { it.id }.toSet()
            val orphanActions = classActions.filter { it.resourceId == null || it.resourceId !in poolIds }
            val invocations = orphanActions.filter { act ->
                val n = act.name.lowercase()
                n.contains("\u0432\u043E\u0437\u0437\u0432") || n.contains("invocation") || act.effectId?.lowercase()?.contains("invocation") == true
            }
            val otherOrphans = orphanActions - invocations.toSet()
            if (invocations.isNotEmpty()) {
                if (!isPanelExpanded) invocationsExpanded = false
                InvocationToggleRow(title = "\u0412\u041E\u0417\u0417\u0412\u0410\u041D\u0418\u042F", isExpanded = invocationsExpanded, onToggle = { invocationsExpanded = !invocationsExpanded })
                AnimatedVisibility(visible = invocationsExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 12.dp, top = 4.dp)) {
                        invocations.forEach { action -> renderAction(action, 0) }
                    }
                }
            }
            if (otherOrphans.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { otherOrphans.forEach { action -> renderAction(action, 0) } }
        }
    }
}

@Composable
private fun InvocationToggleRow(title: String, isExpanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "invocation_rotation")
    Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp).rotate(rotation))
            Spacer(Modifier.width(6.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
        Text(if (isExpanded) "\u0421\u043A\u0440\u044B\u0442\u044C" else "\u041F\u043E\u043A\u0430\u0437\u0430\u0442\u044C", color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun ExpandHeader(title: String, isExpanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "console_expand_rotation")
    Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp).rotate(rotation))
            Spacer(Modifier.width(6.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Text(if (isExpanded) "\u0421\u0432\u0435\u0440\u043D\u0443\u0442\u044C" else "\u0420\u0430\u0437\u0432\u0435\u0440\u043D\u0443\u0442\u044C", color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun InteractiveResourceBar(
    pool: ResourcePoolSnapshot,
    spent: Int,
    isExpanded: Boolean,
    hasNestedActions: Boolean,
    onToggleExpand: () -> Unit,
    onSpend: (String) -> Unit,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null
) {
    val color = Color(pool.uiColorHex)
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "rotation")
    Column(modifier = Modifier.fillMaxWidth().run { if (hasNestedActions) clickable(onClick = onToggleExpand) else this }.padding(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasNestedActions) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(16.dp).rotate(rotation))
                    Spacer(Modifier.width(4.dp))
                }
                Text(text = pool.name.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White)
            }
            Text(text = "${pool.max - spent} \u041E\u0421\u0422\u0410\u041B\u041E\u0421\u042C", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(pool.max) { index ->
                    val isSpent = index < spent
                    Box(modifier = Modifier.size(18.dp).border(1.dp, color, CircleShape).background(if (isSpent) Color.Transparent else color, CircleShape).clickable(enabled = !isSpent) { onSpend(pool.id) })
                }
            }
            if (actionLabel != null && onAction != null) {
                val onResourceColor = if (color.luminance() > 0.5f) Color.Black else Color.White
                Button(onClick = onAction, enabled = actionEnabled, modifier = Modifier.height(30.dp), colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = onResourceColor, disabledContainerColor = color, disabledContentColor = onResourceColor)) {
                    if (actionIcon != null) {
                        Icon(imageVector = actionIcon, contentDescription = actionLabel, modifier = Modifier.size(16.dp), tint = onResourceColor)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(actionLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = onResourceColor)
                }
            }
        }
    }
}

@Composable
private fun ManeuverRow(
    action: CombatAction,
    onClick: (CombatAction) -> Unit,
    resourcePools: List<ResourcePoolSnapshot>,
    indentLevel: Int = 0
) {
    val isBlocked = action.isBlocked
    val paddingStart = (12 + (indentLevel * 12)).dp
    val hasDescription = action.description?.isNotBlank() == true
    var showDescription by remember(action.uniqueId) { mutableStateOf(false) }
    if (showDescription && hasDescription) {
        AlertDialog(onDismissRequest = { showDescription = false }, title = { Text(action.name) }, text = { Text(action.description ?: "") }, confirmButton = { TextButton(onClick = { showDescription = false }) { Text("Ок") } })
    }
    val linkedPool = resourcePools.find { it.id == action.resourceId }
    val poolColor = linkedPool?.uiColorHex?.let { Color(it) } ?: Color(0xFF455A64)
    val bgColor = if (action.type == ActionType.FEATURE_TOGGLE && action.isActive) poolColor else Color(0xFF37474F)
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(start = if (indentLevel > 0) 12.dp else 0.dp).background(bgColor, RoundedCornerShape(4.dp))
            .clickable(enabled = !isBlocked) { onClick(action) }.padding(start = paddingStart, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconColor = if (isBlocked) Color.Gray else if (action.actionCostDescription == "0") Color(0xFFA5D6A7) else Color(0xFFFFD54F)
        Icon(imageVector = if (isBlocked) Icons.Default.Lock else Icons.Default.FlashOn, contentDescription = null, tint = iconColor, modifier = Modifier.size(if (indentLevel > 0) 12.dp else 14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = action.name, color = Color.White, fontSize = 12.sp, fontWeight = if (action.isActive) FontWeight.ExtraBold else FontWeight.Bold, modifier = Modifier.weight(1f))
        if (hasDescription) {
            IconButton(onClick = { showDescription = true }, modifier = Modifier.size(28.dp).padding(end = 4.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = Color.White)
            }
        }
        if (action.type == ActionType.FEATURE_TOGGLE) {
            Switch(
                checked = action.isActive,
                onCheckedChange = { onClick(action) },
                enabled = !isBlocked,
                modifier = Modifier.graphicsLayer(scaleX = 0.6f, scaleY = 0.6f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFC28212),
                    checkedTrackColor = Color(0xFF007477),
                    uncheckedThumbColor = Color(0xFF6A3A00),
                    uncheckedTrackColor = Color(0xFF007477)
                )
            )
        } else if (action.damageFormula.isNotBlank() && action.damageFormula != "\u2014") {
            Text(action.damageFormula, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CombatActionRow(
    action: CombatAction,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enableLongClick: Boolean = false,
    isConcentratingThisSpell: Boolean = false
) {
    val isDarkTheme = isSystemInDarkTheme()
    val isBlocked = action.isBlocked
    val bgColor = when {
        isConcentratingThisSpell && isDarkTheme -> Color(0xFF4A3800)
        isConcentratingThisSpell -> Color(0xFFFFE082)
        isDarkTheme && isBlocked -> Color(0xFF2A2A2A)
        isDarkTheme && action.isToggle && action.isActive -> Color(0xFF1F3A28)
        isDarkTheme && action.isActive -> Color(0xFF3A1F24)
        isDarkTheme -> Color(0xFF1E1E1E)
        isBlocked -> Color(0xFFEEEEEE)
        action.isToggle && action.isActive -> Color(0xFFE8F5E9)
        action.isActive -> Color(0xFFFFEBEE)
        else -> Color.White
    }
    val concentrationBorderColor = if (isDarkTheme) Color(0xFFFFB300) else Color(0xFFFF8F00)
    val primaryTextColor = if (isDarkTheme) Color(0xFFF2F2F2) else Color(0xFF212121)
    val secondaryTextColor = if (isDarkTheme) Color(0xFFD0D0D0) else Color(0xFF424242)
    val rightCellBg = if (isDarkTheme) Color(0xFF252525) else Color(0xFFEEEEEE)
    val expandedBg = if (isDarkTheme) Color(0xFF232323) else Color(0xFFFAFAFA)
    val icon = when (action.type) {
        ActionType.WEAPON -> if (action.range.contains("/")) "\uD83C\uDFF9" else "\u2694\uFE0F"
        ActionType.CANTRIP -> "\uD83D\uDD25"
        ActionType.SPELL -> "\u2728"
        ActionType.ITEM -> "\uD83D\uDD2E"
        ActionType.FEATURE_TOGGLE -> "\u26A1"
    }
    var isExpanded by remember { mutableStateOf(false) }
    val hasNested = action.nestedActions.isNotEmpty()
    val displayHit = action.saveDcInfo ?: action.hitBonus
    Column(modifier = Modifier.fillMaxWidth().background(bgColor).border(2.dp, when {
        isConcentratingThisSpell -> concentrationBorderColor
        action.isActive -> Color.Red
        else -> Color(0xFF9E9E9E)
    })) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).combinedClickable(enabled = !isBlocked, onClick = { if (hasNested) isExpanded = !isExpanded else onClick() }, onLongClick = when {
            enableLongClick && onLongClick != null -> onLongClick
            action.isSpell && (action.level ?: 0) > 0 && onLongClick != null -> onLongClick
            else -> null
        }), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(40.dp).fillMaxHeight(), contentAlignment = Alignment.Center) { Text(if (isBlocked) "\uD83D\uDD12" else icon, fontSize = 20.sp) }
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp, horizontal = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = action.name + if (action.quantity != null) " x${action.quantity}" else "", fontWeight = FontWeight.Bold, color = primaryTextColor, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (isConcentratingThisSpell) {
                        Text(text = " C", fontWeight = FontWeight.Black, color = concentrationBorderColor, fontSize = 14.sp)
                    }
                    if (hasNested) Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                if (isBlocked && action.parentEffectId != null) Text(text = "\u0422\u0440\u0435\u0431\u0443\u0435\u0442\u0441\u044F: ${action.parentEffectId.substringAfter("effect_").replace("_active", "").uppercase()}", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                else if (action.ammoType != null && action.currentCharges != null) Text(text = "\u0411\u043E\u0435\u043F\u0440\u0438\u043F\u0430\u0441\u044B: ${action.currentCharges}", fontSize = 10.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                else if (action.currentCharges != null && action.maxCharges != null) Text(text = "\u0417\u0430\u0440\u044F\u0434\u044B: ${action.currentCharges}/${action.maxCharges}", fontSize = 10.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                else if (action.range != "\u2014") Text(text = action.range, fontSize = 10.sp, color = secondaryTextColor)
                if (action.triggerDescriptions.isNotEmpty()) Text(text = action.triggerDescriptions.joinToString(" \u2022 "), fontSize = 9.sp, color = secondaryTextColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box(modifier = Modifier.width(60.dp).fillMaxHeight().background(rightCellBg).border(0.5.dp, Color(0xFF9E9E9E)), contentAlignment = Alignment.Center) {
                Text(text = if (displayHit.isEmpty()) "\u2014" else displayHit, color = primaryTextColor, fontSize = if (displayHit.length > 4) 10.sp else 16.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 11.sp)
            }
            Column(modifier = Modifier.width(85.dp).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = action.damageFormula, color = primaryTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, softWrap = true, textAlign = TextAlign.Center)
                Text(text = action.damageType, fontSize = 9.sp, color = secondaryTextColor, textAlign = TextAlign.Center, maxLines = 1)
            }
            Box(modifier = Modifier.width(46.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                when {
                    action.isToggle -> Switch(checked = action.isActive, onCheckedChange = { onClick() }, enabled = !isBlocked, modifier = Modifier.graphicsLayer(scaleX = 0.7f, scaleY = 0.7f))
                    action.type == ActionType.ITEM && action.damageFormula == "\u2014" -> Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Activate", tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                    !hasNested -> Text(text = if (action.isSpell && (action.level ?: 0) > 0) "Cast \u25BE" else "\uD83C\uDFB2", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.background(expandedBg)) {
                action.nestedActions.forEach { sub ->
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = sub.name, modifier = Modifier.weight(1f), fontSize = 13.sp, color = primaryTextColor)
                        if (!sub.damageFormula.isNullOrBlank()) Text(text = sub.damageFormula, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = primaryTextColor)
                    }
                }
            }
        }
    }
}








