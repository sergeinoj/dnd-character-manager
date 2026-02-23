package com.dnd.app.ui.screens.character_list

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.ui.components.DndTopBar
import kotlinx.coroutines.launch

@Composable
fun CharacterListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToSheet: (Long) -> Unit,
    viewModel: CharacterListViewModel = hiltViewModel()
) {
    val characters by viewModel.characters.collectAsState()
    val importMessage by viewModel.importMessage.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()
    val exportRequest by viewModel.exportRequest.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pendingExportFiles by remember { mutableStateOf<List<CharacterListViewModel.ExportFile>>(emptyList()) }
    var pendingExportIndex by remember { mutableIntStateOf(0) }
    var nextExportFileName by remember { mutableStateOf<String?>(null) }
    var importMenuExpanded by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: error("Input stream is null")
        }.onSuccess { payload ->
            viewModel.importCharacterFromJson(payload)
        }.onFailure { err ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    "\u041e\u0448\u0438\u0431\u043a\u0430 \u0447\u0442\u0435\u043d\u0438\u044f \u0444\u0430\u0439\u043b\u0430: ${err.message}",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingExportFiles.getOrNull(pendingExportIndex)
        var shouldFinalize = true
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.content.toByteArray(Charsets.UTF_8))
                } ?: error("Output stream is null")
            }.onSuccess {
                val nextIndex = pendingExportIndex + 1
                if (nextIndex < pendingExportFiles.size) {
                    pendingExportIndex = nextIndex
                    nextExportFileName = pendingExportFiles[nextIndex].fileName
                    shouldFinalize = false
                } else {
                    viewModel.notifyExportSaved(success = true)
                }
            }.onFailure { err ->
                viewModel.notifyExportSaved(success = false, error = err.message)
            }
        } else {
            viewModel.notifyExportSaved(success = false, error = "\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435 \u043e\u0442\u043c\u0435\u043d\u0435\u043d\u043e")
        }
        if (shouldFinalize) {
            pendingExportFiles = emptyList()
            pendingExportIndex = 0
            viewModel.consumeExportRequest()
        }
    }

    LaunchedEffect(importMessage) {
        importMessage?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short) }
            viewModel.consumeImportMessage()
        }
    }

    LaunchedEffect(exportMessage) {
        exportMessage?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short) }
            viewModel.consumeExportMessage()
        }
    }

    LaunchedEffect(exportRequest) {
        exportRequest?.let { req ->
            if (req.files.isEmpty()) return@let
            pendingExportFiles = req.files
            pendingExportIndex = 0
            nextExportFileName = req.files.first().fileName
        }
    }

    LaunchedEffect(nextExportFileName) {
        nextExportFileName?.let { name ->
            exportLauncher.launch(name)
            nextExportFileName = null
        }
    }

    Scaffold(
        topBar = {
            DndTopBar("\u0412\u044b\u0431\u043e\u0440 \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0430", false) {
                Box {
                    IconButton(onClick = { importMenuExpanded = true }) {
                        Icon(Icons.Default.Save, contentDescription = "Import", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = importMenuExpanded,
                        onDismissRequest = { importMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import .json") },
                            onClick = {
                                importMenuExpanded = false
                                importLauncher.launch(arrayOf("application/json", "text/plain"))
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Default.Add, contentDescription = "Create")
            }
        }
    ) { innerPadding ->
        if (characters.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("\u0421\u043f\u0438\u0441\u043e\u043a \u043f\u0443\u0441\u0442", style = MaterialTheme.typography.titleLarge)
                Text("\u0421\u043e\u0437\u0434\u0430\u0439\u0442\u0435 \u0441\u0432\u043e\u0435\u0433\u043e \u043f\u0435\u0440\u0432\u043e\u0433\u043e \u0433\u0435\u0440\u043e\u044f!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(characters) { character ->
                    CharacterItem(
                        character = character,
                        onClick = { onNavigateToSheet(character.id) },
                        onDelete = { viewModel.deleteCharacter(character.id) },
                        onExportLss = { viewModel.requestExport(character.id, CharacterListViewModel.ExportFormat.LSS) },
                        onExportDnd = { viewModel.requestExport(character.id, CharacterListViewModel.ExportFormat.DND) }
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterItem(
    character: CharacterDomain,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExportLss: () -> Unit,
    onExportDnd: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name.ifBlank { "\u0411\u0435\u0437\u044b\u043c\u044f\u043d\u043d\u044b\u0439" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildString {
                        append(character.raceName)
                        val classLines = character.className
                            .split("/", ",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        if (classLines.isNotEmpty()) {
                            append("\n")
                            append(classLines.joinToString("\n"))
                        }
                        append("\n\u0423\u0440. ${character.level}")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(modifier = Modifier.size(40.dp)) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("\u042d\u043a\u0441\u043f\u043e\u0440\u0442 .lss.json") },
                        onClick = {
                            menuExpanded = false
                            onExportLss()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("\u042d\u043a\u0441\u043f\u043e\u0440\u0442 .dnd.json") },
                        onClick = {
                            menuExpanded = false
                            onExportDnd()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("\u0423\u0434\u0430\u043b\u0438\u0442\u044c") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

