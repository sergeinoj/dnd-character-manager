package com.dnd.app.domain.model

import com.dnd.app.domain.model.snapshot.CharacterLiveState
import kotlinx.serialization.Serializable

@Serializable
data class CharacterExportPayload(
    val draft: DraftCharacter,
    val liveState: CharacterLiveState
)
