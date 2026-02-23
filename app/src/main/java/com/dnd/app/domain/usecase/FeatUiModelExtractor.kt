// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\FeatUiModelExtractor.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatUiModelExtractor @Inject constructor() {


    fun extractChoiceModel(feat: Feature): FeatureChoiceDomain? {


        return feat.choices.firstOrNull()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\usecase\FeatUiModelExtractor.kt