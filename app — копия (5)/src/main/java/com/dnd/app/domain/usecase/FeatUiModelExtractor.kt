// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/FeatUiModelExtractor.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.FeatureChoiceDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatUiModelExtractor @Inject constructor() {

    /**
     * Извлекает модель выбора для UI из данных черты (feat).
     * Теперь полагается на то, что Feature.choices уже может быть заполнен
     * динамическими выборами в Data-слое (например, в DictionaryDataSourceImpl.mapFeature).
     */
    fun extractChoiceModel(feat: Feature): FeatureChoiceDomain? {
        // Просто возвращаем первый элемент из списка выборов, если он существует.
        // Логика по созданию этого выбора из reference_json перенесена в data-слой.
        return feat.choices.firstOrNull()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/FeatUiModelExtractor.kt