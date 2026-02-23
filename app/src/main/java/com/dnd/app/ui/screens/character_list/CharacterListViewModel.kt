package com.dnd.app.ui.screens.character_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.usecase.CharacterExporter
import com.dnd.app.domain.usecase.CharacterImporter
import com.dnd.app.domain.usecase.CharacterUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterListViewModel @Inject constructor(
    private val useCases: CharacterUseCases,
    private val exporter: CharacterExporter,
    private val importer: CharacterImporter
) : ViewModel() {

    val characters: StateFlow<List<CharacterDomain>> = useCases.getAllCharacters()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private val _exportRequest = MutableStateFlow<ExportRequest?>(null)
    val exportRequest: StateFlow<ExportRequest?> = _exportRequest.asStateFlow()

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            useCases.deleteCharacter(id)
        }
    }

    fun importCharacterFromJson(content: String) {
        viewModelScope.launch {
            importer.import(content)
                .onSuccess { _importMessage.value = "\u0418\u043c\u043f\u043e\u0440\u0442 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d" }
                .onFailure { _importMessage.value = "\u041e\u0448\u0438\u0431\u043a\u0430 \u0438\u043c\u043f\u043e\u0440\u0442\u0430: ${it.message}" }
        }
    }

    fun requestExport(characterId: Long, format: ExportFormat) {
        viewModelScope.launch {
            exporter.export(characterId)
                .onSuccess { bundle ->
                    val characterName = characters.value.firstOrNull { it.id == characterId }?.name
                        ?.ifBlank { "character_$characterId" }
                        ?: "character_$characterId"
                    val safeName = characterName.replace(Regex("[^A-Za-z\\u0400-\\u04FF0-9._-]"), "_")
                    val ts = System.currentTimeMillis()
                    val file = when (format) {
                        ExportFormat.LSS -> ExportFile(
                            fileName = "${safeName}_${ts}.lss.json",
                            content = bundle.lssJson
                        )

                        ExportFormat.DND -> ExportFile(
                            fileName = "${safeName}_${ts}.dnd.json",
                            content = bundle.dndJson
                        )
                    }
                    _exportRequest.value = ExportRequest(files = listOf(file))
                }
                .onFailure { _exportMessage.value = "\u041e\u0448\u0438\u0431\u043a\u0430 \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430: ${it.message}" }
        }
    }

    fun consumeImportMessage() {
        _importMessage.value = null
    }

    fun consumeExportMessage() {
        _exportMessage.value = null
    }

    fun consumeExportRequest() {
        _exportRequest.value = null
    }

    fun notifyExportSaved(success: Boolean, error: String? = null) {
        _exportMessage.value = if (success) {
            "\u042d\u043a\u0441\u043f\u043e\u0440\u0442 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d"
        } else {
            "\u041e\u0448\u0438\u0431\u043a\u0430 \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430: ${error ?: "\u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0444\u0430\u0439\u043b"}"
        }
    }

    data class ExportFile(
        val fileName: String,
        val content: String
    )

    data class ExportRequest(
        val files: List<ExportFile>
    )

    enum class ExportFormat {
        LSS,
        DND
    }
}

