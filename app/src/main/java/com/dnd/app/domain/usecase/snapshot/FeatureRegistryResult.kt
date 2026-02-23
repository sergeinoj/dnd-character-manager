// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\FeatureRegistryResult.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.snapshot

import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.snapshot.FeatureDisplayModel


data class FeatureRegistryResult(
    val domainFeatures: List<Feature>,
    val displayModels: List<FeatureDisplayModel>,
    val totalHpBonus: Int
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\snapshot\FeatureRegistryResult.kt