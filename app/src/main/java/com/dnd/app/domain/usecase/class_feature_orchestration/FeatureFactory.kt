// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\FeatureFactory.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import android.util.Log
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.repository.datasource.SpellDataSource
import com.dnd.app.domain.model.*
import com.dnd.app.domain.model.snapshot.ResetRule
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.stripHtml
import kotlinx.serialization.json.*
import java.util.Locale
import kotlin.math.absoluteValue
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FeatureFactory @Inject constructor(
    private val classFeatureRepository: ClassFeatureRepository,
    private val spellDataSource: SpellDataSource,
    private val referenceDao: ReferenceDao
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TAG = "DND_DEBUG_FEAT_FACTORY"
    private val VALID_STATS = setOf("STR", "DEX", "CON", "INT", "WIS", "CHA")

    private val CONTAINER_TOKEN_REGEX = Regex("(?i)@CONTAINER@")

    suspend fun create(
        entity: com.dnd.app.data.local.entity.FeatureEntity,
        proficiencyProvider: (() -> Map<String, Int>)? = null
    ): Feature {
        val choices = mutableListOf<FeatureChoiceDomain>()
        val grantedProficiencies = mutableListOf<String>()
        val localizedNames = mutableMapOf<String, String>()
        val normalizedIndexName = entity.indexName.lowercase(Locale.ROOT)

        entity.referenceJson?.let { raw ->
            try {
                val refObj = json.parseToJsonElement(raw).jsonObject
                refObj["granted_proficiencies"]?.jsonArray?.forEach { prof ->
                    val idx = prof.jsonObject["index"]?.jsonPrimitive?.content
                    val name = prof.jsonObject["name"]?.jsonPrimitive?.content
                    if (idx != null) {
                        grantedProficiencies.add(idx)
                        if (name != null) localizedNames[idx] = name
                    }
                }
                parseReferenceStatChoice(refObj)?.let { choices.add(it) }
                parseReferenceStatBonus(refObj)?.let { choices.add(it) }

                if (normalizedIndexName.contains("pact-of-the-chain")) {
                    val overrides = refObj["spell_overrides"]?.jsonObject
                    val findFamiliar = overrides?.get("find-familiar")?.jsonObject
                    val additionalForms = findFamiliar?.get("additional_forms")?.jsonArray

                    if (additionalForms != null) {
                        val options = additionalForms.mapNotNull {
                            val formId = it.jsonPrimitive.content
                            if (formId.isNotBlank()) {
                                ChoiceOption(
                                    id = formId,
                                    label = DndLocalization.translateProficiency(formId),
                                    kind = ProficiencyKind.NONE
                                )
                            } else null
                        }

                        Log.d("RALPH", "Pact override parsed for $normalizedIndexName, forms=${options.size}")

                        if (options.isNotEmpty()) {
                            choices.add(
                                FeatureChoiceDomain.SelectOption(
                                    count = 1,
                                    options = options,
                                    description = "Выберите форму фамильяра",
                                    targetProficiencyLevel = 1
                                )
                            )
                        }
                    }
                }
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "CRITICAL: Malformed reference_json in feature '${entity.indexName}'.", e)
                Unit
            }
        }

        var finalDescription = entity.description ?: ""
        if (entity.indexName.contains("-skills") && grantedProficiencies.isNotEmpty()) {
            val bulletList = grantedProficiencies.joinToString("\n") { index ->
                val name = localizedNames[index] ?: DndLocalization.translateProficiency(index)
                "• $name"
            }
            finalDescription = if (finalDescription.isBlank()) bulletList else "$finalDescription\n\n$bulletList"
        }

        entity.choicesJson?.let { raw ->
            try {
                val el = json.parseToJsonElement(raw)
                val elements = if (el is JsonArray) el else listOf(el)
                elements.forEach {
                    if (it is JsonObject) {
                        val choice = parseChoice(it, entity.indexName.lowercase(), proficiencyProvider = proficiencyProvider)
                        if (choice !is FeatureChoiceDomain.InvalidChoice) {
                            choices.add(choice)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing choices_json for ${entity.indexName}", e)
            }
        }

        val spells = spellDataSource.getGrantedSpells(entity.spellShowJson)

        return Feature(
            id = entity.id ?: 0,
            index = entity.indexName,
            name = entity.name,
            classIndex = entity.classIndex,
            subclassIndex = entity.subclassIndex,
            description = finalDescription,
            choices = choices,
            embeddedSpells = spells,
            changeRule = entity.changeRule == 1,
            priority = 100,
            grantedProficiencies = grantedProficiencies,
            maxCharges = entity.maxCharges,
            resetRule = parseResetRule(entity.chargeResetRule),
            referenceJson = entity.referenceJson,
            uiGroup = entity.uiGroup
        )
    }

    private fun parseResetRule(rule: String?): ResetRule = when (rule?.uppercase()) {
        "SHORT_REST" -> ResetRule.SHORT_REST
        "DAWN" -> ResetRule.DAWN
        "NEVER" -> ResetRule.NEVER
        else -> ResetRule.LONG_REST
    }

    private fun parseReferenceStatBonus(refObj: JsonObject): FeatureChoiceDomain.SelectStatBonus? {
        val bonusObject = refObj["stat_bonus"] as? JsonObject ?: return null
        val options = bonusObject.entries.mapNotNull { (statKey, rawBonus) ->
            val amount = rawBonus.jsonPrimitive.intOrNull ?: return@mapNotNull null
            if (amount == 0) return@mapNotNull null
            val normalizedStat = statKey.take(3).uppercase(Locale.ROOT)
            ChoiceOption(
                id = normalizedStat,
                label = DndLocalization.translateStat(normalizedStat)
            )
        }.distinctBy { it.id }

        if (options.isEmpty()) return null

        val amountValue = bonusObject.entries.mapNotNull { it.value.jsonPrimitive.intOrNull }.firstOrNull() ?: 1
        val countValue = options.size

        return FeatureChoiceDomain.SelectStatBonus(countValue, amountValue, options)
    }

    private fun parseReferenceStatChoice(refObj: JsonObject): FeatureChoiceDomain.SelectStatBonus? {
        val statKeys = refObj["stat_choice"]?.jsonArray?.mapNotNull { element ->
            element.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        } ?: return null

        if (statKeys.isEmpty()) return null

        val options = statKeys.map {
            ChoiceOption(it.uppercase(Locale.ROOT), DndLocalization.translateStat(it))
        }

        val bonusValue = refObj["stat_value"]?.jsonPrimitive?.intOrNull ?: 1
        val count = refObj["stat_count"]?.jsonPrimitive?.intOrNull ?: 1

        return FeatureChoiceDomain.SelectStatBonus(count, bonusValue, options)
    }

    suspend fun parseChoice(
        obj: JsonObject,
        parentPath: String,
        proficiencyProvider: (() -> Map<String, Int>)? = null,
        contextProficiencyLevel: Int = 1,
        currentDepth: Int = 0,
        contextKind: ProficiencyKind = ProficiencyKind.NONE
    ): FeatureChoiceDomain {
        if (currentDepth > MAX_RECURSION_DEPTH) {
            return FeatureChoiceDomain.InvalidChoice("Max recursion depth at '$parentPath'")
        }

        val rawType = obj["type"]?.jsonPrimitive?.content ?: ""
        val rawDesc = obj["desc"]?.jsonPrimitive?.content ?: ""
        val isTransparent = rawDesc.contains("@CONTAINER@", ignoreCase = true) || rawType.contains("@CONTAINER@", ignoreCase = true)
        val cleanDesc = rawDesc.replace(CONTAINER_TOKEN_REGEX, "").trim().ifBlank { null }
        val type = rawType.replace(CONTAINER_TOKEN_REGEX, "").trim().lowercase()
        val effectiveProficiencyLevel = if (type.contains("expertise")) 2 else contextProficiencyLevel

        var kind = getProficiencyKindFromType(type)
        if (kind == ProficiencyKind.NONE && (parentPath.contains("-skills") || contextKind == ProficiencyKind.SKILL)) {
            kind = ProficiencyKind.SKILL
        }

        val count = obj["choose"]?.jsonPrimitive?.int ?: 1
        val fromElement = obj["from"]

        if (type.contains("expertise") && fromElement == null && proficiencyProvider != null) {
            val allProfs = proficiencyProvider.invoke()
            val availableForExpertise = allProfs.filterValues { it >= 1 }.keys
            val kindFilter = when {
                type.contains("skill") -> ProficiencyKind.SKILL
                type.contains("tool") -> ProficiencyKind.TOOL
                else -> ProficiencyKind.NONE
            }
            val options = availableForExpertise.filter {
                if (kindFilter == ProficiencyKind.NONE) return@filter true
                it.startsWith(if (kindFilter == ProficiencyKind.SKILL) "skill-" else "tool-")
            }.map { ChoiceOption(it, DndLocalization.translateProficiency(it), kind = kindFilter) }.sortedBy { it.label }
            return FeatureChoiceDomain.SelectExpertise(count, options, proficiencyKind = kindFilter)
        }

        val isAsiType = type.contains("asi") || (type.contains("ability-score") && type.contains("improvement"))
        if (isAsiType) {
            val statsOptions = VALID_STATS.map { ChoiceOption(it, DndLocalization.translateStat(it)) }
            val allFeats = classFeatureRepository.getAllFeats().map { ChoiceOption(it.indexName, it.name, kind = ProficiencyKind.FEAT) }
            val statChoice = ChoiceOption(
                id = "asi",
                label = "Улучшение характеристик (+2)",
                subChoice = FeatureChoiceDomain.SelectStatBonus(
                    count = 2,
                    amount = 1,
                    options = statsOptions,
                    allowDuplicateSelections = true
                )
            )
            val featChoice = ChoiceOption(
                id = "feat",
                label = "Черта",
                subChoice = FeatureChoiceDomain.SelectOption(1, allFeats, proficiencyKind = ProficiencyKind.FEAT),
                kind = ProficiencyKind.FEAT
            )
            val optionsArray = (fromElement as? JsonObject)?.get("options") as? JsonArray ?: fromElement as? JsonArray
            val explicitTokens = extractAsiTokens(optionsArray)
            val explicitOptions = buildAsiOptionsFromTokens(explicitTokens, statChoice, featChoice)
            if (explicitOptions.isNotEmpty()) {
                val tokenIds = explicitOptions.joinToString(",") { it.id }
                val explicitMessage = "ASI tokens for \\\"" + parentPath + "\\\": " + explicitTokens + " -> " + tokenIds
                Log.d("RALPH", explicitMessage)
            }
            val options = if (explicitOptions.isNotEmpty()) explicitOptions else listOf(statChoice, featChoice)
            return FeatureChoiceDomain.SelectOption(1, options, "Выберите улучшение")
        }

        if (type.contains("hp_bonus") || type.contains("hp-increase")) {
            val hpOptions = fromElement?.jsonArray?.map { el ->
                val o = el.jsonObject
                ChoiceOption(id = o["index"]?.jsonPrimitive?.content ?: "hp", label = o["name"]?.jsonPrimitive?.content ?: "Бонус хитов")
            } ?: emptyList()
            return FeatureChoiceDomain.SelectOption(count, hpOptions, cleanDesc)
        }

        if (type.contains("class_spell_list") || type.contains("class_ritual_list")) {
            return spellDataSource.parseFeatSpellChoice(obj)
        }
        if (type.contains("spell")) return spellDataSource.parseSpellChoice(obj)

        var inferredKind: ProficiencyKind? = null
        val options: List<ChoiceOption> = when (fromElement) {
            is JsonObject -> {
                when (fromElement["option_set_type"]?.jsonPrimitive?.content) {
                    "resource_list" -> {
                        val resource = fromElement["resource"]?.jsonPrimitive?.content ?: type
                        when (resource) {
                            "skills", "proficiencies", "skill" -> {
                                inferredKind = ProficiencyKind.SKILL
                                classFeatureRepository.getAllSkills().map { ChoiceOption(id = normalizeId(it.indexName, ProficiencyKind.SKILL), label = it.name, kind = ProficiencyKind.SKILL) }
                            }
                            "languages", "language" -> {
                                inferredKind = ProficiencyKind.LANGUAGE
                                classFeatureRepository.getAllLanguages().map { ChoiceOption(id = normalizeId(it.indexName, ProficiencyKind.LANGUAGE), label = it.name, kind = ProficiencyKind.LANGUAGE) }
                            }
                            "general_feats", "feats" -> {
                                inferredKind = ProficiencyKind.FEAT
                                classFeatureRepository.getAllFeats().map { ChoiceOption(it.indexName, it.name, kind = ProficiencyKind.FEAT) }
                            }
                            "artisans-tools", "artisan-tools", "musical-instruments", "instruments", "gaming-sets" -> {
                                inferredKind = ProficiencyKind.TOOL
                                val categoryKey = when(resource) {
                                    "artisan-tools" -> "artisans-tools"
                                    "instruments" -> "musical-instruments"
                                    else -> resource
                                }
                                val itemIndexes = classFeatureRepository.getAllItemIndexesByCategoryRecursive(categoryKey)
                                classFeatureRepository.getEquipmentByIndexes(itemIndexes).map {
                                    ChoiceOption(id = normalizeId(it.indexName, ProficiencyKind.TOOL), label = it.name, info = it.description?.stripHtml(), kind = ProficiencyKind.TOOL)
                                }
                            }
                            else -> emptyList()
                        }
                    }
                    "equipment_category" -> {
                        val rawCategoryIndex = fromElement["equipment_category"]?.jsonObject?.get("index")?.jsonPrimitive?.content
                        val categoryIndex = when(rawCategoryIndex) {
                            "artisan-tools" -> "artisans-tools"
                            "instruments" -> "musical-instruments"
                            else -> rawCategoryIndex
                        }

                        if (categoryIndex != null) {
                            inferredKind = ProficiencyKind.NONE
                            val itemIndexes = classFeatureRepository.getAllItemIndexesByCategoryRecursive(categoryIndex)
                            if (itemIndexes.isNotEmpty()) {
                                val weapons = classFeatureRepository.getWeaponsByIndexes(itemIndexes)
                                val armor = classFeatureRepository.getArmorByIndexes(itemIndexes)
                                val equipment = classFeatureRepository.getEquipmentByIndexes(itemIndexes)
                                val damageTypeIndices = weapons.mapNotNull { it.damageType }.distinct()
                                val damageTypeMap = if (damageTypeIndices.isNotEmpty()) {
                                    damageTypeIndices.mapNotNull { classFeatureRepository.getDamageTypeByIndex(it) }.associate { it.indexName to it.name }
                                } else emptyMap()

                                val weaponOptions = weapons.map { ChoiceOption(id = it.indexName, label = it.name, info = DndLocalization.assembleEnrichedDescription(DndLocalization.translateRarity(it.rarity), DndLocalization.formatWeaponInfo(it.damage, damageTypeMap[it.damageType] ?: it.damageType), it.description), kind = ProficiencyKind.NONE) }
                                val armorOptions = armor.map { ChoiceOption(id = it.indexName, label = it.name, info = DndLocalization.assembleEnrichedDescription(DndLocalization.translateRarity(it.rarity), DndLocalization.formatArmorInfo(it.acBase), it.description), kind = ProficiencyKind.NONE) }
                                val equipmentOptions = equipment.map { ChoiceOption(id = it.indexName, label = it.name, info = it.description?.stripHtml(), kind = ProficiencyKind.NONE) }
                                (weaponOptions + armorOptions + equipmentOptions).distinctBy { it.id }.sortedBy { it.label }
                            } else emptyList()
                        } else emptyList()
                    }
                    else -> parseStaticOptions(fromElement["options"] as? JsonArray, parentPath, kind, proficiencyProvider, effectiveProficiencyLevel, currentDepth, contextKind)
                }
            }
            is JsonArray -> parseStaticOptions(fromElement, parentPath, kind, proficiencyProvider, effectiveProficiencyLevel, currentDepth, contextKind)
            is JsonPrimitive -> {
                when (fromElement.content) {
                    "all_skills_and_tools" -> {
                        inferredKind = ProficiencyKind.NONE
                        val skillOptions = classFeatureRepository.getAllSkills().map {
                            ChoiceOption(id = normalizeId(it.indexName, ProficiencyKind.SKILL), label = it.name, kind = ProficiencyKind.SKILL)
                        }
                        skillOptions
                    }
                    "martial_weapons" -> {
                        inferredKind = ProficiencyKind.WEAPON
                        val melee = classFeatureRepository.getAllItemIndexesByCategoryRecursive("martial-melee-weapons")
                        val ranged = classFeatureRepository.getAllItemIndexesByCategoryRecursive("martial-ranged-weapons")
                        resolveEntitiesToOptions(
                            (melee + ranged).distinct(),
                            inferredKind,
                            fallbackWeaponCategories = listOf("martial-melee-weapons", "martial-ranged-weapons")
                        )
                    }
                    "simple_weapons", "simple-weapons" -> {
                        inferredKind = ProficiencyKind.WEAPON
                        val melee = classFeatureRepository.getAllItemIndexesByCategoryRecursive("simple-melee-weapons")
                        val ranged = classFeatureRepository.getAllItemIndexesByCategoryRecursive("simple-ranged-weapons")
                        resolveEntitiesToOptions(
                            (melee + ranged).distinct(),
                            inferredKind,
                            fallbackWeaponCategories = listOf("simple-melee-weapons", "simple-ranged-weapons")
                        )
                    }
                    "all_classes" -> classFeatureRepository.getAllClassesEntities().map { ChoiceOption(it.indexName, it.name) }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }

        val finalKind = inferredKind ?: contextKind.takeIf { it != ProficiencyKind.NONE } ?: kind
        return when {
            effectiveProficiencyLevel == 2 -> FeatureChoiceDomain.SelectExpertise(count, options, proficiencyKind = finalKind)
            type.contains("ability") -> FeatureChoiceDomain.SelectStatBonus(count, 1, options)
            finalKind != ProficiencyKind.NONE -> FeatureChoiceDomain.SelectOption(count, options, cleanDesc, proficiencyKind = finalKind, targetProficiencyLevel = effectiveProficiencyLevel, isTransparent = isTransparent)
            else -> FeatureChoiceDomain.SelectOption(count, options, cleanDesc, isTransparent = isTransparent)
        }
    }

    private suspend fun parseStaticOptions(
        optionsSource: JsonArray?,
        parentPath: String,
        kind: ProficiencyKind,
        proficiencyProvider: (() -> Map<String, Int>)?,
        effectiveProficiencyLevel: Int,
        currentDepth: Int,
        contextKind: ProficiencyKind
    ): List<ChoiceOption> {
        return optionsSource?.mapIndexedNotNull { index, el ->
            try {
                when (el) {
                    is JsonObject -> {
                        val item = el["item"]?.jsonObject
                        val itemName = item?.get("name")?.jsonPrimitive?.contentOrNull?.trim()
                        val nestedChoice = el["choice"]?.jsonObject
                        val rawId = item?.get("index")?.jsonPrimitive?.contentOrNull
                            ?: el["value"]?.jsonPrimitive?.contentOrNull
                            ?: el["index"]?.jsonPrimitive?.contentOrNull
                            ?: ""
                        val labelCandidate = sequenceOf(
                            itemName,
                            el["name"]?.jsonPrimitive?.contentOrNull?.trim(),
                            el["label"]?.jsonPrimitive?.contentOrNull?.trim(),
                            el["desc"]?.jsonPrimitive?.contentOrNull?.trim(),
                            nestedChoice?.get("desc")?.jsonPrimitive?.contentOrNull?.trim()
                        ).firstOrNull { !it.isNullOrBlank() }
                        val fallbackSource = labelCandidate ?: nestedChoice?.toString()?.takeIf { it.isNotBlank() } ?: el.toString()
                        val generatedId = rawId.ifBlank {
                            buildFallbackStaticOptionId(parentPath, fallbackSource)
                        }
                        val optionKind = el["kind"]?.jsonPrimitive?.contentOrNull?.let { runCatching { ProficiencyKind.valueOf(it.uppercase()) }.getOrNull() }
                            ?: contextKind.takeIf { it != ProficiencyKind.NONE } ?: kind
                        val id = normalizeId(generatedId, optionKind)
                        if (rawId.isBlank()) {
                            Log.d("RALPH", "Fallback static option id '$generatedId' derived from '$fallbackSource' at $parentPath#$index")
                        }

                        val nestedChoiceDesc = nestedChoice?.get("desc")?.jsonPrimitive?.contentOrNull?.trim()
                        val optionTypeLabel = buildOptionTypeLabel(el)
                        val translationLabel = DndLocalization.translateProficiency(id)
                        fun trimNonBlank(value: String?): String? {
                            return value?.trim()?.takeIf { it.isNotBlank() }
                        }
                        val rawLabel = trimNonBlank(labelCandidate)
                            ?: trimNonBlank(optionTypeLabel)
                            ?: trimNonBlank(nestedChoiceDesc)
                            ?: translationLabel
                        val normalizedLabel = localizeStatToken(rawLabel)

                        val subChoice = nestedChoice?.let { parseChoice(it, ChoicePathManager.append(parentPath, id), proficiencyProvider, effectiveProficiencyLevel, currentDepth + 1, optionKind) }

                        ChoiceOption(id = id, label = DndLocalization.cleanLabel(normalizedLabel), info = el["desc"]?.jsonPrimitive?.contentOrNull?.stripHtml(), subChoice = subChoice, kind = optionKind)
                    }
                    is JsonPrimitive -> {
                        val finalKind = contextKind.takeIf { it != ProficiencyKind.NONE } ?: kind
                        val id = normalizeId(el.content, finalKind)
                        val label = localizeStatToken(DndLocalization.translateProficiency(id))
                        ChoiceOption(id = id, label = label, kind = finalKind)
                    }
                    else -> null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Critical failure parsing static option at $parentPath#$index", e)
                null
            }
        } ?: emptyList()
    }

    private suspend fun resolveEntitiesToOptions(
        indexes: List<String>,
        kind: ProficiencyKind,
        fallbackWeaponCategories: List<String> = emptyList(),
        fallbackArmorCategories: List<String> = emptyList()
    ): List<ChoiceOption> {
        val weapons = (classFeatureRepository.getWeaponsByIndexes(indexes) +
            fallbackWeaponCategories.flatMap { classFeatureRepository.getWeaponsByCategory(it) })
            .distinctBy { it.indexName }
        val armor = (classFeatureRepository.getArmorByIndexes(indexes) +
            fallbackArmorCategories.flatMap { classFeatureRepository.getArmorByCategory(it) })
            .distinctBy { it.indexName }
        val equipment = classFeatureRepository.getEquipmentByIndexes(indexes)

        val damageTypeIndices = weapons.mapNotNull { it.damageType }.distinct()
        val damageTypeMap = damageTypeIndices.mapNotNull { dt ->
            classFeatureRepository.getDamageTypeByIndex(dt)?.let { it.indexName to it.name }
        }.toMap()

        val weaponOptions = weapons.map { weapon ->
            ChoiceOption(
                id = weapon.indexName,
                label = weapon.name,
                info = DndLocalization.assembleEnrichedDescription(
                    DndLocalization.translateRarity(weapon.rarity),
                    DndLocalization.formatWeaponInfo(weapon.damage, damageTypeMap[weapon.damageType] ?: weapon.damageType),
                    weapon.description
                ),
                kind = kind
            )
        }
        val armorOptions = armor.map { armorEntity ->
            ChoiceOption(
                id = armorEntity.indexName,
                label = armorEntity.name,
                info = DndLocalization.assembleEnrichedDescription(
                    DndLocalization.translateRarity(armorEntity.rarity),
                    DndLocalization.formatArmorInfo(armorEntity.acBase),
                    armorEntity.description
                ),
                kind = kind
            )
        }
        val equipmentOptions = equipment.map { equip ->
            ChoiceOption(id = equip.indexName, label = equip.name, info = equip.description?.stripHtml(), kind = kind)
        }

        return (weaponOptions + armorOptions + equipmentOptions).distinctBy { it.id }.sortedBy { it.label }
    }

    private fun buildFallbackStaticOptionId(parentPath: String, seed: String): String {
        val normalizedSeed = "$parentPath:$seed"
        val normalizedHash = normalizedSeed.hashCode().let { raw ->
            val safeValue = if (raw == Int.MIN_VALUE) Int.MAX_VALUE else raw
            safeValue.absoluteValue
        }
        return "fallback-${normalizedHash.toString(16)}"
    }

    private fun buildOptionTypeLabel(el: JsonObject): String? {
        val rawType = el["option_type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: return null
        if (rawType == "reference") return null
        val baseLabel = OPTION_TYPE_TRANSLATIONS[rawType] ?: rawType.toFriendlyLabel()
        val suffix = when (rawType) {
            "ability_score_increase" -> el["points"]?.jsonPrimitive?.intOrNull?.let { points ->
                " (+$points ${formatRussianPoints(points)})"
            }
            "feat_choice" -> el["from"]?.jsonObject?.get("resource")?.jsonPrimitive?.contentOrNull?.let { resource ->
                " (${DndLocalization.translateProficiency(resource)})"
            }
            else -> null
        }?.trim() ?: ""
        return (baseLabel + suffix).trim()
    }

    private fun formatRussianPoints(points: Int): String {
        val abs = points.absoluteValue
        val lastDigit = abs % 10
        val lastTwo = abs % 100
        val word = when {
            lastTwo in 11..14 -> "очков"
            lastDigit == 1 -> "очко"
            lastDigit in 2..4 -> "очка"
            else -> "очков"
        }
        return "$points $word"
    }

    private fun String.toFriendlyLabel(): String {
        return split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
    }

    private fun extractAsiTokens(source: JsonArray?): List<String> {
        return source
            ?.mapNotNull { element ->
                when (element) {
                    is JsonPrimitive -> element.contentOrNull?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
                    is JsonObject -> element["option_type"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
                    else -> null
                }
            }
            ?: emptyList()
    }

    private fun buildAsiOptionsFromTokens(
        tokens: List<String>,
        statChoice: ChoiceOption,
        featChoice: ChoiceOption
    ): List<ChoiceOption> {
        if (tokens.isEmpty()) return emptyList()
        val includeStat = tokens.any { token ->
            token.contains("stat") ||
                token.contains("ability") ||
                token.contains("asi") ||
                VALID_STATS.contains(token.uppercase())
        }
        val includeFeat = tokens.any { token ->
            token.contains("feat") || token == "feat_choice"
        }
        val options = mutableListOf<ChoiceOption>()
        if (includeStat) options.add(statChoice)
        if (includeFeat) options.add(featChoice)
        return options
    }

    private fun getProficiencyKindFromType(type: String): ProficiencyKind {
        val t = type.lowercase()
        return when {
            t.contains("expertise") -> ProficiencyKind.SKILL
            t.contains("proficiencies") || t.contains("proficiency") -> ProficiencyKind.SKILL
            t.contains("skill") -> ProficiencyKind.SKILL
            t.contains("tool") || t.contains("artisan") || t.contains("instrument") || t.contains("gaming") -> ProficiencyKind.TOOL
            t.contains("language") -> ProficiencyKind.LANGUAGE
            else -> ProficiencyKind.NONE
        }
    }

    private fun normalizeId(raw: String, kind: ProficiencyKind): String {
        if (raw.isBlank()) return ""
        val upper = raw.uppercase()
        if (VALID_STATS.contains(upper)) return upper
        val clean = raw.lowercase().trim()
        val prefix = when(kind) {
            ProficiencyKind.SKILL -> "skill-"
            ProficiencyKind.TOOL -> "tool-"
            ProficiencyKind.LANGUAGE -> "lang-"
            else -> ""
        }
        return if (prefix.isNotEmpty() && !clean.startsWith(prefix)) "$prefix$clean" else clean
    }

    private fun localizeStatToken(raw: String): String {
        val token = raw.trim()
        val upper = token.take(3).uppercase(Locale.ROOT)
        return when (upper) {
            "STR", "DEX", "CON", "INT", "WIS", "CHA" -> DndLocalization.translateStat(upper)
            else -> token
        }
    }

    companion object {
        private val OPTION_TYPE_TRANSLATIONS = mapOf(
            "ability_score_increase" to "Улучшение характеристик",
            "feat_choice" to "Выбор черты",
            "ability_score_improvement" to "Улучшение характеристик",
            "stat_increase" to "Повышение характеристик"
        )
        private const val MAX_RECURSION_DEPTH = 10
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\FeatureFactory.kt
