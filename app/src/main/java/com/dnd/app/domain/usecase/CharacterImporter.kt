package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.CharacterExportPayload
import com.dnd.app.domain.model.BaseInfo
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.LevelStep
import com.dnd.app.domain.model.LssBackupContainer
import com.dnd.app.domain.model.snapshot.CharacterLiveState
import com.dnd.app.domain.repository.CharacterRepository
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterImporter @Inject constructor(
    private val repository: CharacterRepository,
    private val json: Json
) {

    suspend fun import(payloadJson: String): Result<Long> {
        runCatching { json.decodeFromString<CharacterExportPayload>(payloadJson) }
            .onSuccess { payload ->
                return repository.commitFullCharacter(payload.draft).fold(
                    onSuccess = { id -> repository.syncLiveState(id) { payload.liveState }.map { id } },
                    onFailure = { Result.failure(it) }
                )
            }

        val container = runCatching { json.decodeFromString<LssBackupContainer>(payloadJson) }
            .getOrElse { return Result.failure(it) }
        val payload = decodePayload(container)
            .getOrElse { return Result.failure(it) }

        return repository.commitFullCharacter(payload.draft).fold(
            onSuccess = { id ->
                repository.syncLiveState(id) { payload.liveState }
                    .map { id }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun decodePayload(container: LssBackupContainer): Result<CharacterExportPayload> {
        val innerElement = runCatching { json.parseToJsonElement(container.data) }.getOrNull()
        val innerObject = innerElement as? JsonObject
        val embedded = innerObject
            ?.get("_dnd_payload")
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
            ?: innerObject
                ?.get("text")
                ?.jsonObject
                ?.get("_dnd_payload")
                ?.let { (it as? JsonPrimitive)?.contentOrNull }
        if (!embedded.isNullOrBlank()) {
            return runCatching { json.decodeFromString<CharacterExportPayload>(embedded) }
        }
        return runCatching {
            val draft = buildDraftFromLss(innerObject ?: JsonObject(emptyMap()))
            val live = buildLiveFromLss(innerObject, container)
            CharacterExportPayload(draft = draft, liveState = live)
        }
    }

    private fun buildDraftFromLss(inner: JsonObject): DraftCharacter {
        val info = inner["info"]?.jsonObject
        val stats = inner["stats"]?.jsonObject
        val classRaw = readString(info?.get("class")) ?: readString(info?.get("charClass"))
        val raceRaw = readString(info?.get("race"))
        val alignmentRaw = readString(info?.get("alignment"))
        val backgroundRaw = readString(info?.get("background"))
        val classIndex = mapClassIndex(classRaw)
        val raceIndex = mapRaceIndex(raceRaw)
        val alignment = mapAlignmentIndex(alignmentRaw)
        val background = mapBackgroundIndex(backgroundRaw)
        val level = (readInt(info?.get("level")) ?: 1).coerceAtLeast(1)
        val scores = mapOf(
            "STR" to (readInt(stats?.get("str"), nestedKey = "score") ?: 10),
            "DEX" to (readInt(stats?.get("dex"), nestedKey = "score") ?: 10),
            "CON" to (readInt(stats?.get("con"), nestedKey = "score") ?: 10),
            "INT" to (readInt(stats?.get("int"), nestedKey = "score") ?: 10),
            "WIS" to (readInt(stats?.get("wis"), nestedKey = "score") ?: 10),
            "CHA" to (readInt(stats?.get("cha"), nestedKey = "score") ?: 10)
        )
        return DraftCharacter(
            name = repairMojibake(readString(inner["name"])).ifBlank { "Imported LSS" },
            baseInfo = BaseInfo(
                raceIndex = raceIndex,
                backgroundIndex = background,
                alignmentIndex = alignment,
                baseAbilityScores = scores,
                startingClassIndex = classIndex
            ),
            levelStack = List(level) { LevelStep(classIndex = classIndex) }
        )
    }

    private fun buildLiveFromLss(inner: JsonObject?, container: LssBackupContainer): CharacterLiveState {
        val vitality = inner?.get("vitality")?.jsonObject
        val prepared = container.spells.prepared.toSet()
        return CharacterLiveState(
            hpCurrent = readInt(vitality?.get("hp-current")) ?: 10,
            hpTemp = readInt(vitality?.get("hp-temp")) ?: 0,
            preparedSpellIds = if (prepared.isEmpty()) emptyMap() else mapOf("class-wizard" to prepared)
        )
    }

    private fun readString(element: JsonElement?, nestedKey: String = "value"): String? {
        return when (element) {
            is JsonPrimitive -> element.contentOrNull
            is JsonObject -> (element[nestedKey] as? JsonPrimitive)?.contentOrNull
            else -> null
        }
    }

    private fun readInt(element: JsonElement?, nestedKey: String = "value"): Int? {
        return when (element) {
            is JsonPrimitive -> element.intOrNull
            is JsonObject -> {
                val nested = element[nestedKey] as? JsonPrimitive
                nested?.intOrNull ?: nested?.contentOrNull?.toIntOrNull()
            }
            else -> null
        }
    }

    private fun slug(value: String?): String {
        return repairMojibake(value).orEmpty()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    private fun mapClassIndex(raw: String?): String {
        val fixed = repairMojibake(raw).trim().lowercase()
        if (fixed.isBlank()) return "fighter"
        val first = fixed.split("/", ",", ";").firstOrNull()?.trim().orEmpty()
        val mapped = CLASS_MAP[first] ?: CLASS_MAP[fixed]
        return mapped ?: slug(first).ifBlank { "fighter" }
    }

    private fun mapRaceIndex(raw: String?): String {
        val fixed = repairMojibake(raw).trim().lowercase()
        if (fixed.isBlank()) return "human"
        return RACE_MAP[fixed] ?: slug(fixed).ifBlank { "human" }
    }

    private fun mapBackgroundIndex(raw: String?): String {
        val fixed = repairMojibake(raw).trim().lowercase()
        if (fixed.isBlank()) return "acolyte"
        return BACKGROUND_MAP[fixed] ?: slug(fixed).ifBlank { "acolyte" }
    }

    private fun mapAlignmentIndex(raw: String?): String {
        val fixed = repairMojibake(raw).trim().lowercase()
        if (fixed.isBlank()) return "true-neutral"
        return ALIGNMENT_MAP[fixed] ?: slug(fixed).ifBlank { "true-neutral" }
    }

    private fun repairMojibake(value: String?): String {
        val src = value ?: return ""
        val repaired = String(src.toByteArray(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
        val srcCyr = src.count { it in '\u0400'..'\u04FF' }
        val repairedCyr = repaired.count { it in '\u0400'..'\u04FF' }
        val srcMarkers = src.count { it == 'Р' || it == 'С' || it == 'Ð' || it == 'Ñ' }
        val repairedMarkers = repaired.count { it == 'Р' || it == 'С' || it == 'Ð' || it == 'Ñ' }
        return if (repairedCyr > srcCyr && repairedMarkers <= srcMarkers) repaired else src
    }

    companion object {
        private val CLASS_MAP = mapOf(
            "варвар" to "barbarian",
            "бард" to "bard",
            "жрец" to "cleric",
            "друид" to "druid",
            "воин" to "fighter",
            "монах" to "monk",
            "паладин" to "paladin",
            "следопыт" to "ranger",
            "плут" to "rogue",
            "волшебник" to "wizard",
            "колдун" to "warlock",
            "чародей" to "sorcerer",
            "barbarian" to "barbarian",
            "bard" to "bard",
            "cleric" to "cleric",
            "druid" to "druid",
            "fighter" to "fighter",
            "monk" to "monk",
            "paladin" to "paladin",
            "ranger" to "ranger",
            "rogue" to "rogue",
            "wizard" to "wizard",
            "warlock" to "warlock",
            "sorcerer" to "sorcerer"
        )

        private val RACE_MAP = mapOf(
            "человек" to "human",
            "полуэльф" to "half-elf",
            "эльф" to "elf",
            "дварф" to "dwarf",
            "полурослик" to "halfling",
            "гном" to "gnome",
            "полуорк" to "half-orc",
            "тифлинг" to "tiefling",
            "human" to "human",
            "half-elf" to "half-elf",
            "elf" to "elf",
            "dwarf" to "dwarf",
            "halfling" to "halfling",
            "gnome" to "gnome",
            "half-orc" to "half-orc",
            "tiefling" to "tiefling"
        )

        private val BACKGROUND_MAP = mapOf(
            "мудрец" to "sage",
            "отшельник" to "hermit",
            "солдат" to "soldier",
            "преступник" to "criminal",
            "артист" to "entertainer",
            "народный герой" to "folk-hero",
            "благородный" to "noble",
            "послушник" to "acolyte",
            "шарлатан" to "charlatan",
            "sage" to "sage",
            "hermit" to "hermit",
            "soldier" to "soldier",
            "criminal" to "criminal",
            "entertainer" to "entertainer",
            "folk-hero" to "folk-hero",
            "noble" to "noble",
            "acolyte" to "acolyte",
            "charlatan" to "charlatan"
        )

        private val ALIGNMENT_MAP = mapOf(
            "законопослушно-добрый" to "lawful-good",
            "нейтрально-добрый" to "neutral-good",
            "хаотично-добрый" to "chaotic-good",
            "законопослушно-нейтральный" to "lawful-neutral",
            "истинно нейтральный" to "true-neutral",
            "нейтральный" to "true-neutral",
            "хаотично-нейтральный" to "chaotic-neutral",
            "законопослушно-злой" to "lawful-evil",
            "нейтрально-злой" to "neutral-evil",
            "хаотично-злой" to "chaotic-evil",
            "lawful-good" to "lawful-good",
            "neutral-good" to "neutral-good",
            "chaotic-good" to "chaotic-good",
            "lawful-neutral" to "lawful-neutral",
            "true-neutral" to "true-neutral",
            "chaotic-neutral" to "chaotic-neutral",
            "lawful-evil" to "lawful-evil",
            "neutral-evil" to "neutral-evil",
            "chaotic-evil" to "chaotic-evil"
        )
    }
}
