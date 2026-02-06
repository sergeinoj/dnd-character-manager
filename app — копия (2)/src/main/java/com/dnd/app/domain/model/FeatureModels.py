// Имя файла: app/src/main/java/com/dnd/app/domain/model/FeatureModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChoiceOption(
    val id: String,
    val label: String,
    val info: String? = null,
    // Вложенный выбор, который активируется при выборе этой опции
    val subChoice: FeatureChoiceDomain? = null
)

@Serializable
data class Feature(
    val id: Int,
    val index: String,
    val name: String,
    val description: String,
    val level: Int? = null,
    val choices: List<FeatureChoiceDomain> = emptyList(),
    val embeddedSpells: List<Spell> = emptyList(),
    val changeRule: Boolean = false,
    val isSubraceSelector: Boolean = false,
    val priority: Int = 100
)

@Serializable
sealed class FeatureChoiceDomain {
    abstract val count: Int
    abstract val options: List<ChoiceOption>

    @Serializable
    data class SelectSkill(
        override val count: Int,
        override val options: List<ChoiceOption>
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectSpell(
        override val count: Int,
        val poolType: String,
        override val options: List<ChoiceOption>
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectOption(
        override val count: Int,
        override val options: List<ChoiceOption>,
        val description: String? = null,
        val isStringChoice: Boolean = false
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectStatBonus(
        override val count: Int,
        val amount: Int,
        override val options: List<ChoiceOption>
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectExpertise(
        override val count: Int,
        override val options: List<ChoiceOption>
    ) : FeatureChoiceDomain()
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/FeatureModels.kt