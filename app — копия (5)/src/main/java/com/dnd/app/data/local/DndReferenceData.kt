// Имя файла: app/src/main/java/com/dnd/app/data/local/DndReferenceData.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local

/**
 * [DndReferenceData]
 *
 * Списки инструментов, языков и снаряжения теперь хранятся в таблицах
 * `languages`, `proficiencies` и `equipment`.
 * Этот файл больше не является источником истины.
 */
object DndReferenceData {
    // Оставляем хелпер, так как UI инвентаря может его использовать для группировки,
    // пока мы не реализуем полноценный запрос категорий из БД.
    fun expandToolCategory(category: String): List<String> {
        // Временная реализация: возвращаем саму категорию,
        // так как база данных теперь сама обрабатывает выборы через Feature.choices
        return listOf(category)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/DndReferenceData.kt