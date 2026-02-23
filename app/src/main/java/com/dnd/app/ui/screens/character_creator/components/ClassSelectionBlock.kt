// Имя файла: F:/app/D&D/app/src/main/java/com/dnd/app/ui/screens/character_creator/components/ClassSelectionBlock.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.ChoiceOption

@Composable
fun ClassSelectionBlock(
    currentClassIndex: String,
    previousClassIndex: String?,
    availableClasses: List<ChoiceOption>,
    onClassSelected: (String) -> Unit,
    disabledClassIds: Set<String> = emptySet(),
    statusText: String? = null,
    statusColor: Color = Color.Blue
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SmartDropdown(
            options = availableClasses,
            selectedId = currentClassIndex,
            onSelected = { onClassSelected(it.id) },
            placeholder = "Выберите класс...",
            exclusions = disabledClassIds
        )

        if (previousClassIndex != null && currentClassIndex != previousClassIndex) {
            Text(
                statusText ?: "Вы выбрали мультиклассирование!",
                fontSize = 11.sp,
                color = statusColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
