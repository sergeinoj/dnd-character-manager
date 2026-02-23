// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\repository\ItemRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.repository

import com.dnd.app.domain.usecase.inventory.RawItemData


interface ItemRepository {

    suspend fun getRawItem(index: String): RawItemData?


    suspend fun getRawItems(indexes: List<String>): List<RawItemData>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\repository\ItemRepository.kt