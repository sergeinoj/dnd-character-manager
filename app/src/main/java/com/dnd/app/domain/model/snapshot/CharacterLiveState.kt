// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\CharacterLiveState.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model.snapshot

import com.dnd.app.domain.model.Money
import kotlinx.serialization.Serializable


@Serializable
data class CharacterLiveState(

    val hpCurrent: Int = 10,
    val hpTemp: Int = 0,
    val transformationId: String? = null,
    val transformationHp: Int = 0,
    val hitDiceSpent: Int = 0,
    val deathSaves: DeathSavesState = DeathSavesState(),



    val featureCharges: Map<String, Int> = emptyMap(),
    val equippedItemIds: Set<String> = emptySet(),
    val attunedItemIds: Set<String> = emptySet(),
    val coins: Money = Money(),
    val itemOverrides: Map<String, ItemOverride> = emptyMap(),
    val purchasedItems: List<PurchasedItemRecord> = emptyList(),



    val preparedSpellIds: Map<String, Set<String>> = emptyMap(),
    val concentrationSpellId: String? = null,

    val spentGlobalSlots: Map<Int, Int> = emptyMap(),

    val spentPactSlots: Int = 0,
    val innateUsage: Map<String, Int> = emptyMap(),


    val activeEffects: Set<String> = emptySet(),
    val activeConditions: Set<String> = emptySet(),
    val exhaustionLevel: Int = 0,

    val notes: String = "",

    val systemLogs: List<String> = emptyList()
)

@Serializable
data class ItemOverride(val quantity: Int)

@Serializable
data class PurchasedItemRecord(
    val id: String,
    val refIndex: String,
    val timestamp: Long,
    val capturedPriceInCp: Int,
    val traceKey: String
)

@Serializable
data class DeathSavesState(
    val successes: Int = 0,
    val failures: Int = 0
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\CharacterLiveState.kt
