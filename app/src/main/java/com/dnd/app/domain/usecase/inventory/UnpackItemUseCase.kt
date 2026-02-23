// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\UnpackItemUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.inventory

import android.util.Log
import com.dnd.app.domain.model.snapshot.ResetRule
import com.dnd.app.domain.repository.ItemRepository
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UnpackItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val json: Json
) {
    private val TAG = "DND_LOG_UNPACK_UC"
    private val maxDepth = 10
    private val pseudoBundleRegex = Regex("""(\(x\d+\))?\s*(и|and)\s*""", RegexOption.IGNORE_CASE)

    @Throws(ItemUnpackException::class)
    suspend operator fun invoke(sourceMap: Map<String, String>): List<UnpackedItem> {
        if (sourceMap.isEmpty()) return emptyList()

        val allRequiredIndexes = mutableSetOf<String>()
        allRequiredIndexes.addAll(sourceMap.values)


        val initialData = itemRepository.getRawItems(sourceMap.values.toList())
        initialData.forEach { data -> data.baseItemIndex?.let { allRequiredIndexes.add(it) } }

        val globalCache = itemRepository.getRawItems(allRequiredIndexes.toList()).associateBy { it.indexName }
        val finalMap = mutableMapOf<String, UnpackedItem>()

        val sortedSource = sourceMap.entries.sortedBy { it.key }

        for ((sourceKey, itemId) in sortedSource) {
            unpackRecursive(
                itemId = itemId,
                sourceKey = sourceKey,
                containerId = null,
                depth = 0,
                path = emptySet(),
                cache = globalCache,
                resultSink = finalMap,
                inheritedQty = 1
            )
        }

        return finalMap.values.toList()
    }

    private suspend fun unpackRecursive(
        itemId: String,
        sourceKey: String,
        containerId: String?,
        depth: Int,
        path: Set<String>,
        cache: Map<String, RawItemData>,
        resultSink: MutableMap<String, UnpackedItem>,
        inheritedQty: Int
    ) {
        if (depth > maxDepth) {
            Log.e(TAG, "Max depth exceeded at $itemId")
            return
        }
        if (itemId in path) {
            Log.e(TAG, "Cycle detected at $itemId")
            return
        }


        val raw = cache[itemId] ?: itemRepository.getRawItem(itemId)

        if (raw == null) {
            Log.e(TAG, "CRITICAL DATA GAP: Item index '$itemId' not found in database. Item will be skipped.")
            return
        }

        val baseRaw = raw.baseItemIndex?.let { cache[it] ?: itemRepository.getRawItem(it) }
        val contentsJson = raw.contentsJson


        val isPseudoBundle = (raw.name.contains(pseudoBundleRegex) || itemId.startsWith("bundle-"))
                && !contentsJson.isNullOrBlank()

        var unpackSuccess = false

        if (isPseudoBundle && contentsJson != null) {

            val sizeBefore = resultSink.size
            unpackContents(contentsJson, sourceKey, containerId, depth, path + itemId, cache, resultSink, inheritedQty)
            val sizeAfter = resultSink.size


            if (sizeAfter > sizeBefore) {
                unpackSuccess = true
            } else {
                Log.w(TAG, "Bundle '$itemId' failed to unpack (empty or malformed contents). Fallback to item.")
            }
        }




        if (!isPseudoBundle || !unpackSuccess) {
            val uniqueId = if (containerId != null) "$containerId.$itemId" else "$sourceKey.$itemId"

            val existing = resultSink[uniqueId]
            if (existing != null) {
                resultSink[uniqueId] = existing.copy(quantity = existing.quantity + inheritedQty)
            } else {
                val newItem = createUnpackedItem(raw, baseRaw, itemId, sourceKey, uniqueId, containerId, inheritedQty)
                resultSink[uniqueId] = newItem



                if (!contentsJson.isNullOrBlank() && !isPseudoBundle) {
                    unpackContents(contentsJson, sourceKey, uniqueId, depth, path + itemId, cache, resultSink, 1)
                }
            }
        }
    }

    private suspend fun unpackContents(
        jsonString: String, sourceKey: String, parentId: String?, depth: Int, path: Set<String>,
        cache: Map<String, RawItemData>, resultSink: MutableMap<String, UnpackedItem>, multiplier: Int
    ) {
        runCatching {
            val element = json.parseToJsonElement(jsonString)
            if (element is JsonArray) {
                element.forEach { el ->
                    if (el is JsonObject) {

                        val id = extractIndex(el)

                        val qty = (el["quantity"]?.jsonPrimitive?.int ?: 1) * multiplier

                        if (id != null) {
                            unpackRecursive(id, sourceKey, parentId, depth + 1, path, cache, resultSink, qty)
                        } else {
                            Log.w(TAG, "Could not extract index from bundle element: $el")
                        }
                    }
                }
            }
        }.onFailure { Log.e(TAG, "Failed to parse contents JSON for a container. Error: ${it.message}") }
    }


    private fun extractIndex(el: JsonObject): String? {

        el["item"]?.jsonObject?.get("index")?.jsonPrimitive?.content?.let { return it }


        el["item_index"]?.jsonPrimitive?.content?.let { return it }


        el["item"]?.jsonPrimitive?.content?.let { return it }


        el["item"]?.jsonObject?.get("url")?.jsonPrimitive?.content?.let { url ->
            return url.trimEnd('/').substringAfterLast('/')
        }


        el["url"]?.jsonPrimitive?.content?.let { url ->
            return url.trimEnd('/').substringAfterLast('/')
        }

        return null
    }

    private fun createUnpackedItem(
        magic: RawItemData,
        base: RawItemData?,
        itemId: String,
        sourceKey: String,
        uniqueId: String,
        containerId: String?,
        quantity: Int
    ): UnpackedItem {
        val properties = magic.propertiesJson?.let { parseProperties(it) }
            ?: base?.propertiesJson?.let { parseProperties(it) }
            ?: emptyList()

        val resetRule = when (magic.chargeResetRule?.uppercase()) {
            "DAWN" -> ResetRule.DAWN
            "SHORT_REST" -> ResetRule.SHORT_REST
            "NEVER" -> ResetRule.NEVER
            else -> ResetRule.LONG_REST
        }

        val spells = magic.grantedSpellsJson?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()

        val finalWeight = magic.weight ?: base?.weight ?: 0.0
        val finalCost = magic.costCp.takeIf { it > 0 } ?: base?.costCp ?: 0

        return UnpackedItem(
            itemId = itemId,
            sourceKey = sourceKey,
            uniqueId = uniqueId,
            name = magic.name,
            weight = finalWeight,
            description = magic.description ?: base?.description ?: "",
            costCp = finalCost,
            quantity = quantity,
            isPack = !magic.contentsJson.isNullOrBlank(),
            containerId = containerId,
            totalContentsWeight = null,
            damage = magic.damageDice ?: base?.damageDice,
            damageType = magic.damageType ?: base?.damageType,
            properties = properties,
            baseAc = magic.baseAc ?: base?.baseAc,
            dexCap = magic.dexCap ?: base?.dexCap,
            isShield = magic.categoryIndex?.contains("shield") == true || base?.categoryIndex?.contains("shield") == true,
            stealthDisadvantage = magic.stealthDisadvantage || (base?.stealthDisadvantage ?: false),
            strMinimum = magic.strMinimum ?: base?.strMinimum,
            isAttunementRequired = magic.requiresAttunement,
            maxCharges = magic.maxCharges,
            resetRule = resetRule,
            bonusAc = magic.bonusAc,
            bonusAttack = magic.bonusAttack,
            bonusDamage = magic.bonusDamage,
            bonusSaveDc = magic.bonusSaveDc,
            grantedSpells = spells,
            referenceJson = magic.referenceJson,
            categoryIndex = magic.categoryIndex ?: base?.categoryIndex,
            rarity = magic.rarity ?: base?.rarity,
            statOverridesJson = magic.statOverridesJson ?: base?.statOverridesJson,
            mechanicsJson = magic.mechanicsJson ?: base?.mechanicsJson,
            variant = magic.variant ?: base?.variant
        )
    }

    private fun parseProperties(jsonString: String): List<String> {
        return runCatching {
            val element = json.parseToJsonElement(jsonString)
            if (element is JsonArray) {
                element.mapNotNull { el ->
                    if (el is JsonObject) {
                        val name = el["name"]?.jsonPrimitive?.content ?: "Unknown"
                        val index = el["index"]?.jsonPrimitive?.content ?: ""
                        if (index.isNotBlank()) "$name [$index]" else name
                    } else if (el is JsonPrimitive) {
                        el.content
                    } else null
                }
            } else emptyList()
        }.getOrDefault(emptyList())
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\inventory\UnpackItemUseCase.kt