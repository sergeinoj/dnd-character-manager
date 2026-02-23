// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\converters\DraftConverters.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.converters

import android.util.Log
import androidx.room.TypeConverter
import com.dnd.app.domain.model.DraftCharacter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put


class DraftConverters {
    private val tag = "DRAFT_CONVERTER"
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromDraft(draft: DraftCharacter?): String? {
        return draft?.let {
            val encoded = json.encodeToString(it)
            Log.d(tag, "fromDraft id=${it.id} name='${it.name}' levels=${it.levelStack.size} jsonLen=${encoded.length}")
            encoded
        }
    }

    @TypeConverter
    fun toDraft(data: String?): DraftCharacter? {
        return data?.let {
            try {
                json.decodeFromString<DraftCharacter>(normalizeLegacyDraftJson(it))
            } catch (e: Exception) {
                val head = it.take(500)
                Log.e(tag, "toDraft decode failed len=${it.length} head=$head", e)
                null
            }
        }
    }

    private fun normalizeLegacyDraftJson(raw: String): String {
        val root = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull() ?: return raw
        var fixedCount = 0

        fun fixSelectionMap(element: JsonElement?): JsonElement? {
            val obj = element as? JsonObject ?: return element
            return JsonObject(obj.mapValues { (_, value) ->
                val choice = value as? JsonObject ?: return@mapValues value
                val type = choice["type"]?.let { it as? JsonPrimitive }?.contentOrNull
                if (type != "kotlin.collections.LinkedHashMap") return@mapValues value

                fixedCount++
                val stats = choice["stats"]
                val statsArray = when (stats) {
                    is JsonArray -> stats
                    is JsonPrimitive -> buildJsonArray { add(JsonPrimitive(stats.content)) }
                    else -> buildJsonArray { }
                }
                buildJsonObject {
                    put("type", JsonPrimitive("StatBonus"))
                    put("stats", statsArray)
                }
            })
        }

        val baseInfo = (root["baseInfo"] as? JsonObject)?.let { base ->
            JsonObject(
                base.toMutableMap().apply {
                    this["raceSelections"] = fixSelectionMap(this["raceSelections"]) ?: JsonObject(emptyMap())
                    this["backgroundSelections"] = fixSelectionMap(this["backgroundSelections"]) ?: JsonObject(emptyMap())
                    this["inventorySelections"] = fixSelectionMap(this["inventorySelections"]) ?: JsonObject(emptyMap())
                }
            )
        } ?: root["baseInfo"]

        val levelStack = (root["levelStack"] as? JsonArray)?.let { stack ->
            JsonArray(stack.map { levelEl ->
                val levelObj = levelEl as? JsonObject ?: return@map levelEl
                JsonObject(levelObj.toMutableMap().apply {
                    this["selections"] = fixSelectionMap(this["selections"]) ?: JsonObject(emptyMap())
                })
            })
        } ?: root["levelStack"]

        if (fixedCount == 0) return raw
        val normalized = JsonObject(root.toMutableMap().apply {
            this["baseInfo"] = baseInfo ?: JsonObject(emptyMap())
            this["levelStack"] = levelStack ?: JsonArray(emptyList())
        })
        Log.w(tag, "Legacy draft migrated on read: fixedChoices=$fixedCount")
        return normalized.toString()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\local\converters\DraftConverters.kt
