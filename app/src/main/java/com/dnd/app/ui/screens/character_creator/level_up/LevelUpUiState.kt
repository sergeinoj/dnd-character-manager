// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\level_up\LevelUpUiState.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.level_up

import com.dnd.app.domain.model.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf


data class LevelUpUiState(
    val draft: DraftCharacter = DraftCharacter(),
    val isLoading: Boolean = true,
    val interactionError: String? = null,
    val validationIssues: List<ValidationIssue> = emptyList(),


    val currentClassInfo: ClassInfo? = null,
    val availableClasses: List<ClassInfo> = emptyList(),
    val classStepFeatures: List<Feature> = emptyList(),
    val availableSubclasses: List<SubclassInfo> = emptyList(),
    val subclassChoiceFeature: Feature? = null,
    val aggregatedSpellFeature: Feature? = null,
    val featMetadataRegistry: Map<String, Feature> = emptyMap(),
    val proficiencyExclusions: Map<Int, Set<String>> = emptyMap(),


    val expandedStates: PersistentMap<String, Boolean> = persistentMapOf()
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\ui\screens\character_creator\level_up\LevelUpUiState.kt