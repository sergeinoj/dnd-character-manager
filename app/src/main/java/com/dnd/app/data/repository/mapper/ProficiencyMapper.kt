// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\mapper\ProficiencyMapper.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository.mapper

import com.dnd.app.domain.model.DndConstants
import com.dnd.app.domain.model.ProficiencyKind
import com.dnd.app.domain.model.StaticProficiency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProficiencyMapper @Inject constructor(
    private val dao: com.dnd.app.data.local.dao.ReferenceDao
) {

    suspend fun map(index: String): StaticProficiency {

        var kind = when {
            index.startsWith("skill-") -> ProficiencyKind.SKILL
            index.startsWith("tool-") -> ProficiencyKind.TOOL
            index.startsWith("lang-") -> ProficiencyKind.LANGUAGE
            index.startsWith(DndConstants.VirtualKeys.SAVING_THROW_PREFIX) -> ProficiencyKind.SAVING_THROW
            index.startsWith("armor-") -> ProficiencyKind.ARMOR
            index.startsWith("weapon-") -> ProficiencyKind.WEAPON
            else -> ProficiencyKind.NONE
        }


        if (kind == ProficiencyKind.NONE) {
            val dbType = dao.getProficiencyType(index)
            kind = when (dbType) {
                "Armor" -> ProficiencyKind.ARMOR
                "Weapons" -> ProficiencyKind.WEAPON
                "Tools" -> ProficiencyKind.TOOL
                "Vehicles" -> ProficiencyKind.TOOL
                else -> ProficiencyKind.NONE
            }
        }


        if (kind == ProficiencyKind.NONE) {
            val catIndex = dao.getEquipmentCategoryFor(index)
            kind = when {
                catIndex == null -> ProficiencyKind.NONE
                catIndex.contains("tools") -> ProficiencyKind.TOOL
                catIndex.contains("kits") -> ProficiencyKind.TOOL
                catIndex.contains("gaming-sets") -> ProficiencyKind.TOOL
                catIndex.contains("musical-instruments") -> ProficiencyKind.TOOL
                catIndex.contains("vehicles") -> ProficiencyKind.TOOL
                catIndex.contains("weapon") -> ProficiencyKind.WEAPON
                catIndex.contains("armor") -> ProficiencyKind.ARMOR
                else -> ProficiencyKind.NONE
            }
        }


        if (kind == ProficiencyKind.NONE) {
            kind = when {
                index in setOf("light-armor", "medium-armor", "heavy-armor", "shields") -> ProficiencyKind.ARMOR
                index in setOf("simple-weapons", "martial-weapons") -> ProficiencyKind.WEAPON
                index == "disguise-kit" || index == "forgery-kit" || index == "poisoner-kit" || index == "herbalism-kit" -> ProficiencyKind.TOOL
                else -> ProficiencyKind.NONE
            }
        }

        return StaticProficiency(id = index, kind = kind)
    }

    suspend fun mapRaw(index: String, hint: ProficiencyKind): StaticProficiency {
        return when (hint) {
            ProficiencyKind.SAVING_THROW -> {
                val id = "${DndConstants.VirtualKeys.SAVING_THROW_PREFIX}${index.lowercase()}"
                StaticProficiency(id = id, kind = ProficiencyKind.SAVING_THROW)
            }
            else -> map(index)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\data\repository\mapper\ProficiencyMapper.kt