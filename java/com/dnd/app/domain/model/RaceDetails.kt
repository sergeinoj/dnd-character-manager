// Имя файла: app/src/main/java/com/dnd/app/domain/model/RaceDetails.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

data class RaceDetails(
    val race: Race,
    val baseFeatures: List<Feature>,      // Фичи родителя (Тёмное зрение, etc)
    val additionalFeatures: List<Feature>, // Фичи подрасы (Мудрость +1, etc)
    val hasSubraces: Boolean,
    val subraceOptions: List<String>,
    val subraceLabel: String
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/RaceDetails.kt