// Имя файла: app/src/main/java/com/dnd/app/domain/model/ClassFeaturesForLevel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

/**
 * Структурированный контейнер для способностей, получаемых классом на определенном уровне.
 * Разделяет способности базового класса, выбор подкласса и способности выбранного подкласса.
 * Является результатом работы "интеллектуального" источника данных (ClassDataSource).
 */
data class ClassFeaturesForLevel(
    // Способности, общие для всего класса на этом уровне (включая виртуальные).
    val baseClassFeatures: List<Feature> = emptyList(),

    // Способность, представляющая собой выбор архетипа/домена/пути (если есть на этом уровне).
    val subclassChoiceFeature: Feature? = null,

    // Способности, полученные от УЖЕ ВЫБРАННОГО подкласса на этом уровне.
    // Этот список будет пуст, если подкласс еще не выбран.
    val selectedSubclassFeatures: List<Feature> = emptyList()
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/ClassFeaturesForLevel.kt