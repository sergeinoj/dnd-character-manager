// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\SheetCharacter.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model.snapshot

import kotlinx.serialization.Serializable


@Serializable
data class SheetCharacter(
    val id: Long,
    val snapshot: CharacterSnapshot,
    val liveState: CharacterLiveState
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app\domain\model\snapshot\SheetCharacter.kt