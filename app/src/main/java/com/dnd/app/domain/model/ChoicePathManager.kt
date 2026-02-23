// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\ChoicePathManager.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model


object ChoicePathManager {

    private const val DELIMITER = "."
    const val INDEX_MARKER = "#"

    const val ADDED_SUBKEY = "added"
    const val AVAILABLE_SUBKEY = "available"


    fun createIndexedKey(source: SelectionSource, featureIndex: String, choiceIndex: Int): String {
        val cleanFeature = featureIndex.lowercase().substringBefore(INDEX_MARKER)
        return "${source.name}$DELIMITER$cleanFeature$INDEX_MARKER$choiceIndex"
    }


    fun createRootKey(source: SelectionSource, featureIndex: String): String {
        return createIndexedKey(source, featureIndex, 0)
    }


    fun append(parentPath: String, childId: String, choiceIndex: Int = 0): String {
        val cleanChild = childId.lowercase().substringBefore(INDEX_MARKER)
        return "$parentPath$DELIMITER$cleanChild$INDEX_MARKER$choiceIndex"
    }

    fun isRootFeatureKey(key: String, source: SelectionSource): Boolean {
        val prefix = "${source.name}$DELIMITER"
        return key.startsWith(prefix) && key.count { it == '.' } == 1
    }

    fun isChildOf(parentKey: String, potentialChildKey: String): Boolean {
        return potentialChildKey.startsWith(parentKey + DELIMITER)
    }

    fun isPreparedSpellPath(path: String): Boolean {
        val lowerPath = path.lowercase()
        return lowerPath.contains(DndConstants.VirtualKeys.PREPARED_SPELLS_PREFIX) ||
                lowerPath.contains(DndConstants.VirtualKeys.AGGREGATED_SPELL_CHOICE)
    }

    fun extractChoiceIndex(key: String): Int? {
        return key.substringAfterLast(INDEX_MARKER, "").toIntOrNull()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\ChoicePathManager.kt