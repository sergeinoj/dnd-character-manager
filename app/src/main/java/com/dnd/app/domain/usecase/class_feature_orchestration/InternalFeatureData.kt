// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\InternalFeatureData.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase.class_feature_orchestration

import com.dnd.app.data.local.entity.FeatureEntity
import com.dnd.app.domain.model.ClassFeaturesForLevel


internal data class InternalFeatureData(
    val result: ClassFeaturesForLevel,
    val sourceEntities: List<FeatureEntity>
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\class_feature_orchestration\InternalFeatureData.kt