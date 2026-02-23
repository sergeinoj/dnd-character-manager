package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.CharacterExportPayload
import com.dnd.app.domain.model.LssBackupContainer
import com.dnd.app.domain.model.LssSpellSummary
import com.dnd.app.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterExporter @Inject constructor(
    private val repository: CharacterRepository,
    private val json: Json
) {
    data class ExportBundle(
        val lssJson: String,
        val dndJson: String
    )

    suspend fun export(characterId: Long): Result<ExportBundle> {
        val draft = repository.getDraftById(characterId)
            ?: return Result.failure(IllegalArgumentException("Draft $characterId is missing"))

        val sheet = repository.getCharacterForSheet(characterId).firstOrNull()
            ?: return Result.failure(IllegalStateException("Live state $characterId not found"))

        val payload = CharacterExportPayload(draft = draft, liveState = sheet.liveState)
        val payloadJson = json.encodeToString(CharacterExportPayload.serializer(), payload)
        val spells = LssSpellSummary(
            mode = "cards",
            prepared = draft.getAllSelectedSpells().toList(),
            book = emptyList(),
            edition = "2014"
        )
        val textResourceLocations = listOf(
            "features",
            "attacks",
            "spells-level-0",
            "traits",
            "equipment",
            "quests",
            "background",
            "allies",
            "personality",
            "ideals",
            "bonds",
            "flaws",
            "prof",
            "feats"
        )
        val inner = buildJsonObject {
            put("isDefault", false)
            put("jsonType", "character")
            put("template", "ph")
            putJsonObject("name") {
                put("value", draft.name)
            }
            put("createdAt", Instant.now().toString())
            put("hiddenName", "")
            put("avatar", "")
            putJsonObject("info") {
                putJsonObject("charClass") {
                    put("name", "charClass")
                    put("label", "class and level")
                    put("value", draft.baseInfo.startingClassIndex)
                }
                putJsonObject("charSubclass") {
                    put("name", "charSubclass")
                    put("value", draft.levelStack.firstOrNull()?.subclassIndex ?: "")
                }
                putJsonObject("level") {
                    put("name", "level")
                    put("label", "level")
                    put("value", draft.levelStack.size.coerceAtLeast(1))
                }
                putJsonObject("background") {
                    put("name", "background")
                    put("label", "background")
                    put("value", draft.baseInfo.backgroundIndex)
                }
                putJsonObject("playerName") {
                    put("name", "playerName")
                    put("label", "player name")
                    put("value", "")
                }
                putJsonObject("race") {
                    put("name", "race")
                    put("label", "race")
                    put("value", draft.baseInfo.raceIndex)
                }
                putJsonObject("alignment") {
                    put("name", "alignment")
                    put("label", "alignment")
                    put("value", draft.baseInfo.alignmentIndex)
                }
                putJsonObject("experience") {
                    put("name", "experience")
                    put("label", "experience")
                    put("value", 0)
                }
            }
            putJsonObject("subInfo") {
                putJsonObject("age") { put("name", "age"); put("label", "age"); put("value", "") }
                putJsonObject("height") { put("name", "height"); put("label", "height"); put("value", "") }
                putJsonObject("weight") { put("name", "weight"); put("label", "weight"); put("value", "") }
                putJsonObject("eyes") { put("name", "eyes"); put("label", "eyes"); put("value", "") }
                putJsonObject("skin") { put("name", "skin"); put("label", "skin"); put("value", "") }
                putJsonObject("hair") { put("name", "hair"); put("label", "hair"); put("value", "") }
            }
            put("proficiency", 0)
            put("proficiencyCustom", 0)
            putJsonObject("stats") {
                draft.resolveEffectiveStats().forEach { (k, v) ->
                    putJsonObject(k.lowercase()) {
                        put("name", k.lowercase())
                        put("label", k)
                        put("score", v)
                        put("modifier", JsonPrimitive(null as String?))
                        put("check", JsonPrimitive(null as String?))
                    }
                }
            }
            putJsonObject("saves") {}
            putJsonObject("skills") {}
            putJsonObject("vitality") {
                putJsonObject("hp-current") { put("value", sheet.liveState.hpCurrent) }
                putJsonObject("hp-max") { put("value", sheet.snapshot.maxHp) }
                putJsonObject("hp-temp") { put("value", sheet.liveState.hpTemp) }
                putJsonObject("ac") { put("value", sheet.snapshot.finalArmorClass) }
                putJsonObject("speed") { put("value", sheet.snapshot.finalSpeed) }
                putJsonObject("initiative") { put("value", JsonPrimitive(null as String?)) }
                putJsonObject("hp-dice-current") { put("value", sheet.snapshot.hitDiceCount) }
                putJsonObject("hit-die") { put("value", sheet.snapshot.hitDice) }
                put("isDying", sheet.liveState.hpCurrent <= 0)
                put("deathFails", sheet.liveState.deathSaves.failures)
                put("deathSuccesses", sheet.liveState.deathSaves.successes)
            }
            putJsonObject("spells") {}
            putJsonObject("spellsInfo") {}
            putJsonObject("spellsPact") {}
            putJsonObject("text") {
                put("_dnd_payload", payloadJson)
                textResourceLocations.forEach { location ->
                    putJsonObject(location) {
                        putJsonObject("value") {
                            putJsonObject("data") {
                                put("type", "doc")
                                putJsonArray("content") {
                                    add(
                                        buildJsonObject {
                                            put("type", "resource")
                                            putJsonObject("attrs") {
                                                put("id", "resource-$location")
                                                put("textName", location)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            putJsonObject("resources") {
                textResourceLocations.forEach { location ->
                    putJsonObject("resource-$location") {
                        put("id", "resource-$location")
                        put("name", "")
                        put("current", 0)
                        put("max", 1)
                        put("location", location)
                    }
                }
            }
            putJsonObject("bonusesSkills") {}
            putJsonObject("bonusesStats") {}
            putJsonArray("conditions") {
                sheet.liveState.activeConditions.forEach { add(JsonPrimitive(it)) }
            }
            putJsonObject("coins") {
                put("gp", sheet.liveState.coins.gp)
                put("sp", sheet.liveState.coins.sp)
                put("cp", sheet.liveState.coins.cp)
            }
            putJsonObject("weapons") {}
            putJsonArray("weaponsList") {}
            putJsonArray("attunementsList") {}
        }
        val disabledBlocks = buildJsonObject {
            putJsonArray("info-left") {}
            putJsonArray("info-right") {}
            putJsonArray("subinfo-left") {}
            putJsonArray("subinfo-right") {}
            putJsonArray("notes-left") {}
            putJsonArray("notes-right") {}
        }
        val container = LssBackupContainer(
            disabledBlocks = disabledBlocks,
            edition = "2024",
            data = json.encodeToString(JsonObject.serializer(), inner),
            spells = spells
        )
        return runCatching {
            ExportBundle(
                lssJson = json.encodeToString(LssBackupContainer.serializer(), container),
                dndJson = payloadJson
            )
        }
    }
}
