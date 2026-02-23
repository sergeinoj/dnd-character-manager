// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\ItemRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*
import com.dnd.app.domain.repository.ItemRepository
import com.dnd.app.domain.usecase.inventory.RawItemData
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val dao: ReferenceDao,
    private val json: Json
) : ItemRepository {


    private val virtualBundles = mapOf(
        "bundle-crossbow-light-crossbow-bolt" to RawItemData(
            indexName = "bundle-crossbow-light-crossbow-bolt",
            name = "Арбалет, лёгкий и Арбалетный болт (x20)",
            weight = 0.0,
            costCp = 0,
            categoryIndex = "bundles",
            contentsJson = """[{"item":{"index":"crossbow-light","name":"Арбалет, лёгкий"},"quantity":1},{"item":{"index":"crossbow-bolt","name":"Арбалетный болт"},"quantity":20}]"""
        ),
        "bundle-leather-armor-longbow-arrow" to RawItemData(
            indexName = "bundle-leather-armor-longbow-arrow",
            name = "Кожаный доспех, Длинный лук и Стрела (x20)",
            weight = 0.0,
            costCp = 0,
            categoryIndex = "bundles",
            contentsJson = """[{"item":{"index":"leather-armor","name":"Кожаный доспех"},"quantity":1},{"item":{"index":"longbow","name":"Длинный лук"},"quantity":1},{"item":{"index":"arrow","name":"Стрела"},"quantity":20}]"""
        ),
        "bundle-shield" to RawItemData(
            indexName = "bundle-shield",
            name = "Щит",
            weight = 0.0,
            costCp = 0,
            categoryIndex = "bundles",
            contentsJson = """[{"item":{"index":"shield","name":"Щит"},"quantity":1}]"""
        ),
        "bundle-shortbow-arrow" to RawItemData(
            indexName = "bundle-shortbow-arrow",
            name = "Короткий лук и Стрела (x20)",
            weight = 0.0,
            costCp = 0,
            categoryIndex = "bundles",
            contentsJson = """[{"item":{"index":"shortbow","name":"Короткий лук"},"quantity":1},{"item":{"index":"arrow","name":"Стрела"},"quantity":20}]"""
        )
    )

    override suspend fun getRawItem(index: String): RawItemData? {
        return virtualBundles[index] ?: getRawItems(listOf(index)).firstOrNull()
    }

    override suspend fun getRawItems(indexes: List<String>): List<RawItemData> {
        if (indexes.isEmpty()) return emptyList()

        val results = mutableListOf<RawItemData>()
        val missingFromVirtual = mutableListOf<String>()

        indexes.forEach { idx ->
            val v = virtualBundles[idx]
            if (v != null) results.add(v) else missingFromVirtual.add(idx)
        }

        if (missingFromVirtual.isEmpty()) return results

        val weapons = dao.getWeaponsByIndexes(missingFromVirtual)
        val armor = dao.getArmorByIndexes(missingFromVirtual)
        val equipment = dao.getEquipmentByIndexes(missingFromVirtual)
        val magic = dao.getMagicItemsByIndexes(missingFromVirtual)

        weapons.forEach { e -> results.add(mapWeapon(e)) }
        armor.forEach { e -> results.add(mapArmor(e)) }
        equipment.forEach { e -> results.add(mapEquipment(e)) }
        magic.forEach { e -> results.add(mapMagic(e)) }


        return results.groupBy { it.indexName }.values.map { duplicates ->
            duplicates.reduce { acc, next -> mergeRawItems(acc, next) }
        }
    }

    private fun mergeRawItems(acc: RawItemData, next: RawItemData): RawItemData {
        return acc.copy(
            weight = next.weight ?: acc.weight,
            costCp = if (next.costCp > 0) next.costCp else acc.costCp,
            categoryIndex = next.categoryIndex ?: acc.categoryIndex,
            description = if (!next.description.isNullOrBlank()) next.description else acc.description,
            damageDice = next.damageDice ?: acc.damageDice,
            damageType = next.damageType ?: acc.damageType,
            propertiesJson = next.propertiesJson ?: acc.propertiesJson,
            baseAc = next.baseAc ?: acc.baseAc,
            dexCap = next.dexCap ?: acc.dexCap,
            strMinimum = next.strMinimum ?: acc.strMinimum,
            stealthDisadvantage = next.stealthDisadvantage || acc.stealthDisadvantage,
            contentsJson = next.contentsJson ?: acc.contentsJson,
            baseItemIndex = next.baseItemIndex ?: acc.baseItemIndex,
            requiresAttunement = next.requiresAttunement || acc.requiresAttunement,
            maxCharges = if (next.maxCharges > 0) next.maxCharges else acc.maxCharges,
            chargeResetRule = next.chargeResetRule ?: acc.chargeResetRule,
            bonusAc = acc.bonusAc + next.bonusAc,
            bonusAttack = acc.bonusAttack + next.bonusAttack,
            bonusDamage = acc.bonusDamage + next.bonusDamage,
            bonusSaveDc = acc.bonusSaveDc + next.bonusSaveDc,
            grantedSpellsJson = next.grantedSpellsJson ?: acc.grantedSpellsJson,
            referenceJson = next.referenceJson ?: acc.referenceJson,
            rarity = next.rarity ?: acc.rarity,
            statOverridesJson = next.statOverridesJson ?: acc.statOverridesJson,
            mechanicsJson = next.mechanicsJson ?: acc.mechanicsJson,
            variant = next.variant ?: acc.variant
        )
    }

    private fun mapWeapon(e: WeaponEntity) = RawItemData(
        indexName = e.indexName,
        name = e.name,
        weight = e.weight,
        costCp = e.costCp,
        damageDice = e.damage,
        damageType = e.damageType,
        categoryIndex = e.categoryIndex,
        propertiesJson = e.propertiesJson,
        rarity = e.rarity,
        description = e.description
    )

    private fun mapArmor(e: ArmorEntity) = RawItemData(
        indexName = e.indexName,
        name = e.name,
        weight = e.weight,
        costCp = e.costCp,
        baseAc = e.acBase,
        dexCap = e.maxBonus,
        strMinimum = e.strMinimum,
        stealthDisadvantage = e.stealthDisadvantage == 1,
        categoryIndex = e.categoryIndex,
        rarity = e.rarity,
        description = e.description
    )

    private fun mapEquipment(e: EquipmentEntity): RawItemData {
        var extractedAc: Int? = null
        var extractedDamage: String? = null
        var extractedDamageType: String? = null

        e.armorClassJson?.let { raw ->
            runCatching {
                val obj = json.parseToJsonElement(raw).jsonObject
                extractedAc = obj["base"]?.jsonPrimitive?.intOrNull
                    ?: obj["ac"]?.jsonPrimitive?.intOrNull
            }
        }

        e.damageJson?.let { raw ->
            runCatching {
                val obj = json.parseToJsonElement(raw).jsonObject
                extractedDamage = obj["damage_dice"]?.jsonPrimitive?.content
                extractedDamageType = obj["damage_type"]?.jsonObject?.get("index")?.jsonPrimitive?.content
            }
        }

        return RawItemData(
            indexName = e.indexName,
            name = e.name,
            weight = e.weight,
            costCp = e.costCp,
            description = e.description,
            contentsJson = e.contentsJson,
            categoryIndex = e.categoryIndex,
            baseAc = extractedAc,
            damageDice = extractedDamage,
            damageType = extractedDamageType,
            strMinimum = e.strMinimum,
            stealthDisadvantage = e.stealthDisadvantage == 1
        )
    }

    private fun mapMagic(e: MagicItemEntity) = RawItemData(
        indexName = e.indexName,
        name = e.name,
        weight = e.weight,
        costCp = e.costCp,
        description = e.description,
        baseItemIndex = e.baseItemIndex,
        categoryIndex = e.categoryIndex,
        requiresAttunement = e.requiresAttunement == 1,
        maxCharges = e.maxCharges,
        chargeResetRule = e.chargeResetRule,
        bonusAc = e.bonusAc,
        bonusAttack = e.bonusAttack,
        bonusDamage = e.bonusDamage,
        bonusSaveDc = e.bonusSaveDc,
        grantedSpellsJson = e.grantedSpellsJson,
        referenceJson = e.referenceJson,
        rarity = e.rarity,
        statOverridesJson = e.statOverridesJson,
        mechanicsJson = e.mechanicsJson,
        variant = e.variant
    )
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\ItemRepositoryImpl.kt