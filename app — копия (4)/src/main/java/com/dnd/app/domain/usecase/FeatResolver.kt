// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/FeatResolver.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import android.util.Log
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.Feature
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatResolver @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_LOG_FEAT_RESOLVER"

    fun resolve(feat: Feature, userSubChoice: ChoiceResult?): List<ChoiceResult> {
        val effects = mutableListOf<ChoiceResult>()
        val rawJson = feat.referenceJson ?: return emptyList()

        Log.d(TAG, "Resolving feat '${feat.index}'. Raw JSON: $rawJson, User SubChoice: $userSubChoice")
        try {
            val refObj = json.parseToJsonElement(rawJson).jsonObject

            // --- ШАГ 1: Извлечение СТАТИЧЕСКИХ эффектов (применяются всегда) ---
            val uiGenKeys = setOf("stat_choice", "stat_value", "languages_count", "maneuvers_count", "proficiency_count")

            for ((key, value) in refObj) {
                if (key in uiGenKeys) continue // Эти ключи используются для генерации UI, а не как прямые эффекты

                when (value) {
                    is JsonObject -> { // Пример: "stat_bonus": {"cha": 1}
                        val bonuses = value.entries.associate { (stat, bonus) ->
                            stat.uppercase() to (bonus.jsonPrimitive.content.toIntOrNull() ?: 0)
                        }.filter { it.value != 0 }
                        if (bonuses.isNotEmpty()) {
                            effects.add(ChoiceResult.StatBonus(bonuses))
                        }
                    }
                    is JsonPrimitive -> {
                        // Добавляем как RuleEffect, если это не просто часть другой логики
                        if (key != "feat_group" && key != "dice_type" && key != "proficiency") {
                            effects.add(ChoiceResult.RuleEffect(key, value.content))
                        }
                    }
                    is JsonArray -> {
                        if (key == "proficiency") {
                            // "proficiency": ["light_armor"]
                            val profs = value.mapNotNull { (it as? JsonPrimitive)?.content }
                            if (profs.isNotEmpty()) effects.add(ChoiceResult.SelectedOptions(profs))
                        }
                    }
                }
            }

            // --- ШАГ 2: Обработка ДИНАМИЧЕСКИХ эффектов на основе выбора пользователя ---
            userSubChoice?.let {
                when (it) {
                    is ChoiceResult.SelectedOptions, is ChoiceResult.Skills, is ChoiceResult.Spells -> {
                        effects.add(it)
                    }
                    is ChoiceResult.StatBonus -> {
                        effects.add(it)
                        // Для черты "Стойкий" (Resilient), где выбор стата дает владение спасброском
                        if ((refObj["proficiency"] as? JsonPrimitive)?.content == "saving_throw") {
                            val chosenStat = it.bonuses.keys.firstOrNull() ?: return@let
                            val statKey = chosenStat.take(3).lowercase()
                            effects.add(ChoiceResult.SelectedOptions(listOf("saving-throw-${statKey}")))
                        }
                    }
                    else -> Log.d(TAG, "Unhandled userSubChoice type: $it")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse reference_json for feat '${feat.index}'", e)
        }

        val finalEffects = effects.distinct()
        Log.d(TAG, "Resolved effects for '${feat.index}': $finalEffects")
        return finalEffects
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/FeatResolver.kt