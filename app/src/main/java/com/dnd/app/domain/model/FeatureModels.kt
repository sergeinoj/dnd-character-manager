// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\FeatureModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import com.dnd.app.domain.model.snapshot.ResetRule
import kotlinx.serialization.Serializable

@Serializable
enum class ProficiencyKind {
    SKILL, TOOL, LANGUAGE, SAVING_THROW, WEAPON, ARMOR, FEAT, NONE
}

@Serializable
data class ChoiceOption(
    val id: String,
    val label: String,
    val info: String? = null,
    val subChoice: FeatureChoiceDomain? = null,
    val spell: Spell? = null,
    val kind: ProficiencyKind = ProficiencyKind.NONE
)

@Serializable
data class Feature(
    val id: Int,
    val index: String,
    val name: String,
    val description: String,
    val classIndex: String? = null,
    val subclassIndex: String? = null,
    val raceIndex: String? = null,
    val subraceIndex: String? = null,
    val level: Int? = null,
    val choices: List<FeatureChoiceDomain> = emptyList(),
    val embeddedSpells: List<Spell> = emptyList(),
    val changeRule: Boolean = false,
    val isSubraceSelector: Boolean = false,
    val priority: Int = 100,
    val grantedProficiencies: List<String> = emptyList(),

    val maxCharges: Int = 0,
    val resetRule: ResetRule = ResetRule.LONG_REST,

    val referenceJson: String? = null,
    val uiGroup: String? = "GENERAL"
)

interface ProficiencyChoice {
    val targetProficiencyLevel: Int
}

@Serializable
sealed class FeatureChoiceDomain {
    abstract val count: Int
    abstract val options: List<ChoiceOption>
    val isContainer: Boolean get() = options.any { it.subChoice != null }

    @Serializable
    data class SelectSpell(
        override val count: Int,
        val poolType: String,
        override val options: List<ChoiceOption>,
        val autoAdjustLimit: Boolean = false
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectOption(
        override val count: Int,
        override val options: List<ChoiceOption>,
        val description: String? = null,
        val proficiencyKind: ProficiencyKind = ProficiencyKind.NONE,
        override val targetProficiencyLevel: Int = 1,
        val isTransparent: Boolean = false
    ) : FeatureChoiceDomain(), ProficiencyChoice

    @Serializable
    data class SelectStatBonus(
        override val count: Int,
        val amount: Int,
        override val options: List<ChoiceOption>,
        val allowDuplicateSelections: Boolean = false,
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectExpertise(
        override val count: Int,
        override val options: List<ChoiceOption>,
        val proficiencyKind: ProficiencyKind = ProficiencyKind.SKILL
    ) : FeatureChoiceDomain(), ProficiencyChoice {
        override val targetProficiencyLevel: Int = 2
    }

    @Serializable
    data class SelectSkill(
        override val count: Int,
        override val options: List<ChoiceOption>
    ) : FeatureChoiceDomain(), ProficiencyChoice {
        override val targetProficiencyLevel: Int = 1
    }

    @Serializable
    data class InvalidChoice(val reason: String) : FeatureChoiceDomain() {
        override val count: Int = 0
        override val options: List<ChoiceOption> = emptyList()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\FeatureModels.kt
