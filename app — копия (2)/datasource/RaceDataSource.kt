```kotlin
// Имя файла: ui/screens/character_sheet/tabs/BioTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.Bio

@Composable
fun BioTab(bio: Bio) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BioField("Черты характера", bio.traits)
        BioField("Идеалы", bio.ideals)
        BioField("Привязанности", bio.bonds)
        BioField("Слабости", bio.flaws)
        BioField("Предыстория", bio.background)
        BioField("Заметки", bio.notes)
    }
}

@Composable
fun BioField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {}, // Read-only пока
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true
    )
    Spacer(modifier = Modifier.height(8.dp))
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/BioTab.kt
```
------
```kotlin
// Имя файла: ui/screens/character_sheet/tabs/SpellsTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.Spell

@Composable
fun SpellsTab(
    spells: List<Spell>
) {
    if (spells.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Книга заклинаний пуста.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(spells) { spell ->
                SpellCard(spell)
            }
        }
    }
}

@Composable
fun SpellCard(spell: Spell) {
    Card(
        modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(spell.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("${spell.level} круг, ${spell.school}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            Text("Время: ${spell.castingTime} | Дистанция: ${spell.range}", style = MaterialTheme.typography.bodySmall)
            Text(spell.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/SpellsTab.kt
```
------
```kotlin
// Имя файла: ui/screens/character_sheet/tabs/InventoryTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.Weapon

@Composable
fun InventoryTab(
    items: List<Weapon> // В будущем можно сделать общий интерфейс Item
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Рюкзак", style = MaterialTheme.typography.titleLarge)
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (items.isEmpty()) {
            Text("Пусто", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(items) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text("${item.weight} фнт. | ${item.cost}") },
                        trailingContent = { Text("1 шт.") }
                    )
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/InventoryTab.kt
```
------
```kotlin
// Имя файла: ui/screens/character_sheet/tabs/CombatTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnd.app.domain.model.Weapon

@Composable
fun CombatTab(
    weapons: List<Weapon>
) {
    if (weapons.isEmpty()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("В руках пусто. Добавьте оружие в инвентарь.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(weapons) { weapon ->
                WeaponCard(weapon)
            }
        }
    }
}

@Composable
fun WeaponCard(weapon: Weapon) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text(weapon.name, fontWeight = FontWeight.Bold)
                Text(weapon.damage, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Text("Тип: ${weapon.damageType}", style = MaterialTheme.typography.bodySmall)
            if (weapon.properties.isNotEmpty()) {
                Text("Св-ва: ${weapon.properties}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/CombatTab.kt
```
------
```kotlin
// Имя файла: MainActivity.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dnd.app.ui.DndApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Возвращаем приложение к жизни!
        setContent {
            DndApp()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: MainActivity.kt
```
------
```kotlin
// Имя файла: ui/screens/character_list/CharacterListScreen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.ui.components.DndTopBar

@Composable
fun CharacterListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToSheet: (Long) -> Unit,
    viewModel: CharacterListViewModel = hiltViewModel()
) {
    val characters by viewModel.characters.collectAsState()

    Scaffold(
        topBar = { DndTopBar("Выбор персонажа", false) },
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
                Text("Список пуст", style = MaterialTheme.typography.titleLarge)
                Text("Создайте своего первого героя!")
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
                        onDelete = { viewModel.deleteCharacter(character.id) }
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
    onDelete: () -> Unit
) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = character.name.ifBlank { "Безымянный" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Уровень ${character.level}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_list/CharacterListScreen.kt
```
------
```kotlin
// Имя файла: ui/screens/character_list/CharacterListViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.usecase.CharacterUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterListViewModel @Inject constructor(
    private val useCases: CharacterUseCases
) : ViewModel() {

    // Подписываемся на поток персонажей из БД
    val characters: StateFlow<List<CharacterDomain>> = useCases.getAllCharacters()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            useCases.deleteCharacter(id)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_list/CharacterListViewModel.kt
```
------
```kotlin
// Имя файла: ui/components/Inputs.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.calculator.DndCalculator

@Composable
fun StatInput(
    name: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    calculator: DndCalculator
) {
    val modifier = calculator.calculateModifier(value)
    val modString = calculator.formatModifier(modifier)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Название (Сила, Ловкость...)
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        // Блок Значения и Модификатора
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Кнопка Минус
            IconButton(
                onClick = { if (value > 1) onValueChange(value - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            // Центральный квадрат (как на скриншоте)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(60.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                // Большой модификатор
                Text(
                    text = modString,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                // Мелкое значение
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Кнопка Плюс
            IconButton(
                onClick = { if (value < 30) onValueChange(value + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Если список пуст или индекс неверен, показываем заглушку
    val displayText = if (options.isNotEmpty() && selectedIndex in options.indices) {
        options[selectedIndex]
    } else {
        "Выберите..."
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            readOnly = true,
            value = displayText,
            onValueChange = { },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, selectionOption ->
                DropdownMenuItem(
                    text = { Text(text = selectionOption) },
                    onClick = {
                        onOptionSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/components/Inputs.kt
```
------
```kotlin
// Имя файла: ui/DndApp.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dnd.app.ui.navigation.DndNavGraph
import com.dnd.app.ui.theme.DndTheme

@Composable
fun DndApp() {
    DndTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            DndNavGraph(navController = navController)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/DndApp.kt
```
------
```kotlin
// Имя файла: ui/navigation/Screen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.navigation

sealed class Screen(val route: String) {
    // Главный экран: Список персонажей
    data object CharacterList : Screen("character_list")

    // Экран создания: Мастер
    data object CharacterCreator : Screen("character_creator")

    // Экран персонажа: Передаем ID
    data object CharacterSheet : Screen("character_sheet/{characterId}") {
        fun createRoute(characterId: Long) = "character_sheet/$characterId"
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/navigation/Screen.kt
```
------
```kotlin
// Имя файла: domain/usecase/CharacterUseCases.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.repository.CharacterRepository
import javax.inject.Inject

// @Inject говорит Hilt'у, как создавать этот класс. Модуль не нужен.
data class CharacterUseCases @Inject constructor(
    val getAllCharacters: GetAllCharactersUseCase,
    val getCharacter: GetCharacterByIdUseCase,
    val saveCharacter: SaveCharacterUseCase,
    val deleteCharacter: DeleteCharacterUseCase
)

class GetAllCharactersUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    operator fun invoke() = repository.getAllCharacters()
}

class GetCharacterByIdUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke(id: Long): CharacterDomain? {
        return repository.getCharacterById(id)
    }
}

class SaveCharacterUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke(character: CharacterDomain): Long {
        if (character.name.isBlank()) {
            throw IllegalArgumentException("Character name cannot be empty")
        }
        return repository.saveCharacter(character)
    }
}

class DeleteCharacterUseCase @Inject constructor(
    private val repository: CharacterRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteCharacter(id)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: domain/usecase/CharacterUseCases.kt
```
------
```xml
// Имя файла: AndroidManifest.xml
// --- НАЧАЛО ФАЙЛА ---
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:tools="http://schemas.android.com/tools">

<application
android:name=".DndApplication"
android:allowBackup="true"
android:dataExtractionRules="@xml/data_extraction_rules"
android:fullBackupContent="@xml/backup_rules"
android:icon="@mipmap/ic_launcher"
android:label="@string/app_name"
android:roundIcon="@mipmap/ic_launcher_round"
android:supportsRtl="true"
android:theme="@style/Theme.DD">
<activity
android:name=".MainActivity"
android:exported="true"
android:label="@string/app_name"
android:theme="@style/Theme.DD">
<intent-filter>
<action android:name="android.intent.action.MAIN" />

<category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
</activity>
</application>

</manifest>
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: AndroidManifest.xml
```
------
```kotlin
// Имя файла: DndApplication.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DndApplication : Application()
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: DndApplication.kt
```
------
```kotlin
// Имя файла: di/AppModule.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.di

import com.dnd.app.data.repository.CharacterRepositoryImpl
import com.dnd.app.data.repository.LibraryRepositoryImpl
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository {
        return impl
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: di/AppModule.kt
```
------
```kotlin
// Имя файла: domain/repository/CharacterRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.repository

import com.dnd.app.domain.model.CharacterDomain
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun getAllCharacters(): Flow<List<CharacterDomain>>
    suspend fun getCharacterById(id: Long): CharacterDomain?
    suspend fun saveCharacter(character: CharacterDomain): Long
    suspend fun deleteCharacter(characterId: Long)
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: domain/repository/CharacterRepository.kt
```
------
```kotlin
// Имя файла: ui/theme/Type.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/theme/Type.kt
```
------

```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/local/dao/ReferenceDao.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.dnd.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceDao {
    // --- КЛАССЫ ---
    @Query("SELECT * FROM classes ORDER BY name ASC")
    suspend fun getAllClasses(): List<ClassEntity>

    @Query("SELECT * FROM subclasses WHERE class_index = :classIndex ORDER BY name ASC")
    suspend fun getSubclassesForClass(classIndex: String): List<SubclassEntity>

    @Query("SELECT * FROM progression WHERE class_index = :classIndex AND level = :level")
    suspend fun getProgressionForLevel(classIndex: String, level: Int): List<ProgressionEntity>

    // --- ФИЧИ ---
    @Query("SELECT * FROM features WHERE index_name IN (:indexes) ORDER BY level ASC")
    suspend fun getFeaturesByIndexes(indexes: List<String>): List<FeatureEntity>

    // ИСПРАВЛЕНИЕ: Добавлены недостающие методы поиска фич
    @Query("SELECT * FROM features WHERE id = :id")
    suspend fun getFeatureById(id: Int): FeatureEntity?

    @Query("SELECT * FROM features WHERE index_name = :indexName")
    suspend fun getFeatureByIndex(indexName: String): FeatureEntity?

    @Query("SELECT * FROM features WHERE class_index = :classIdx OR subclass_index = :subIdx OR race_index = :raceIdx OR subrace_index = :subraceIdx OR background_index = :bgIdx")
    suspend fun findFeaturesByContext(
        classIdx: String? = null,
        subIdx: String? = null,
        raceIdx: String? = null,
        subraceIdx: String? = null,
        bgIdx: String? = null
    ): List<FeatureEntity>

    // --- СНАРЯЖЕНИЕ И КАТЕГОРИИ ---
    @Query("""
        SELECT * FROM equipment WHERE category_index = :catIdx
        UNION
        SELECT * FROM equipment WHERE index_name IN (SELECT item_index FROM equipment_category_links WHERE category_index = :catIdx)
    """)
    suspend fun getEquipmentByCategory(catIdx: String): List<EquipmentEntity>

    // --- РАСЫ ---
    // ИСПРАВЛЕНИЕ: Унифицировано название метода getAllRaces
    @Query("SELECT * FROM races ORDER BY name ASC")
    suspend fun getAllRaces(): List<RaceEntity>

    @Query("SELECT * FROM subraces WHERE race_index = :raceIndex ORDER BY name ASC")
    suspend fun getSubracesForRace(raceIndex: String): List<SubraceEntity>

    // --- БЭКГРАУНДЫ ---
    @Query("SELECT * FROM backgrounds ORDER BY name ASC")
    suspend fun getAllBackgrounds(): List<BackgroundEntity>

    @Query("SELECT * FROM alignments ORDER BY id ASC")
    suspend fun getAllAlignments(): List<AlignmentEntity>

    // --- МАГИЯ ---
    @Query("SELECT * FROM spells WHERE index_name IN (:indexes)")
    suspend fun getSpellsByIndexes(indexes: List<String>): List<SpellEntity>

    @Query("SELECT * FROM spells WHERE level = :level AND (classes_json LIKE '%' || :clsIdx || '%' OR :clsIdx IS NULL) ORDER BY name ASC")
    suspend fun getSpellsByLevel(level: Int, clsIdx: String?): List<SpellEntity>

    @Query("SELECT * FROM spells ORDER BY name ASC")
    fun getAllSpells(): Flow<List<SpellEntity>>

    @Query("SELECT * FROM spells WHERE id IN (:ids)")
    suspend fun getSpellsByIds(ids: List<Int>): List<SpellEntity>

    // --- ИНВЕНТАРЬ ---
    @Query("SELECT * FROM weapons ORDER BY label ASC")
    fun getAllWeapons(): Flow<List<WeaponEntity>>

    @Query("SELECT * FROM weapons WHERE id IN (:ids)")
    suspend fun getWeaponsByIds(ids: List<Int>): List<WeaponEntity>

    @Query("SELECT * FROM armor ORDER BY name ASC")
    fun getAllArmor(): Flow<List<ArmorEntity>>

    @Query("SELECT * FROM equipment WHERE name LIKE '%' || :query || '%'")
    suspend fun searchEquipment(query: String): List<EquipmentEntity>

    @Query("SELECT id FROM equipment WHERE index_name IN (:idxNames)")
    suspend fun getEquipmentIdsByIdxNames(idxNames: List<String>): List<Int>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/dao/ReferenceDao.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/CreatorComponents.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.util.DndLocalization
import com.dnd.app.util.stripHtml
import kotlinx.serialization.json.Json

@Composable
fun FlatWizardSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth().border(1.dp, Color(0xFF424242))) {
        if (title.isNotBlank()) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF424242)).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Color.White)
            }
        }
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFC0C0C0)).padding(8.dp)) {
            content()
        }
    }
}

@Composable
fun SmartDropdown(
    options: List<ChoiceOption>,
    selectedId: String?,
    onSelected: (ChoiceOption) -> Unit,
    placeholder: String = "Пусто",
    exclusions: Set<String> = emptySet()
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.id == selectedId }
    val displayText = selectedOption?.label ?: placeholder

    val filteredOptions = options.filter { it.id == selectedId || !exclusions.contains(it.id) }

    Column {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color.White, RoundedCornerShape(2.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
            .clickable { if (filteredOptions.isNotEmpty()) expanded = true }
            .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = displayText,
                    color = if (selectedOption == null) Color.Gray else Color.Black,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = Color.Black)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White)) {
                filteredOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.label, color = Color.Black, fontSize = 14.sp) },
                        onClick = { onSelected(opt); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureChoiceBlock(
    choice: FeatureChoiceDomain,
    currentSelection: ChoiceResult?,
    onSelectionChanged: (ChoiceResult) -> Unit,
    globalExclusions: Set<String> = emptySet(),
    pickedSkills: List<String> = emptyList()
) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        when (choice) {
            is FeatureChoiceDomain.SelectSkill -> {
                val selected = (currentSelection as? ChoiceResult.Skills)?.skillIndexes ?: emptyList()
                repeat(choice.count) { i ->
                    SmartDropdown(
                        options = choice.options,
                        selectedId = selected.getOrNull(i),
                        onSelected = { opt ->
                            val newList = selected.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                            onSelectionChanged(ChoiceResult.Skills(newList.filter { it.isNotBlank() }))
                        },
                        exclusions = globalExclusions + selected.filterIndexed { index, _ -> index != i },
                        placeholder = "Выберите навык..."
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            is FeatureChoiceDomain.SelectOption -> {
                val selected = (currentSelection as? ChoiceResult.SelectedOptions)?.items ?: emptyList()
                repeat(choice.count) { i ->
                    val currentId = selected.getOrNull(i)
                    val selectedOption = choice.options.find { it.id == currentId }

                    Column {
                        SmartDropdown(
                            options = choice.options,
                            selectedId = currentId,
                            onSelected = { opt ->
                                val newList = selected.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                                onSelectionChanged(ChoiceResult.SelectedOptions(newList.filter { it.isNotBlank() }))
                            },
                            exclusions = globalExclusions + selected.filterIndexed { index, _ -> index != i },
                            placeholder = "Выберите вариант..."
                        )

                        // РЕКУРСИЯ: Если у выбранного пункта есть вложенный выбор
                        if (selectedOption?.subChoice != null) {
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier
                                .padding(start = 12.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                            ) {
                                FeatureChoiceBlock(
                                    choice = selectedOption.subChoice,
                                    currentSelection = null, // Вложенные выборы требуют отдельной логики сохранения
                                    onSelectionChanged = { /* Обработка вложенности */ },
                                    pickedSkills = pickedSkills
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            is FeatureChoiceDomain.SelectSpell -> {
                val selected = (currentSelection as? ChoiceResult.Spells)?.spellIndexes ?: emptyList()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    choice.options.forEach { opt ->
                        val isSelected = selected.contains(opt.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0xFFE8F5E9) else Color.White)
                                .border(1.dp, Color.LightGray)
                                .clickable {
                                    val newList = if (isSelected) selected - opt.id else (selected + opt.id).take(choice.count)
                                    onSelectionChanged(ChoiceResult.Spells(newList))
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF2E7D32) else Color.Gray
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(opt.label, fontSize = 14.sp)
                        }
                    }
                }
            }
            is FeatureChoiceDomain.SelectStatBonus -> {
                val sb = (currentSelection as? ChoiceResult.StatBonus)?.bonuses ?: emptyMap()
                val keys = sb.keys.toList()
                repeat(choice.count) { i ->
                    SmartDropdown(
                        options = choice.options,
                        selectedId = keys.getOrNull(i),
                        onSelected = { opt ->
                            val nm = sb.toMutableMap()
                            if(keys.getOrNull(i) != null) nm.remove(keys[i])
                            nm[opt.id] = choice.amount
                            onSelectionChanged(ChoiceResult.StatBonus(nm))
                        },
                        exclusions = globalExclusions + keys.filterIndexed { index, _ -> index != i }
                    )
                }
            }
            is FeatureChoiceDomain.SelectExpertise -> {
                val selected = (currentSelection as? ChoiceResult.Skills)?.skillIndexes ?: emptyList()
                val options = pickedSkills.map { ChoiceOption(it, DndLocalization.translateSkill(it)) }

                repeat(choice.count) { i ->
                    SmartDropdown(
                        options = options,
                        selectedId = selected.getOrNull(i),
                        onSelected = { opt ->
                            val newList = selected.toMutableList().apply { while (size <= i) add(""); set(i, opt.id) }
                            onSelectionChanged(ChoiceResult.Skills(newList.filter { it.isNotBlank() }))
                        },
                        exclusions = selected.filterIndexed { index, _ -> index != i }.toSet(),
                        placeholder = "Выберите мастерство..."
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun EmbeddedSpellRow(spell: Spell) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).background(Color.White).border(1.dp, Color.Gray)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(spell.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${spell.level} уровень, ${spell.school}", fontSize = 11.sp, color = Color.Gray)
            }
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
        }
        if (expanded) {
            Column(modifier = Modifier.padding(8.dp).background(Color(0xFFF5F5F5)).fillMaxWidth().padding(8.dp)) {
                Text("Время: ${spell.castingTime}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Дистанция: ${spell.range}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Компоненты: ${spell.components.stripHtml()}", fontSize = 12.sp)
                Text("Длительность: ${spell.duration}", fontSize = 12.sp)
                Divider(Modifier.padding(vertical = 4.dp))
                Text(spell.description.stripHtml(), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/components/CreatorComponents.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/domain/model/FeatureModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChoiceOption(
    val id: String,
    val label: String,
    val info: String? = null,
    // Вложенный выбор, который активируется при выборе этой опции
    val subChoice: FeatureChoiceDomain? = null
)

@Serializable
data class Feature(
    val id: Int,
    val index: String,
    val name: String,
    val description: String,
    val level: Int? = null,
    val choices: List<FeatureChoiceDomain> = emptyList(),
    val embeddedSpells: List<Spell> = emptyList(),
    val changeRule: Boolean = false,
    val isSubraceSelector: Boolean = false,
    val priority: Int = 100
)

@Serializable
sealed class FeatureChoiceDomain {
    abstract val count: Int
    abstract val options: List<ChoiceOption>

    @Serializable
    data class SelectSkill(
        override val count: Int,
        override val options: List<ChoiceOption>
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectSpell(
        override val count: Int,
        val poolType: String,
        override val options: List<ChoiceOption>
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectOption(
        override val count: Int,
        override val options: List<ChoiceOption>,
        val description: String? = null,
        val isStringChoice: Boolean = false
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectStatBonus(
        override val count: Int,
        val amount: Int,
        override val options: List<ChoiceOption>
    ) : FeatureChoiceDomain()

    @Serializable
    data class SelectExpertise(
        override val count: Int,
        override val options: List<ChoiceOption>
    ) : FeatureChoiceDomain()
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/FeatureModels.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/util/DndLocalization.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.util

import java.util.Locale

object DndLocalization {

    val ALL_SKILLS = mapOf(
        "acrobatics" to "Акробатика", "animal-handling" to "Уход за животными",
        "arcana" to "Магия", "athletics" to "Атлетика", "deception" to "Обман",
        "history" to "История", "insight" to "Проницательность", "intimidation" to "Запугивание",
        "investigation" to "Анализ", "medicine" to "Медицина", "nature" to "Природа",
        "perception" to "Внимательность", "performance" to "Выступление", "persuasion" to "Убеждение",
        "religion" to "Религия", "sleight-of-hand" to "Ловкость рук", "stealth" to "Скрытность",
        "survival" to "Выживание"
    )

    private val statTranslations = mapOf(
        "STR" to "Сила", "DEX" to "Ловкость", "CON" to "Телосложение",
        "INT" to "Интеллект", "WIS" to "Мудрость", "CHA" to "Харизма"
    )

    /**
     * Перевод специфических заголовков для фич-выборов.
     */
    fun translateFeatureChoiceHeader(index: String): String {
        return when {
            index.contains("fighting-style") -> "Боевой стиль"
            index.contains("favored-enemy") -> "Избранный враг"
            index.contains("natural-explorer") -> "Знание местности"
            index.contains("sorcerous-origin") -> "Происхождение чародея"
            index.contains("draconic-ancestry") -> "Драконье наследие"
            else -> ""
        }
    }

    fun translateStat(code: String): String = statTranslations[code.take(3).uppercase()] ?: code

    fun translateSkill(id: String): String {
        val cleanId = id.replace("skill-", "").lowercase().trim()
        return ALL_SKILLS[cleanId] ?: id.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun cleanLabel(label: String): String {
        return label.replace("Навык: ", "").replace("Skill: ", "").replace("Proficiency: ", "").replace("Saving Throw: ", "Спасбросок: ").trim()
    }

    fun translateProficiency(name: String): String {
        val cleaned = cleanLabel(name)
        if (cleaned.startsWith("Спасбросок: ")) {
            val stat = cleaned.substringAfter(": ")
            return "Спасбросок: ${translateStat(stat)}"
        }
        return cleaned
    }

    fun getSpeciesHeader(parentRaceIndex: String): String {
        val speciesGenitive = mapOf("dwarf" to "дварфов", "elf" to "эльфов", "gnome" to "гномов", "halfling" to "полуросликов", "human" to "людей", "dragonborn" to "драконорожденных", "tiefling" to "тифлингов")
        return "Виды ${speciesGenitive[parentRaceIndex.lowercase()] ?: parentRaceIndex}"
    }

    fun getStatIncreaseSummary(bonuses: Map<String, Int>): String {
        if (bonuses.isEmpty()) return ""
        return "Значение вашей " + bonuses.entries.joinToString { "${translateStat(it.key)} увеличивается на ${it.value}" }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/util/DndLocalization.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/ClassStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.components.*
import com.dnd.app.util.stripHtml

@Composable
fun ClassStep(
    availableClasses: List<ClassInfo>,
    selectedClassIndex: String,
    onClassSelect: (String) -> Unit,
    onSubclassSelect: (String) -> Unit,
    classFeatures: List<Feature>,
    currentSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    globalExclusions: Set<String>,
    currentSubclassIndex: String?,
    pickedSkills: List<String>
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1. ВЫБОР КЛАССА
        item {
            FlatWizardSection(title = "Класс") {
                val opts = availableClasses.map { ChoiceOption(it.index, it.name) }
                SmartDropdown(opts, selectedClassIndex, onSelected = { onClassSelect(it.id) })
            }
        }

        val selClass = availableClasses.find { it.index == selectedClassIndex }
        if (selClass != null) {
            // 2. ХИТЫ
            item {
                FlatWizardSection(title = "Хиты") {
                    Column {
                        Text("Кость здоровья: 1к${selClass.hitDie}", fontSize = 14.sp)
                        Text("Начальное здоровье: ${selClass.hitDie} + Мод. Телосложения", fontSize = 14.sp, color = Color.DarkGray)
                    }
                }
            }

            // 3. СПЕЦИАЛИЗАЦИЯ (Только те, кто выбирает на 1-м уровне)
            // ПАТЧ: Убраны bard и wizard, т.к. они выбирают позже.
            val classesWithLvl1Subclass = listOf("cleric", "sorcerer", "warlock")
            if (selClass.subclasses.isNotEmpty() && (selectedClassIndex in classesWithLvl1Subclass)) {
                item {
                    FlatWizardSection(title = "Специализация") {
                        val subOpts = selClass.subclasses.map { ChoiceOption(it.index, it.name) }
                        SmartDropdown(subOpts, currentSubclassIndex, onSelected = { onSubclassSelect(it.id) }, placeholder = "Выберите путь...")
                    }
                }
            }

            // 4. ДИНАМИЧЕСКИЕ ФИЧИ (Навыки, Экспертиза, Магия)
            items(classFeatures, key = { "f_${it.id}_${it.index}" }) { feat ->
                FlatWizardSection(title = feat.name) {
                    Column {
                        if (feat.description.isNotBlank()) {
                            Text(feat.description.stripHtml(), fontSize = 14.sp, lineHeight = 17.sp)
                        }
                        feat.embeddedSpells.forEach { EmbeddedSpellRow(it) }
                        FeatureChoiceBlock(
                            choice = feat.choices.firstOrNull() ?: return@Column, // Поддерживаем один основной выбор на фичу
                            currentSelection = currentSelections[feat.index],
                            onSelectionChanged = { onSelectionChanged(feat.index, it) },
                            globalExclusions = globalExclusions,
                            pickedSkills = pickedSkills
                        )
                        // Если выборов несколько (редко, но бывает)
                        if (feat.choices.size > 1) {
                            feat.choices.drop(1).forEach { additionalChoice ->
                                FeatureChoiceBlock(additionalChoice, currentSelections[feat.index], { onSelectionChanged(feat.index, it) }, globalExclusions, pickedSkills)
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/ClassStep.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorScreen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.ui.components.DndActionTopBar
import com.dnd.app.ui.screens.character_creator.tabs.*
import com.dnd.app.ui.theme.DndBackground
import com.dnd.app.ui.theme.DndPrimary

@Composable
fun CharacterCreatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CharacterCreatorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        CreatorTab("Раса", Icons.Default.Person),
        CreatorTab("Класс", Icons.Default.AccountBox),
        CreatorTab("Статы", Icons.Default.Build),
        CreatorTab("Био", Icons.Default.Face),
        CreatorTab("Вещи", Icons.Default.ShoppingCart)
    )

    val dynamicRaceBonuses = remember(state.draft.baseInfo.raceSelections) {
        val bonuses = mutableMapOf<String, Int>()
        state.draft.baseInfo.raceSelections.values.forEach { result ->
            if (result is ChoiceResult.StatBonus) {
                result.bonuses.forEach { (stat, value) ->
                    val key = stat.take(3).uppercase()
                    bonuses[key] = (bonuses[key] ?: 0) + value
                }
            }
        }
        bonuses
    }

    Scaffold(
        topBar = {
            DndActionTopBar(
                title = if (state.draft.name.isBlank()) "Новый герой" else state.draft.name,
                onBack = onNavigateBack,
                onActionClick = { viewModel.saveCharacter(onSuccess = onNavigateBack) },
                actionIcon = { Icon(Icons.Default.Check, null, tint = Color.Black) }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DndPrimary) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        containerColor = DndBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                when (selectedTab) {
                    0 -> RaceStep(
                        availableRaces = state.availableRaces,
                        selectedRaceIndex = state.draft.baseInfo.raceIndex,
                        onRaceSelect = viewModel::selectRace,
                        availableSubraces = state.availableSubraces,
                        selectedSubraceIndex = state.draft.baseInfo.subraceIndex,
                        onSubraceSelect = viewModel::selectSubrace,
                        features = state.raceFeatures,
                        currentSelections = state.draft.baseInfo.raceSelections,
                        onSelectionChanged = viewModel::onRaceSelectionChange,
                        globalExclusions = state.globalExclusions
                    )
                    1 -> ClassStep(
                        availableClasses = state.availableClasses,
                        selectedClassIndex = state.draft.levelStack.firstOrNull()?.classIndex ?: "",
                        onClassSelect = viewModel::selectClass,
                        onSubclassSelect = viewModel::selectSubclass,
                        classFeatures = state.classFeatures,
                        currentSelections = state.draft.levelStack.firstOrNull()?.selections ?: emptyMap(),
                        onSelectionChanged = viewModel::onClassSelectionChange,
                        globalExclusions = state.globalExclusions,
                        currentSubclassIndex = state.draft.levelStack.firstOrNull()?.subclassIndex,
                        pickedSkills = state.draft.getPickedSkills() // ПРОБРОС ВЛАДЕНИЙ
                    )
                    2 -> StatsStep(
                        scores = state.draft.baseInfo.baseAbilityScores,
                        staticBonuses = state.draft.baseInfo.staticRaceBonuses,
                        dynamicBonuses = dynamicRaceBonuses,
                        onStatChange = viewModel::updateStat
                    )
                    3 -> BioStep(
                        name = state.draft.name,
                        onNameChange = { viewModel.updateName(it) },
                        availableAlignments = state.availableAlignments,
                        selectedAlignment = state.draft.baseInfo.alignmentIndex,
                        onAlignmentSelect = { viewModel.selectAlignment(it) },
                        availableBackgrounds = state.availableBackgrounds,
                        selectedBackground = state.draft.baseInfo.backgroundIndex,
                        onBackgroundSelect = { viewModel.selectBackground(it) },
                        backgroundFeatures = state.backgroundFeatures,
                        currentSelections = state.draft.baseInfo.backgroundSelections,
                        onSelectionChanged = { id, res -> viewModel.onBgSelectionChange(id, res) },
                        personalityTrait = state.draft.baseInfo.personalityTrait,
                        ideal = state.draft.baseInfo.ideal,
                        bond = state.draft.baseInfo.bond,
                        flaw = state.draft.baseInfo.flaw,
                        onRollTrait = { viewModel.rollCharacterTrait(it) },
                        onManualBioChange = { type, value -> viewModel.updateBioField(type, value) }
                    )
                    4 -> InventoryStep(
                        className = state.draft.levelStack.firstOrNull()?.classIndex,
                        backgroundName = state.draft.baseInfo.backgroundIndex
                    )
                }
            }
        }
    }
}

data class CreatorTab(val title: String, val icon: ImageVector)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorScreen.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/domain/model/DraftCharacter.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class ChoiceResult {
    @Serializable
    data class Skills(val skillIndexes: List<String>) : ChoiceResult()
    @Serializable
    data class Spells(val spellIndexes: List<String>) : ChoiceResult()
    @Serializable
    data class SelectedOptions(val items: List<String>) : ChoiceResult()
    @Serializable
    data class StatBonus(val bonuses: Map<String, Int>) : ChoiceResult()
    @Serializable
    data class Note(val text: String) : ChoiceResult()
}

@Serializable
data class LevelStep(
    val classIndex: String,
    val subclassIndex: String? = null,
    val hpIncrease: Int = 0,
    val selections: Map<String, ChoiceResult> = emptyMap()
)

@Serializable
data class BaseInfo(
    val raceIndex: String = "",
    val subraceIndex: String? = null,
    val backgroundIndex: String = "",
    val alignmentIndex: String = "",
    val personalityTrait: String = "",
    val ideal: String = "",
    val bond: String = "",
    val flaw: String = "",
    val baseAbilityScores: Map<String, Int> = mapOf(
        "STR" to 8, "DEX" to 8, "CON" to 8,
        "INT" to 8, "WIS" to 8, "CHA" to 8
    ),
    val staticRaceBonuses: Map<String, Int> = emptyMap(),
    val raceSelections: Map<String, ChoiceResult> = emptyMap(),
    val backgroundSelections: Map<String, ChoiceResult> = emptyMap()
)

@Serializable
data class DraftCharacter(
    val id: Long = 0,
    val name: String = "",
    val baseInfo: BaseInfo = BaseInfo(),
    val levelStack: List<LevelStep> = emptyList()
) {
    /**
     * Собирает все ID навыков, выбранных игроком на данный момент.
     * Используется для наполнения списка Экспертизы.
     */
    fun getPickedSkills(): List<String> {
        val skills = mutableSetOf<String>()

        // 1. Из расы
        baseInfo.raceSelections.values.filterIsInstance<ChoiceResult.Skills>().forEach {
            skills.addAll(it.skillIndexes)
        }

        // 2. Из классов (проходим по всем уровням)
        levelStack.forEach { step ->
            step.selections.values.filterIsInstance<ChoiceResult.Skills>().forEach {
                skills.addAll(it.skillIndexes)
            }
            // Инструменты или навыки из SelectedOptions (иногда навыки приходят там)
            step.selections.values.filterIsInstance<ChoiceResult.SelectedOptions>().forEach { opt ->
                skills.addAll(opt.items.filter { it.startsWith("skill-") })
            }
        }

        return skills.toList()
    }

    fun getGlobalExclusions(): Set<String> {
        val exclusions = mutableSetOf<String>()
        baseInfo.staticRaceBonuses.keys.forEach { exclusions.add(it) }
        baseInfo.raceSelections.values.forEach { exclusions.addAll(extractIds(it)) }
        levelStack.forEach { step ->
            // ВАЖНО: Мы НЕ добавляем выборы экспертизы в исключения,
            // иначе один дропдаун экспертизы заблокирует другой.
            step.selections.forEach { (featIdx, res) ->
                if (!featIdx.contains("expertise")) {
                    exclusions.addAll(extractIds(res))
                }
            }
        }
        return exclusions
    }

    private fun extractIds(result: ChoiceResult): List<String> {
        return when (result) {
            is ChoiceResult.Skills -> result.skillIndexes
            is ChoiceResult.Spells -> result.spellIndexes
            is ChoiceResult.SelectedOptions -> result.items
            is ChoiceResult.StatBonus -> result.bonuses.keys.toList()
            else -> emptyList()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/DraftCharacter.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/BioStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.components.*
import com.dnd.app.util.stripHtml

@Composable
fun BioStep(
    name: String, onNameChange: (String) -> Unit, availableAlignments: List<AlignmentEntity>,
    selectedAlignment: String, onAlignmentSelect: (String) -> Unit, availableBackgrounds: List<Background>,
    selectedBackground: String, onBackgroundSelect: (Background) -> Unit, backgroundFeatures: List<Feature>,
    currentSelections: Map<String, ChoiceResult>, onSelectionChanged: (String, ChoiceResult) -> Unit,
    personalityTrait: String, ideal: String, bond: String, flaw: String,
    onRollTrait: (String) -> Unit, onManualBioChange: (String, String) -> Unit
) {
    val selectedBg = availableBackgrounds.find { it.name == selectedBackground }
    Column(modifier = Modifier.fillMaxSize().padding(8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlatWizardSection(title = "Личность") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BioRow("Имя", name, onNameChange)
                Column {
                    Text("Мировоззрение", fontSize = 12.sp, color = Color.DarkGray)
                    val opts = availableAlignments.map { ChoiceOption(it.indexName, it.name) }
                    SmartDropdown(opts, selectedAlignment, onSelected = { onAlignmentSelect(it.id) })
                }
            }
        }
        FlatWizardSection(title = "Предыстория") {
            val opts = availableBackgrounds.map { ChoiceOption(it.name, it.name) }
            SmartDropdown(opts, selectedBackground, onSelected = { o -> availableBackgrounds.find { it.name == o.id }?.let { onBackgroundSelect(it) } })
        }
        if (selectedBg != null) {
            FlatWizardSection(title = "Характер") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RandomTraitRow("Черта", personalityTrait, { onRollTrait("personality") }, { onManualBioChange("personality", it) })
                    RandomTraitRow("Идеал", ideal, { onRollTrait("ideal") }, { onManualBioChange("ideal", it) })
                    RandomTraitRow("Привязанность", bond, { onRollTrait("bond") }, { onManualBioChange("bond", it) })
                    RandomTraitRow("Изъян", flaw, { onRollTrait("flaw") }, { onManualBioChange("flaw", it) })
                }
            }
        }
        backgroundFeatures.forEach { f ->
            FlatWizardSection(title = f.name) {
                Column {
                    if (f.description.isNotBlank()) Text(f.description.stripHtml(), fontSize = 14.sp)
                    f.choices.forEach { c -> FeatureChoiceBlock(c, currentSelections[f.index], { onSelectionChanged(f.index, it) }) }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun RandomTraitRow(label: String, value: String, onRoll: () -> Unit, onManualChange: (String) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424242))
            IconButton(onClick = onRoll, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, null, tint = Color(0xFF1B5E20)) }
        }
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color.Gray).padding(8.dp)) {
            BasicTextField(value = value, onValueChange = onManualChange, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp), textStyle = TextStyle(fontSize = 13.sp, color = Color.Black), decorationBox = { if(value.isEmpty()) Text("Нажми 🎲 или впиши...", color = Color.Gray, fontSize = 13.sp); it() })
        }
    }
}

@Composable
fun BioRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(36.dp).border(1.dp, Color.Gray), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color(0xFFE0E0E0)).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) { Text(label, fontSize = 13.sp) }
        Box(modifier = Modifier.weight(0.6f).fillMaxHeight().background(Color.White).padding(8.dp), contentAlignment = Alignment.CenterStart) {
            BasicTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = TextStyle(fontSize = 13.sp))
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/BioStep.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.CharacterRepository
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.CharacterAssembler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatorUiState(
    val draft: DraftCharacter = DraftCharacter(),
    val availableRaces: List<Race> = emptyList(),
    val availableSubraces: List<Race> = emptyList(),
    val availableClasses: List<ClassInfo> = emptyList(),
    val availableBackgrounds: List<Background> = emptyList(),
    val availableAlignments: List<AlignmentEntity> = emptyList(),
    val raceFeatures: List<Feature> = emptyList(),
    val classFeatures: List<Feature> = emptyList(),
    val backgroundFeatures: List<Feature> = emptyList(),
    val globalExclusions: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CharacterCreatorViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val characterRepository: CharacterRepository,
    private val assembler: CharacterAssembler
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorUiState())
    val uiState = _uiState.asStateFlow()

    init { loadInitialData() }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(
                availableRaces = libraryRepository.getAllParentRaces(),
                availableClasses = libraryRepository.getAllClasses(),
                availableBackgrounds = libraryRepository.getAllBackgrounds(),
                availableAlignments = libraryRepository.getAllAlignments(),
                isLoading = false
            ) }
        }
    }

    private fun updateGlobalExclusions() { _uiState.update { it.copy(globalExclusions = it.draft.getGlobalExclusions()) } }

    fun selectRace(idx: String) {
        viewModelScope.launch {
            val r = _uiState.value.availableRaces.find { it.index == idx } ?: return@launch
            val subraces = libraryRepository.getSubracesFromDb(r.id)
            val features = libraryRepository.getRaceFeatures(r.id, null)
            _uiState.update { it.copy(
                draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(raceIndex = idx, subraceIndex = null, raceSelections = emptyMap(), staticRaceBonuses = r.baseStats)),
                availableSubraces = subraces, raceFeatures = features
            ) }
            updateGlobalExclusions()
        }
    }

    fun selectSubrace(idx: String) {
        viewModelScope.launch {
            val rIdx = _uiState.value.draft.baseInfo.raceIndex
            val r = _uiState.value.availableRaces.find { it.index == rIdx } ?: return@launch
            val feats = libraryRepository.getRaceFeatures(r.id, idx)
            _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(subraceIndex = idx)), raceFeatures = feats) }
            updateGlobalExclusions()
        }
    }

    fun onRaceSelectionChange(fIdx: String, res: ChoiceResult) {
        val sel = _uiState.value.draft.baseInfo.raceSelections.toMutableMap()
        sel[fIdx] = res
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(raceSelections = sel))) }
        updateGlobalExclusions()
    }

    fun selectClass(idx: String) {
        viewModelScope.launch {
            val feats = libraryRepository.getProgressionFeatures(idx, 1, null)
            _uiState.update { it.copy(draft = it.draft.copy(levelStack = listOf(LevelStep(classIndex = idx))), classFeatures = feats) }
            updateGlobalExclusions()
        }
    }

    fun selectSubclass(sIdx: String) {
        val cur = _uiState.value.draft.levelStack.firstOrNull() ?: return
        viewModelScope.launch {
            val feats = libraryRepository.getProgressionFeatures(cur.classIndex, 1, sIdx)
            _uiState.update { it.copy(draft = it.draft.copy(levelStack = listOf(cur.copy(subclassIndex = sIdx))), classFeatures = feats) }
            updateGlobalExclusions()
        }
    }

    fun onClassSelectionChange(fIdx: String, res: ChoiceResult) {
        val stack = _uiState.value.draft.levelStack.toMutableList()
        if (stack.isNotEmpty()) {
            val sel = stack[0].selections.toMutableMap(); sel[fIdx] = res
            stack[0] = stack[0].copy(selections = sel)
            _uiState.update { it.copy(draft = it.draft.copy(levelStack = stack)) }
            updateGlobalExclusions()
        }
    }

    fun selectBackground(bg: Background) {
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(backgroundIndex = bg.name, backgroundSelections = emptyMap())), backgroundFeatures = bg.features) }
        updateGlobalExclusions()
    }

    fun onBgSelectionChange(fIdx: String, res: ChoiceResult) {
        val sel = _uiState.value.draft.baseInfo.backgroundSelections.toMutableMap(); sel[fIdx] = res
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(backgroundSelections = sel))) }
        updateGlobalExclusions()
    }

    fun rollCharacterTrait(type: String) {
        val bgName = _uiState.value.draft.baseInfo.backgroundIndex
        val bg = _uiState.value.availableBackgrounds.find { it.name == bgName } ?: return
        val list = when(type) { "personality" -> bg.personalityTraits; "ideal" -> bg.ideals; "bond" -> bg.bonds; "flaw" -> bg.flaws; else -> emptyList() }
        if (list.isNotEmpty()) updateBioField(type, list.random())
    }

    fun updateBioField(type: String, v: String) {
        val b = _uiState.value.draft.baseInfo
        val nb = when(type) { "personality" -> b.copy(personalityTrait = v); "ideal" -> b.copy(ideal = v); "bond" -> b.copy(bond = v); "flaw" -> b.copy(flaw = v); else -> b }
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = nb)) }
    }

    fun updateName(n: String) { _uiState.update { it.copy(draft = it.draft.copy(name = n)) } }
    fun selectAlignment(idx: String) { _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(alignmentIndex = idx))) } }
    fun updateStat(s: String, d: Int) {
        val sc = _uiState.value.draft.baseInfo.baseAbilityScores.toMutableMap()
        sc[s] = ((sc[s] ?: 8) + d).coerceIn(8, 15)
        _uiState.update { it.copy(draft = it.draft.copy(baseInfo = it.draft.baseInfo.copy(baseAbilityScores = sc))) }
    }

    fun saveCharacter(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val char = assembler.assemble(_uiState.value.draft)
            characterRepository.saveCharacter(char)
            onSuccess()
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/CharacterCreatorViewModel.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/model/FeatureJsonModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ProgressionSpellcastingJson(
    @SerialName("cantrips_known") val cantripsKnown: Int? = null,
    @SerialName("spells_known") val spellsKnown: Int? = null,
    @SerialName("spell_slots_level_1") val spellSlotsLevel1: Int? = null,
    @SerialName("pact_slots") val pactSlots: Int? = null,
    @SerialName("pact_slot_level") val pactSlotLevel: Int? = null
)

@Serializable
data class SubclassSpellsJson(
    val type: String? = null,
    val list: List<SubclassSpellLevelGroup> = emptyList()
)

@Serializable
data class SubclassSpellLevelGroup(
    val level: Int,
    val spells: List<String>
)

@Serializable
data class ChoiceJson(
    val choose: Int = 1,
    val type: String = "",
    val from: OptionSetJson? = null,
    val desc: String? = null
)

@Serializable
data class OptionSetJson(
    @SerialName("option_set_type") val optionSetType: String? = null,
    val options: List<OptionJson> = emptyList(),
    @SerialName("equipment_category") val equipmentCategory: ReferenceJson? = null
)

@Serializable
data class OptionJson(
    @SerialName("option_type") val optionType: String? = null,
    val item: ReferenceJson? = null,
    val choice: ChoiceJson? = null,
    val string: String? = null,
    val desc: String? = null
)

@Serializable
data class ReferenceJson(
    val index: String = "",
    val name: String = ""
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/model/FeatureJsonModels.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/local/ReferenceDatabase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dnd.app.data.local.dao.ReferenceDao
import com.dnd.app.data.local.entity.*

@Database(
    entities = [
        ClassEntity::class,
        SubclassEntity::class,
        ProgressionEntity::class,
        FeatureEntity::class,
        RaceEntity::class,
        SubraceEntity::class,
        BackgroundEntity::class,
        AlignmentEntity::class,
        SpellEntity::class,
        MagicSchoolEntity::class,
        EquipmentEntity::class,
        WeaponEntity::class,
        ArmorEntity::class,
        MagicItemEntity::class,
        WeaponPropertyEntity::class,
        ProficiencyEntity::class,
        SkillEntity::class,
        LanguageEntity::class,
        DamageTypeEntity::class,
        EquipmentCategoryEntity::class,
        EquipmentCategoryLinkEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ReferenceDatabase : RoomDatabase() {
    abstract fun referenceDao(): ReferenceDao
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/ReferenceDatabase.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/ReferenceEntities.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "classes", indices = [Index(value = ["index_name"], unique = true)])
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "hit_die") val hitDie: Int?,
    @ColumnInfo(name = "proficiency_choices_json") val proficiencyChoicesJson: String?,
    @ColumnInfo(name = "proficiencies_json") val proficienciesJson: String?,
    @ColumnInfo(name = "saving_throws_json") val savingThrowsJson: String?,
    @ColumnInfo(name = "starting_equipment_json") val startingEquipmentJson: String?,
    @ColumnInfo(name = "starting_equipment_options_json") val startingEquipmentOptionsJson: String?,
    @ColumnInfo(name = "spellcasting_json") val spellcastingJson: String?,
    @ColumnInfo(name = "class_levels_url") val classLevelsUrl: String?,
    @ColumnInfo(name = "multi_classing_json") val multiClassingJson: String?,
    @ColumnInfo(name = "subclasses_json") val subclassesJson: String?,
    @ColumnInfo(name = "spells_url") val spellsUrl: String?
)

@Entity(tableName = "subclasses", indices = [Index(value = ["index_name"], unique = true)])
data class SubclassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    @ColumnInfo(name = "class_index") val classIndex: String,
    val name: String,
    @ColumnInfo(name = "subclass_flavor") val subclassFlavor: String?,
    val desc: String?,
    @ColumnInfo(name = "spells_json") val spellsJson: String?,
    @ColumnInfo(name = "subclass_levels_url") val subclassLevelsUrl: String?
)

@Entity(tableName = "progression", indices = [Index(value = ["entity_index"], unique = true)])
data class ProgressionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "entity_index") val entityIndex: String,
    val level: Int,
    @ColumnInfo(name = "class_index") val classIndex: String,
    @ColumnInfo(name = "subclass_index") val subclassIndex: String?,
    @ColumnInfo(name = "ability_score_bonuses") val abilityScoreBonuses: Int?,
    @ColumnInfo(name = "prof_bonus") val profBonus: Int?,
    @ColumnInfo(name = "feature_indices_json") val featureIndicesJson: String?,
    @ColumnInfo(name = "class_specific_json") val classSpecificJson: String?,
    @ColumnInfo(name = "subclass_specific_json") val subclassSpecificJson: String?,
    @ColumnInfo(name = "spellcasting_json") val spellcastingJson: String?
)

@Entity(tableName = "features", indices = [Index(value = ["index_name"], unique = true)])
data class FeatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    val level: Int?,
    @ColumnInfo(name = "class_index") val classIndex: String?,
    @ColumnInfo(name = "subclass_index") val subclassIndex: String?,
    @ColumnInfo(name = "race_index") val raceIndex: String?,
    @ColumnInfo(name = "subrace_index") val subraceIndex: String?,
    @ColumnInfo(name = "background_index") val backgroundIndex: String?,
    @ColumnInfo(name = "choices_json") val choicesJson: String?,
    @ColumnInfo(name = "spell_show_json") val spellShowJson: String?,
    @ColumnInfo(name = "change_rule") val changeRule: Int?,
    @ColumnInfo(name = "prerequisites_json") val prerequisitesJson: String?,
    @ColumnInfo(name = "reference_json") val referenceJson: String?
)

@Entity(tableName = "races", indices = [Index(value = ["index_name"], unique = true)])
data class RaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    val speed: Int?,
    @ColumnInfo(name = "ability_bonuses_json") val abilityBonusesJson: String?,
    val age: String?,
    val alignment: String?,
    val size: String?,
    @ColumnInfo(name = "size_desc") val sizeDescription: String?,
    @ColumnInfo(name = "languages_json") val languagesJson: String?,
    @ColumnInfo(name = "language_desc") val languageDesc: String?,
    @ColumnInfo(name = "traits_json") val traitsJson: String?,
    @ColumnInfo(name = "starting_proficiencies_json") val startingProficienciesJson: String?,
    @ColumnInfo(name = "starting_proficiency_options_json") val startingProficiencyOptionsJson: String?,
    @ColumnInfo(name = "language_options_json") val languageOptionsJson: String?,
    @ColumnInfo(name = "subraces_json") val subracesJson: String?
)

@Entity(tableName = "subraces", indices = [Index(value = ["index_name"], unique = true)])
data class SubraceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    @ColumnInfo(name = "race_index") val raceIndex: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "ability_bonuses_json") val abilityBonusesJson: String?,
    @ColumnInfo(name = "traits_json") val traitsJson: String?,
    @ColumnInfo(name = "starting_proficiencies_json") val startingProficienciesJson: String?,
    @ColumnInfo(name = "language_options_json") val languageOptionsJson: String?
)

@Entity(tableName = "backgrounds", indices = [Index(value = ["index_name"], unique = true)])
data class BackgroundEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "starting_proficiencies_json") val startingProficienciesJson: String?,
    @ColumnInfo(name = "language_options_json") val languageOptionsJson: String?,
    @ColumnInfo(name = "starting_equipment_json") val startingEquipmentJson: String?,
    @ColumnInfo(name = "feature_index") val featureIndex: String?,
    @ColumnInfo(name = "feature_name") val featureName: String?,
    @ColumnInfo(name = "feature_desc") val featureDesc: String?,
    @ColumnInfo(name = "personality_traits_json") val personalityTraitsJson: String?,
    @ColumnInfo(name = "ideals_json") val idealsJson: String?,
    @ColumnInfo(name = "bonds_json") val bondsJson: String?,
    @ColumnInfo(name = "flaws_json") val flawsJson: String?,
    @ColumnInfo(name = "starting_equipment_options_json") val startingEquipmentOptionsJson: String?
)

@Entity(tableName = "alignments", indices = [Index(value = ["index_name"], unique = true)])
data class AlignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val abbreviation: String?,
    val desc: String?
)

@Entity(tableName = "spells", indices = [Index(value = ["index_name"], unique = true)])
data class SpellEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val level: Int?,
    val school: String?,
    @ColumnInfo(name = "casting_time") val castingTime: String?,
    val range: String?,
    @ColumnInfo(name = "components_json") val componentsJson: String?,
    val material: String?,
    val duration: String?,
    val concentration: Int?,
    val ritual: Int?,
    val description: String?,
    @ColumnInfo(name = "higher_level") val higherLevel: String?,
    @ColumnInfo(name = "classes_json") val classesJson: String?,
    @ColumnInfo(name = "damage_json") val damageJson: String?,
    @ColumnInfo(name = "attack_type") val attackType: String?,
    @ColumnInfo(name = "dc_json") val dcJson: String?,
    @ColumnInfo(name = "area_of_effect_json") val areaOfEffectJson: String?,
    @ColumnInfo(name = "heal_at_slot_level_json") val healAtSlotLevelJson: String?,
    @ColumnInfo(name = "subclasses_json") val subclassesJson: String?
)

@Entity(tableName = "magic_schools", indices = [Index(value = ["index_name"], unique = true)])
data class MagicSchoolEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?
)

@Entity(tableName = "equipment", indices = [Index(value = ["index_name"], unique = true)])
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    @ColumnInfo(name = "cost_json") val costJson: String?,
    val weight: Double?,
    val description: String?,
    @ColumnInfo(name = "armor_class_json") val armorClassJson: String?,
    @ColumnInfo(name = "str_minimum") val strMinimum: Int?,
    @ColumnInfo(name = "stealth_disadvantage") val stealthDisadvantage: Int?,
    @ColumnInfo(name = "damage_json") val damageJson: String?,
    @ColumnInfo(name = "range_json") val rangeJson: String?,
    @ColumnInfo(name = "properties_json") val propertiesJson: String?,
    @ColumnInfo(name = "contents_json") val contentsJson: String?
)

@Entity(tableName = "weapons", indices = [Index(value = ["index_name"], unique = true)])
data class WeaponEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    @ColumnInfo(name = "label") val name: String,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    @ColumnInfo(name = "damage_dice") val damage: String?,
    @ColumnInfo(name = "damage_type") val damageType: String?,
    val cost: String?,
    val weight: Double?,
    @ColumnInfo(name = "properties_json") val propertiesJson: String?,
    val rarity: String?
)

@Entity(tableName = "armor", indices = [Index(value = ["index_name"], unique = true)])
data class ArmorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    @ColumnInfo(name = "ac_base") val acBase: Int?,
    @ColumnInfo(name = "dex_bonus") val dexBonus: Int?,
    @ColumnInfo(name = "max_bonus") val maxBonus: Int?,
    @ColumnInfo(name = "str_minimum") val strMinimum: Int?,
    @ColumnInfo(name = "stealth_disadvantage") val stealthDisadvantage: Int?,
    val cost: String?,
    val weight: Double?,
    val rarity: String?
)

@Entity(tableName = "magic_items", indices = [Index(value = ["index_name"], unique = true)])
data class MagicItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "category_index") val categoryIndex: String?,
    val rarity: String?,
    val variant: Int?,
    @ColumnInfo(name = "variants_json") val variantsJson: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "reference_json") val referenceJson: String?
)

@Entity(tableName = "weapon_properties", indices = [Index(value = ["index_name"], unique = true)])
data class WeaponPropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String
)

@Entity(tableName = "proficiencies", indices = [Index(value = ["index_name"], unique = true)])
data class ProficiencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val type: String?,
    val name: String,
    @ColumnInfo(name = "reference_json") val referenceJson: String?,
    @ColumnInfo(name = "classes_json") val classesJson: String?,
    @ColumnInfo(name = "races_json") val racesJson: String?
)

@Entity(tableName = "skills", indices = [Index(value = ["index_name"], unique = true)])
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "ability_score_index") val abilityScoreIndex: String?
)

@Entity(tableName = "languages", indices = [Index(value = ["index_name"], unique = true)])
data class LanguageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val type: String?,
    val script: String?,
    val description: String?,
    @ColumnInfo(name = "typical_speakers_json") val typicalSpeakersJson: String?
)

@Entity(tableName = "damage_types", indices = [Index(value = ["index_name"], unique = true)])
data class DamageTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String,
    val description: String?
)

@Entity(tableName = "equipment_categories", indices = [Index(value = ["index_name"], unique = true)])
data class EquipmentCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "index_name") val indexName: String,
    val name: String?
)

@Entity(tableName = "equipment_category_links", indices = [Index(value = ["category_index", "item_index"], unique = true)])
data class EquipmentCategoryLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "category_index") val categoryIndex: String,
    @ColumnInfo(name = "item_index") val itemIndex: String
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/ReferenceEntities.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/domain/model/CharacterModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterDomain(
    val id: Long = 0,
    val name: String = "",
    val raceName: String = "",
    val className: String = "",
    val level: Int = 1,
    val stats: Stats = Stats(),
    val hpCurrent: Int = 10,
    val hpMax: Int = 10,
    val speed: Int = 30,
    val inventoryIds: List<Int> = emptyList(),
    val spellsKnownIds: List<Int> = emptyList(),
    val raceSpellIds: List<String> = emptyList(),
    val features: List<Feature> = emptyList(),
    val bio: Bio = Bio(),
    val skillProficiencies: Map<String, Int> = emptyMap()
)

@Serializable
data class Stats(
    val strength: Int = 10, val dexterity: Int = 10, val constitution: Int = 10,
    val intelligence: Int = 10, val wisdom: Int = 10, val charisma: Int = 10,
    val copper: Int = 0, val silver: Int = 0, val gold: Int = 0
)

@Serializable
data class Bio(
    val alignment: String = "",
    val background: String = "",
    val backgroundName: String = "",
    val traits: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val notes: String = ""
)

@Serializable
data class Race(
    val id: Int,
    val index: String,
    val name: String,
    val description: String?,
    val age: String?,
    val alignment: String?,
    val sizeDesc: String?,
    val languagesDesc: String?,
    val speed: Int,
    val baseStats: Map<String, Int>,
    val subraces: List<Subrace> = emptyList()
)

@Serializable
data class Subrace(
    val id: Int, val index: String, val name: String,
    val description: String?, val bonusStats: Map<String, Int>
)

@Serializable
data class ClassInfo(
    val id: Int,
    val index: String,
    val name: String,
    val hitDie: Int,
    val subclasses: List<SubclassInfo> = emptyList() // ДОБАВЛЕНО: Связь с подклассами
)

@Serializable
data class SubclassInfo(
    val index: String,
    val name: String,
    val flavor: String,
    val description: String
)

@Serializable
data class Background(
    val id: Int,
    val name: String,
    val features: List<Feature>,
    val personalityTraits: List<String> = emptyList(),
    val ideals: List<String> = emptyList(),
    val bonds: List<String> = emptyList(),
    val flaws: List<String> = emptyList()
)

@Serializable
data class Spell(
    val id: Int, val index: String, val name: String, val level: Int,
    val school: String, val castingTime: String, val range: String,
    val components: String, val duration: String, val description: String,
    val isConcentration: Boolean, val isRitual: Boolean
)

@Serializable
data class Weapon(
    val id: Int, val name: String, val damage: String, val damageType: String,
    val cost: String, val weight: String, val properties: String
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/CharacterModels.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/di/DatabaseModule.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.di

import android.content.Context
import androidx.room.Room
import com.dnd.app.data.local.AppDatabase
import com.dnd.app.data.local.ReferenceDatabase
import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.dao.ReferenceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // ВАЖНО: Новое имя файла для v1.23
    private const val REFERENCE_DB_NAME = "dnd_v2.db"
    private const val ASSET_DB_PATH = "database/dnd_clean.db"

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "dnd_app_user.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideReferenceDatabase(@ApplicationContext context: Context): ReferenceDatabase {
        return Room.databaseBuilder(
            context,
            ReferenceDatabase::class.java,
            REFERENCE_DB_NAME
        )
            .createFromAsset(ASSET_DB_PATH)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideReferenceDao(db: ReferenceDatabase): ReferenceDao {
        return db.referenceDao()
    }

    @Provides
    fun provideCharacterDao(db: AppDatabase): CharacterDao {
        return db.characterDao()
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/di/DatabaseModule.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/StatsStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.rules.DndRules

@Composable
fun StatsStep(
    scores: Map<String, Int>,         // Купленные очки (8-15)
    staticBonuses: Map<String, Int>,  // Врожденные (Дварф +2)
    dynamicBonuses: Map<String, Int>, // Выбранные (Полуэльф +1)
    onStatChange: (String, Int) -> Unit
) {
    val spent = scores.values.sumOf { DndRules.getPointCost(it) }
    val remaining = DndRules.MAX_POINTS - spent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Табло очков (Point Buy)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF424242), RoundedCornerShape(4.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Осталось очков", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "$remaining",
                    color = if (remaining >= 0) Color.White else Color.Red,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Сетка характеристик 2x3 (Квадратные блоки)
        val stats = listOf(
            "Сила" to "STR", "Ловкость" to "DEX",
            "Телосложение" to "CON", "Интеллект" to "INT",
            "Мудрость" to "WIS", "Харизма" to "CHA"
        )

        stats.chunked(2).forEach { rowStats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowStats.forEach { (name, key) ->
                    StatSquareItem(
                        modifier = Modifier.weight(1f),
                        name = name,
                        boughtValue = scores[key] ?: 8,
                        staticBonus = staticBonuses[key] ?: 0,
                        dynamicBonus = dynamicBonuses[key] ?: 0,
                        onValueChange = { diff -> onStatChange(key, diff) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun StatSquareItem(
    modifier: Modifier,
    name: String,
    boughtValue: Int,   // То, что накликано (8-15)
    staticBonus: Int,   // От расы
    dynamicBonus: Int,  // От выбора (Полуэльф)
    onValueChange: (Int) -> Unit
) {
    val totalValue = boughtValue + staticBonus + dynamicBonus
    val modifierValue = (totalValue - 10) / 2
    val modSign = if (modifierValue >= 0) "+" else ""

    // Формула детализации
    val pointsAboveBase = boughtValue - 8
    val racePart = staticBonus + dynamicBonus

    Column(
        modifier = modifier
            .aspectRatio(1f) // Делаем блок строго квадратным
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)

        // Большой Модификатор
        Text(
            text = "$modSign$modifierValue",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black
        )

        // Итоговое значение и детализация
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$totalValue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Text(
                text = "8 + $pointsAboveBase + $racePart",
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        // Кнопки управления (внизу квадрата)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(2.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onValueChange(-1) },
                contentAlignment = Alignment.Center
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.LightGray)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onValueChange(1) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/StatsStep.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/CharacterAssembler.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.*
import com.dnd.app.domain.repository.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterAssembler @Inject constructor(
    private val repository: LibraryRepository,
    private val calculator: DndCalculator
) {
    /**
     * Собирает финальный объект CharacterDomain из черновика.
     * Реализация уровня Senior: использует предварительно рассчитанные бонусы из Draft,
     * обеспечивая принцип "What You See Is What You Get" (WYSIWYG).
     */
    suspend fun assemble(draft: DraftCharacter): CharacterDomain {
        val finalStatsMap = mutableMapOf<String, Int>()
        val skillProficiencies = mutableMapOf<String, Int>()
        val autoLearnedSpells = mutableListOf<String>()
        val features = mutableListOf<Feature>()

        var baseSpeed = 30
        var extraHp = 0

        // 1. БАЗОВЫЕ ХАРАКТЕРИСТИКИ + СТАТИЧЕСКИЕ БОНУСЫ (Раса + Подраса)
        // Мы берем значения, которые пользователь "натыкал" (8-15)
        // и прибавляем к ним staticRaceBonuses, которые ViewModel подготовила заранее.
        draft.baseInfo.baseAbilityScores.forEach { (stat, value) ->
            val staticBonus = draft.baseInfo.staticRaceBonuses[stat] ?: 0
            finalStatsMap[stat] = value + staticBonus
        }

        // 2. ПРИМЕНЕНИЕ ВЫБОРОВ ( raceSelections + levelStack.selections )
        // Здесь учитываются "плавающие" бонусы (например, +1 к любой стате у полуэльфа)
        val allSelections = mutableListOf<ChoiceResult>()
        allSelections.addAll(draft.baseInfo.raceSelections.values)
        draft.levelStack.forEach { allSelections.addAll(it.selections.values) }

        allSelections.forEach { result ->
            applyChoiceResult(result, finalStatsMap, skillProficiencies, autoLearnedSpells)
        }

        // 3. СБОРКА СПОСОБНОСТЕЙ И ЛОГИКИ ПРАВИЛ
        // Нам все еще нужны объекты фич для определения спец-логики (типа скорости или доп. ХП)
        val race = repository.getAllParentRaces().find { it.index == draft.baseInfo.raceIndex }
        if (race != null) {
            baseSpeed = race.speed
            val raceFeatures = repository.getRaceFeatures(race.id, draft.baseInfo.subraceIndex)
            features.addAll(raceFeatures)

            raceFeatures.forEach { feat ->
                if (feat.changeRule) {
                    when(feat.index) {
                        "fleet-of-foot" -> baseSpeed = 35
                        "dwarven-toughness" -> extraHp += draft.levelStack.size.coerceAtLeast(1)
                    }
                }
            }
        }

        // 4. КЛАССОВАЯ ПРОГРЕССИЯ
        val conModInitial = calculator.calculateModifier(finalStatsMap["CON"] ?: 10)
        var hpMax = extraHp

        draft.levelStack.forEachIndexed { index, step ->
            val lvl = index + 1
            val classInfo = repository.getAllClasses().find { it.index == step.classIndex }

            // Расчет ХП
            if (index == 0) hpMax += (classInfo?.hitDie ?: 8) + conModInitial
            else hpMax += step.hpIncrease + conModInitial

            // Фичи класса
            features.addAll(repository.getProgressionFeatures(step.classIndex, lvl, step.subclassIndex))
        }

        val classString = draft.levelStack.groupBy { it.classIndex }
            .map { (idx, list) -> "${idx.replaceFirstChar { it.uppercase() }} ${list.size}" }
            .joinToString(" / ")

        return CharacterDomain(
            id = draft.id,
            name = draft.name.ifBlank { "Герой" },
            raceName = race?.name ?: "",
            className = classString,
            level = draft.levelStack.size.coerceAtLeast(1),
            stats = Stats(
                strength = finalStatsMap["STR"] ?: 10,
                dexterity = finalStatsMap["DEX"] ?: 10,
                constitution = finalStatsMap["CON"] ?: 10,
                intelligence = finalStatsMap["INT"] ?: 10,
                wisdom = finalStatsMap["WIS"] ?: 10,
                charisma = finalStatsMap["CHA"] ?: 10
            ),
            hpMax = hpMax, hpCurrent = hpMax,
            speed = baseSpeed,
            features = features,
            raceSpellIds = autoLearnedSpells.distinct(),
            skillProficiencies = skillProficiencies,
            bio = Bio(
                alignment = draft.baseInfo.alignmentIndex,
                traits = draft.baseInfo.personalityTrait,
                ideals = draft.baseInfo.ideal,
                bonds = draft.baseInfo.bond,
                flaws = draft.baseInfo.flaw
            )
        )
    }

    private fun applyChoiceResult(
        result: ChoiceResult,
        stats: MutableMap<String, Int>,
        skills: MutableMap<String, Int>,
        spells: MutableList<String>
    ) {
        when (result) {
            is ChoiceResult.StatBonus -> result.bonuses.forEach { (s, b) ->
                val key = s.take(3).uppercase()
                stats[key] = (stats[key] ?: 0) + b
            }
            is ChoiceResult.Skills -> result.skillIndexes.forEach { skills[it] = 1 }
            is ChoiceResult.Spells -> spells.addAll(result.spellIndexes)
            is ChoiceResult.SelectedOptions -> {
                result.items.forEach { if (it.contains("skill-")) skills[it] = 1 }
            }
            else -> {}
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/CharacterAssembler.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/RaceStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.*
import com.dnd.app.ui.screens.character_creator.components.*
import com.dnd.app.util.stripHtml

@Composable
fun RaceStep(
    availableRaces: List<Race>,
    selectedRaceIndex: String,
    onRaceSelect: (String) -> Unit,
    availableSubraces: List<Race>,
    selectedSubraceIndex: String?,
    onSubraceSelect: (String) -> Unit,
    features: List<Feature>,
    currentSelections: Map<String, ChoiceResult>,
    onSelectionChanged: (String, ChoiceResult) -> Unit,
    globalExclusions: Set<String>
) {
    // Сохраняем состояние скролла между рекомпозициями
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. СЕЛЕКТОР ОСНОВНОЙ РАСЫ
        item(key = "main_race_dropdown_selector") {
            FlatWizardSection(title = "Раса") {
                val options = availableRaces.map { ChoiceOption(it.index, it.name) }
                SmartDropdown(options, selectedRaceIndex, onSelected = { onRaceSelect(it.id) })
            }
        }

        // 2. ДИНАМИЧЕСКИЙ СПИСОК ФИЧ
        // Ключ включает ID фичи, что гарантирует стабильность позиции существующих элементов
        items(features, key = { "feat_${it.index}_${it.id}" }) { feature ->
            if (feature.isSubraceSelector) {
                // СЕЛЕКТОР ПОДРАСЫ: Появляется ровно там, где его поставил репозиторий
                FlatWizardSection(title = feature.name) {
                    Column {
                        if (feature.description.isNotBlank()) {
                            Text(
                                text = feature.description,
                                fontSize = 13.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        val options = availableSubraces.map { ChoiceOption(it.index, it.name) }
                        SmartDropdown(
                            options = options,
                            selectedId = selectedSubraceIndex,
                            onSelected = { onSubraceSelect(it.id) },
                            placeholder = "Выберите разновидность..."
                        )
                    }
                }
            } else {
                // ОБЫЧНАЯ ФИЧА
                FlatWizardSection(title = feature.name) {
                    Column {
                        if (feature.description.isNotBlank()) {
                            Text(
                                text = feature.description.stripHtml(),
                                fontSize = 14.sp,
                                color = Color.Black,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(bottom = if (feature.choices.isNotEmpty() || feature.embeddedSpells.isNotEmpty()) 8.dp else 0.dp)
                            )
                        }

                        feature.embeddedSpells.forEach { EmbeddedSpellRow(it) }

                        feature.choices.forEach { choice ->
                            FeatureChoiceBlock(
                                choice = choice,
                                currentSelection = currentSelections[feature.index],
                                onSelectionChanged = { res -> onSelectionChanged(feature.index, res) },
                                globalExclusions = globalExclusions
                            )
                        }
                    }
                }
            }
        }

        item(key = "race_bottom_padding") { Spacer(Modifier.height(32.dp)) }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/RaceStep.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/domain/repository/LibraryRepository.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.repository

import com.dnd.app.data.local.entity.AlignmentEntity
import com.dnd.app.data.local.entity.ArmorEntity
import com.dnd.app.data.local.entity.EquipmentEntity
import com.dnd.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    // Classes
    suspend fun getAllClasses(): List<ClassInfo>
    suspend fun getSubclassesForClass(classIndex: String): List<SubclassInfo>
    suspend fun getProgressionFeatures(classIndex: String, level: Int, subclassIndex: String? = null): List<Feature>
    suspend fun getClassSkillOptions(classId: Int): Pair<Int, List<String>>

    // Bio & Races
    suspend fun getAllParentRaces(): List<Race>
    suspend fun getSubracesFromDb(parentId: Int): List<Race>
    suspend fun getRaceFeatures(raceId: Int, subraceName: String?): List<Feature>
    suspend fun getAllBackgrounds(): List<Background>
    suspend fun getAllAlignments(): List<AlignmentEntity>

    // Spells & Equipment
    fun getAllSpells(): Flow<List<Spell>>
    suspend fun getSpellsByIds(ids: List<Int>): List<Spell>
    fun getAllWeapons(): Flow<List<Weapon>>
    suspend fun getWeaponsByIds(ids: List<Int>): List<Weapon>
    fun getAllArmor(): Flow<List<ArmorEntity>>
    suspend fun searchEquipment(query: String): List<EquipmentEntity>

    // Features & Utils
    suspend fun getFeatureById(id: Int): Feature?
    suspend fun getFeatureByName(name: String): Feature?
    suspend fun getEquipmentIdsByNames(idxNames: List<String>): List<Int>
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/repository/LibraryRepository.kt
```
------
```kotlin
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
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/domain/calculator/DndCalculator.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.calculator

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor

@Singleton
class DndCalculator @Inject constructor() {

    fun calculateModifier(score: Int): Int {
        return floor((score - 10) / 2.0).toInt()
    }

    fun calculateProficiencyBonus(totalLevel: Int): Int {
        return when {
            totalLevel >= 17 -> 6
            totalLevel >= 13 -> 5
            totalLevel >= 9 -> 4
            totalLevel >= 5 -> 3
            else -> 2
        }
    }

    fun formatModifier(mod: Int): String {
        return if (mod >= 0) "+$mod" else "$mod"
    }

    fun calculateSkillBonus(score: Int, profBonus: Int, multiplier: Int): Int {
        val mod = calculateModifier(score)
        return mod + (profBonus * multiplier)
    }

    // ВОССТАНОВЛЕННЫЙ МЕТОД
    fun calculateBaseAC(dexModifier: Int): Int {
        // Базовая логика 10 + ловкость.
        // Для брони нужно будет расширять логику, но пока так.
        return 10 + dexModifier
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/calculator/DndCalculator.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/model/DataJsonModels.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.model

import kotlinx.serialization.Serializable

/**
 * Описывает структуру элемента из массива granted_spell в JSON.
 */
@Serializable
data class GrantedSpellJson(
    val id: Int,
    val level: Int
)

/**
 * Описывает выбор навыков классом (skill_choices_json).
 */
@Serializable
data class ClassSkillsJson(
    val choices: List<String>
)

// FeatureChoiceJson и FeatureJsonModel УДАЛЕНЫ, так как перенесены в FeatureJsonModels.kt
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/model/DataJsonModels.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/InventoryStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InventoryStep(
    className: String?,
    backgroundName: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Стартовое снаряжение", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Ваше снаряжение определяется выбранным классом и предысторией.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Блок класса
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("От класса: ${className ?: "Не выбран"}", fontWeight = FontWeight.Bold)
                if (className != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("• Стандартный набор экипировки для $className (будет добавлено в инвентарь).", fontSize = 14.sp)
                    // В будущем здесь будет парсинг JSON с вариантами (А или Б)
                }
            }
        }

        // Блок предыстории
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("От предыстории: ${backgroundName ?: "Не выбрана"}", fontWeight = FontWeight.Bold)
                if (backgroundName != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("• Набор одежды", fontSize = 14.sp)
                    Text("• Кошель с монетами", fontSize = 14.sp)
                    Text("• Профессиональный инструмент (если есть)", fontSize = 14.sp)
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/InventoryStep.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/repository/CharacterRepositoryImpl.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.repository

import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.entity.CharacterEntity
import com.dnd.app.domain.model.Bio
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.model.DraftCharacter
import com.dnd.app.domain.model.Stats
import com.dnd.app.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CharacterRepositoryImpl @Inject constructor(
    private val dao: CharacterDao
) : CharacterRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllCharacters(): Flow<List<CharacterDomain>> {
        return dao.getAllCharacters().map { entities ->
            entities.map { mapEntityToDomain(it) }
        }
    }

    override suspend fun getCharacterById(id: Long): CharacterDomain? {
        val entity = dao.getCharacterById(id) ?: return null
        return mapEntityToDomain(entity)
    }

    /**
     * Новый метод: Получение черновика для редактирования.
     * Если черновика нет в базе (старый персонаж), мы должны попытаться восстановить его из домена,
     * но пока возвращаем пустой (это повод для миграции данных в будущем).
     */
    suspend fun getDraftById(id: Long): DraftCharacter? {
        return dao.getCharacterById(id)?.draftData
    }

    override suspend fun saveCharacter(character: CharacterDomain): Long {
        // ВНИМАНИЕ: Этот метод теперь сохраняет только SNAPSHOT.
        // DraftCharacter должен сохраняться отдельно или передаваться сюда же,
        // но в текущей архитектуре Assembler возвращает Domain, теряя Draft.
        // Эту логику мы поправим в ViewModel: она будет сохранять Entity, имея на руках И Draft И Domain.

        // Временная заглушка, чтобы код компилировался.
        // Реальное сохранение происходит через ViewModel, которая собирает Entity вручную.
        val entity = mapDomainToEntity(character, null)
        return dao.insertCharacter(entity)
    }

    suspend fun saveFullCharacter(domain: CharacterDomain, draft: DraftCharacter) {
        val entity = mapDomainToEntity(domain, draft)
        if (domain.id == 0L) {
            dao.insertCharacter(entity)
        } else {
            dao.updateCharacter(entity)
        }
    }

    override suspend fun deleteCharacter(characterId: Long) {
        // Для удаления Room требует Entity, но ему достаточно только ID, если он помечен @PrimaryKey
        // Однако, безопаснее сделать Query в DAO: DELETE FROM characters WHERE id = :id
        // Пока используем старый метод:
        val dummy = dao.getCharacterById(characterId)
        if (dummy != null) dao.deleteCharacter(dummy)
    }

    private fun mapEntityToDomain(entity: CharacterEntity): CharacterDomain {
        return try {
            CharacterDomain(
                id = entity.id,
                name = entity.name,
                raceName = entity.raceName,
                className = entity.className,
                level = entity.level,
                hpCurrent = entity.hpCurrent,
                hpMax = entity.hpMax,
                stats = try { json.decodeFromString<Stats>(entity.statsJson) } catch (e: Exception) { Stats() },
                inventoryIds = try { json.decodeFromString<List<Int>>(entity.inventoryIdsJson) } catch (e: Exception) { emptyList() },
                spellsKnownIds = try { json.decodeFromString<List<Int>>(entity.spellsKnownIdsJson) } catch (e: Exception) { emptyList() },
                bio = try { json.decodeFromString<Bio>(entity.bioJson) } catch (e: Exception) { Bio() },
                skillProficiencies = try { json.decodeFromString<Map<String, Int>>(entity.skillProficienciesJson) } catch (e: Exception) { emptyMap() }
            )
        } catch (e: Exception) {
            CharacterDomain(id = entity.id, name = "Error Data")
        }
    }

    private fun mapDomainToEntity(domain: CharacterDomain, draft: DraftCharacter?): CharacterEntity {
        return CharacterEntity(
            id = domain.id,
            name = domain.name,
            raceName = domain.raceName,
            className = domain.className,
            level = domain.level,
            hpCurrent = domain.hpCurrent,
            hpMax = domain.hpMax,
            statsJson = json.encodeToString(domain.stats),
            inventoryIdsJson = json.encodeToString(domain.inventoryIds),
            spellsKnownIdsJson = json.encodeToString(domain.spellsKnownIds),
            bioJson = json.encodeToString(domain.bio),
            skillProficienciesJson = json.encodeToString(domain.skillProficiencies),
            draftData = draft
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/repository/CharacterRepositoryImpl.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/local/AppDatabase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dnd.app.data.local.converters.DraftConverters
import com.dnd.app.data.local.dao.CharacterDao
import com.dnd.app.data.local.entity.CharacterEntity

@Database(
    entities = [CharacterEntity::class],
    version = 2, // Увеличиваем версию для миграции
    exportSchema = false
)
@TypeConverters(DraftConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/AppDatabase.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/CharacterEntity.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dnd.app.domain.model.DraftCharacter

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,

    // Поля "Кэша" (для отображения в списке без десериализации всего)
    @ColumnInfo(name = "race_name") val raceName: String = "",
    @ColumnInfo(name = "class_name") val className: String = "",
    @ColumnInfo(name = "level") val level: Int = 1,
    @ColumnInfo(name = "hp_current") val hpCurrent: Int = 10,
    @ColumnInfo(name = "hp_max") val hpMax: Int = 10,

    // JSON-поля для готового листа (Domain Model)
    @ColumnInfo(name = "stats_json") val statsJson: String,
    @ColumnInfo(name = "inventory_ids_json") val inventoryIdsJson: String,
    @ColumnInfo(name = "spells_known_ids_json") val spellsKnownIdsJson: String,
    @ColumnInfo(name = "bio_json") val bioJson: String,
    @ColumnInfo(name = "skill_proficiencies_json") val skillProficienciesJson: String,

    /**
     * "Мастер-лента" (Master Tape).
     * Здесь хранится полная история создания персонажа (DraftCharacter).
     * При загрузке листа мы можем пересобрать (re-assemble) персонажа из этого поля,
     * если логика игры изменится.
     */
    @ColumnInfo(name = "draft_data")
    val draftData: DraftCharacter? = null
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/entity/CharacterEntity.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/local/converters/DraftConverters.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.converters

import androidx.room.TypeConverter
import com.dnd.app.domain.model.DraftCharacter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Конвертер для сохранения всего объекта DraftCharacter в одну колонку БД.
 * Это упрощает архитектуру: мы не нормализуем черновик, так как он временный и сложный.
 */
class DraftConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromDraft(draft: DraftCharacter?): String? {
        return draft?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toDraft(data: String?): DraftCharacter? {
        return data?.let {
            try {
                json.decodeFromString<DraftCharacter>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/converters/DraftConverters.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/LevelStep.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.model.ChoiceResult
import com.dnd.app.domain.model.Feature
import com.dnd.app.domain.model.LevelStep

@Composable
fun LevelStep(
    step: LevelStep,
    features: List<Feature>,
    onHpChange: (Int) -> Unit,
    onSelectionChange: (String, ChoiceResult) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Блок ХП
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Увеличение Здоровья (HP)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = if (step.hpIncrease == 0) "" else step.hpIncrease.toString(),
                            onValueChange = { onHpChange(it.toIntOrNull() ?: 0) },
                            modifier = Modifier.width(100.dp),
                            label = { Text("HP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Киньте кость хитов класса или возьмите среднее значение.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Блок Фич
        if (features.isEmpty()) {
            item {
                Text("На этом уровне нет новых способностей.", modifier = Modifier.padding(8.dp), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        } else {
            items(features) { feature ->
                FeatureCard(feature, step, onSelectionChange)
            }
        }
    }
}

@Composable
fun FeatureCard(
    feature: Feature,
    step: LevelStep,
    onSelectionChange: (String, ChoiceResult) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color.LightGray),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(feature.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            if (feature.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(feature.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            if (feature.choices.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Необходимо сделать выбор:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                feature.choices.forEach { _ ->
                    // Здесь будет сложный UI выбора (Dropdown, Checkboxes)
                    // Пока реализуем заглушку-кнопку для ТЗ
                    val isChosen = step.selections.containsKey(feature.index)

                    Button(
                        onClick = { onSelectionChange(feature.index, ChoiceResult.Note("Choice Made")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (isChosen) "Выбор сделан" else "Выбрать...")
                    }
                }
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_creator/tabs/LevelStep.kt
```
------
```kotlin
// Имя файла: [app/src/main/java/com/dnd/app/ui/screens/character_creator/components/WizardUiConfig.kt]
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_creator.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object WizardUiConfig {
    val SECTION_GAP: Dp = 8.dp
    val SECTION_PADDING: Dp = 0.dp
    val CONTENT_INNER_PADDING: Dp = 8.dp
    val SPELL_LIST_PADDING: Dp = 0.dp

    val BORDER_WIDTH: Dp = 1.dp
    val CORNER_RADIUS: Dp = 0.dp

    val COLOR_HEADER_BG = Color(0xFF424242)
    val COLOR_HEADER_TEXT = Color.White
    val COLOR_CONTENT_BG = Color(0xFFC0C0C0)
    val COLOR_BORDER = Color(0xFF424242)
    val COLOR_DIVIDER = Color(0x4D000000)

    val FONT_SIZE_HEADER = 16.sp
    val FONT_SIZE_CONTENT = 14.sp
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: [app/src/main/java/com/dnd/app/ui/screens/character_creator/components/WizardUiConfig.kt]
```
------
```kotlin
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
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/GetStructuredRacesUseCase.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.usecase

import com.dnd.app.domain.model.ParentRace
import com.dnd.app.domain.repository.LibraryRepository
import javax.inject.Inject

class GetStructuredRacesUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(): List<ParentRace> {
        val structuredRaces = mutableListOf<ParentRace>()
        val parentRaces = repository.getAllParentRaces()

        for (parent in parentRaces) {
            val dbSubraces = repository.getSubracesFromDb(parent.id)

            // ИСПРАВЛЕНИЕ ДЛЯ ДРАКОНОРОЖДЕННОГО:
            // Мы берем ТОЛЬКО реальные подрасы из БД.
            // Если dbSubraces пусто (как у Дракона), то subraceOptions будет пустым.
            // UI не покажет дропдаун подрас, и выбор цвета Дракона останется внутри карточки фичи.
            val subraceOptions = dbSubraces.map { it.name }

            structuredRaces.add(ParentRace(parent.id, parent.name, subraceOptions))
        }

        return structuredRaces.sortedBy { it.name }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/usecase/GetStructuredRacesUseCase.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/domain/model/RaceMapping.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

/**
 * СЛОВАРЬ СООТВЕТСТВИЯ (MAPPING LAYER)
 */
object RaceMapping {

    // БЕЛЫЙ СПИСОК: Строгое совпадение с именами в таблице `races`
    val PRIMARY_RACES = setOf(
        "Dragonborn",
        "Dwarf",
        "Elf",
        "Gnome",
        "Half-Elf", // С дефисом!
        "Half-Orc", // С дефисом!
        "Halfling",
        "Human",
        "Tiefling"
    )

    // Словарь: "Название в UI" -> "Name в БД (Техническое)"
    val subraceMap: Map<String, String> = mapOf(
        // Дварфы
        "Горный дварф" to "RockDwarf",
        "Холмовой дварф" to "HillDwarf",

        // Эльфы
        "Высший эльф" to "HighElf",
        "Лесной эльф" to "ForestElf",
        "Дроу" to "Drow",
        "Тёмный эльф (Дроу)" to "Drow",

        // Гномы
        "Лесной гном" to "ForestGnome",
        "Скальный гном" to "RockGnome",

        // Полурослики
        "Легконогий" to "LightLeged", // Как в базе (ID 14)
        "Коренастый" to "Stocky",     // Как в базе (ID 17)

        // Люди
        "Альтернативный" to "HumanAlt",
        "Классический" to "HumanClassic",
        "Альтернативный человек" to "HumanAlt",
        "Классический человек" to "HumanClassic"
    )

    fun getDbName(uiName: String): String? {
        // Сначала ищем точное совпадение
        if (subraceMap.containsKey(uiName)) return subraceMap[uiName]

        // Если не нашли, ищем частичное (например "Белый (Холод...)" не найдет, и это нормально для Драконов)
        return subraceMap.entries.find { uiName.contains(it.key, ignoreCase = true) }?.value
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/RaceMapping.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/domain/model/ParentRace.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.model

/**
 * Структурированная раса для UI.
 * @param id ID родительской расы (например, Дварф)
 * @param name Имя родительской расы
 * @param subraces Список имен подрас, доступных для выбора (например, [Горный дварф, Холмовой дварф])
 */
data class ParentRace(
    val id: Int,
    val name: String,
    val subraces: List<String>
)
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/domain/model/ParentRace.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/components/CommonUi.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnd.app.ui.theme.DndPrimary

// Старая TopBar (можно оставить или удалить, если не используется)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndTopBar(
    title: String,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit = {},
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White),
        navigationIcon = { if (canNavigateBack) IconButton(onClick = navigateUp) { Icon(Icons.Filled.ArrowBack, "Back") } },
        actions = { actions() }
    )
}

@Composable
fun DndActionTopBar(
    title: String,
    onBack: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    actionIcon: @Composable (() -> Unit)? = null,
    isActionEnabled: Boolean = true,
    // НОВОЕ: Для выбора уровня в Создателе
    level: Int = 0,
    onLevelChange: ((Int) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(DndPrimary).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Назад
        Box(
            modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(4.dp)).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.Black, modifier = Modifier.size(32.dp))
        }

        // Заголовок
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )

        // ВЫБОР УРОВНЯ (Если передан level > 0)
        if (level > 0 && onLevelChange != null) {
            LevelSelector(level, onLevelChange)
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Кнопка действия (Сохранить)
        if (onActionClick != null && actionIcon != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isActionEnabled) Color.White else Color.Gray, RoundedCornerShape(4.dp))
                    .clickable(enabled = isActionEnabled, onClick = onActionClick),
                contentAlignment = Alignment.Center
            ) {
                actionIcon()
            }
        }
    }
}

@Composable
fun LevelSelector(level: Int, onLevelChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(48.dp)
            .background(Color.White, RoundedCornerShape(4.dp))
            .clickable { expanded = true }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ур. $level", fontWeight = FontWeight.Bold, color = Color.Black)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Black)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (i in 1..20) {
                DropdownMenuItem(
                    text = { Text("Уровень $i") },
                    onClick = { onLevelChange(i); expanded = false }
                )
            }
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/components/CommonUi.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/util/Extensions.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.util

import java.util.Locale

fun String.stripHtml(): String {
    return this.replace(Regex("<.*?>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
}

fun String.capitalizeFirst(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/util/Extensions.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_sheet/CharacterSheetViewModel.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.model.Spell
import com.dnd.app.domain.model.Stats
import com.dnd.app.domain.model.Weapon
import com.dnd.app.domain.repository.LibraryRepository
import com.dnd.app.domain.usecase.CharacterUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CharacterSheetState(
    val character: CharacterDomain? = null,
    val weapons: List<Weapon> = emptyList(),
    val spells: List<Spell> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CharacterSheetViewModel @Inject constructor(
    private val useCases: CharacterUseCases,
    private val libraryRepository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
    val calculator: DndCalculator
) : ViewModel() {

    private val characterId: Long = checkNotNull(savedStateHandle["characterId"])

    private val _state = MutableStateFlow(CharacterSheetState())
    val state = _state.asStateFlow()

    init {
        loadCharacter()
    }

    private fun loadCharacter() {
        viewModelScope.launch {
            val char = useCases.getCharacter(characterId)
            if (char != null) {
                val loadedWeapons = libraryRepository.getWeaponsByIds(char.inventoryIds)
                val loadedSpells = libraryRepository.getSpellsByIds(char.spellsKnownIds)

                _state.value = _state.value.copy(
                    character = char,
                    weapons = loadedWeapons,
                    spells = loadedSpells,
                    isLoading = false
                )
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun updateHp(change: Int) {
        val currentChar = _state.value.character ?: return
        val newHp = (currentChar.hpCurrent + change).coerceIn(0, currentChar.hpMax)

        saveCharacter(currentChar.copy(hpCurrent = newHp))
    }

    fun updateMoney(type: String, delta: Int) {
        val char = _state.value.character ?: return
        var s = char.stats

        // Логика математики монет
        if (delta > 0) {
            // Простое добавление
            s = when(type) {
                "CP" -> s.copy(copper = s.copper + delta)
                "SP" -> s.copy(silver = s.silver + delta)
                "GP" -> s.copy(gold = s.gold + delta)
                else -> s
            }
        } else {
            // Вычитание с разменом
            s = subtractMoneyRecursive(s, type, -delta)
        }

        saveCharacter(char.copy(stats = s))
    }

    private fun subtractMoneyRecursive(stats: Stats, type: String, amount: Int): Stats {
        var currentStats = stats

        when (type) {
            "CP" -> {
                if (currentStats.copper >= amount) {
                    currentStats = currentStats.copy(copper = currentStats.copper - amount)
                } else {
                    // Не хватает меди, пробуем взять у серебра
                    // Исправлено: удалена неиспользуемая переменная needed
                    if (currentStats.silver > 0) {
                        // Размен 1 СМ -> 10 ММ
                        currentStats = currentStats.copy(silver = currentStats.silver - 1, copper = currentStats.copper + 10)
                        // Рекурсивная попытка снова снять
                        return subtractMoneyRecursive(currentStats, "CP", amount)
                    } else if (currentStats.gold > 0) {
                        // Размен 1 ЗМ -> 10 СМ (а потом следующий шаг разменяет СМ на ММ)
                        currentStats = currentStats.copy(gold = currentStats.gold - 1, silver = currentStats.silver + 10)
                        return subtractMoneyRecursive(currentStats, "CP", amount)
                    }
                }
            }
            "SP" -> {
                if (currentStats.silver >= amount) {
                    currentStats = currentStats.copy(silver = currentStats.silver - amount)
                } else {
                    if (currentStats.gold > 0) {
                        // Размен 1 ЗМ -> 10 СМ
                        currentStats = currentStats.copy(gold = currentStats.gold - 1, silver = currentStats.silver + 10)
                        return subtractMoneyRecursive(currentStats, "SP", amount)
                    }
                }
            }
            "GP" -> {
                if (currentStats.gold >= amount) {
                    currentStats = currentStats.copy(gold = currentStats.gold - amount)
                }
            }
        }
        return currentStats
    }

    private fun saveCharacter(character: CharacterDomain) {
        _state.value = _state.value.copy(character = character)
        viewModelScope.launch {
            useCases.saveCharacter(character)
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/ui/screens/character_sheet/CharacterSheetViewModel.kt
```
------
```kotlin
// Имя файла: app/src/main/java/com/dnd/app/data/local/dao/CharacterDao.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dnd.app.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY id DESC")
    fun getAllCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: Long): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/data/local/dao/CharacterDao.kt
```
------
```kotlin
// Имя файла: domain/rules/DndRules.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.domain.rules

object DndRules {
    const val MAX_POINTS = 27
    const val MIN_SCORE = 8
    const val MAX_SCORE = 15
    val pointCost = mapOf(8 to 0, 9 to 1, 10 to 2, 11 to 3, 12 to 4, 13 to 5, 14 to 7, 15 to 9)
    fun getPointCost(score: Int): Int = pointCost[score] ?: 99
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: domain/rules/DndRules.kt
```
------
```kotlin
// Имя файла: ui/navigation/NavGraph.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
// DndCalculator больше не нужен здесь
import com.dnd.app.ui.screens.character_creator.CharacterCreatorScreen
import com.dnd.app.ui.screens.character_list.CharacterListScreen
import com.dnd.app.ui.screens.character_sheet.CharacterSheetScreen

@Composable
fun DndNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CharacterList.route,
        modifier = modifier
    ) {
        composable(route = Screen.CharacterList.route) {
            CharacterListScreen(
                onNavigateToCreate = { navController.navigate(Screen.CharacterCreator.route) },
                onNavigateToSheet = { id -> navController.navigate(Screen.CharacterSheet.createRoute(id)) }
            )
        }

        composable(route = Screen.CharacterCreator.route) {
            CharacterCreatorScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.CharacterSheet.route,
            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
        ) {
            CharacterSheetScreen(
                navigateUp = { navController.navigateUp() }
            )
        }
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/navigation/NavGraph.kt
```
------
```kotlin
// Имя файла: ui/screens/character_sheet/tabs/SkillsTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.CharacterDomain

// Цвета
private val ColorProficientBg = Color(0xFFC8E6C9) // Светло-зеленый
private val ColorExpertiseBg = Color(0xFFF8BBD0)  // Светло-розовый
private val ColorNoneBg = Color(0xFFFFFFFF)       // Белый
private val ColorHeaderBg = Color(0xFFE0E0E0)     // Серый заголовок

@Composable
fun SkillsTab(
    character: CharacterDomain,
    calculator: DndCalculator
) {
    val profBonus = calculator.calculateProficiencyBonus(character.level)
    val stats = character.stats
    val skillsMap = character.skillProficiencies

    // Данные для колонок
    val strSkills = listOf("Атлетика")
    val dexSkills = listOf("Акробатика", "Ловкость рук", "Скрытность")
    val conSkills = emptyList<String>()
    val intSkills = listOf("Анализ", "История", "Магия", "Природа", "Религия")

    val wisSkills = listOf("Внимательность", "Выживание", "Медицина", "Проницательность", "Уход за животными")
    val chaSkills = listOf("Выступление", "Запугивание", "Обман", "Убеждение")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // --- ЛЕВАЯ КОЛОНКА ---
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AbilitySkillsCard("Сила", stats.strength, strSkills, skillsMap, profBonus, calculator)
            AbilitySkillsCard("Ловкость", stats.dexterity, dexSkills, skillsMap, profBonus, calculator)
            AbilitySkillsCard("Телосложение", stats.constitution, conSkills, skillsMap, profBonus, calculator)
            AbilitySkillsCard("Интеллект", stats.intelligence, intSkills, skillsMap, profBonus, calculator)
        }

        // --- ПРАВАЯ КОЛОНКА ---
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AbilitySkillsCard("Мудрость", stats.wisdom, wisSkills, skillsMap, profBonus, calculator)
            AbilitySkillsCard("Харизма", stats.charisma, chaSkills, skillsMap, profBonus, calculator)
        }
    }
}

@Composable
fun AbilitySkillsCard(
    abilityName: String,
    abilityScore: Int,
    skillNames: List<String>,
    proficiencies: Map<String, Int>,
    profBonus: Int,
    calculator: DndCalculator
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.LightGray)
            .background(Color.White)
    ) {
        // Заголовок
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorHeaderBg)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = abilityName,
                fontSize = 18.sp,
                color = Color(0xFF424242)
            )
        }

        Column(modifier = Modifier.padding(vertical = 2.dp)) {
            // 1. Спасбросок (Всегда отображаем)
            val saveKey = "Спасбросок ($abilityName)"
            val saveMult = proficiencies[saveKey] ?: 0

            SavingThrowRow("Спасбросок", abilityScore, saveMult, profBonus, calculator)

            // Линия-разделитель (Всегда отображаем)
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // 2. Обычные навыки
            skillNames.forEach { name ->
                val mult = proficiencies[name] ?: 0
                SkillRow(name, abilityScore, mult, profBonus, calculator)
            }

            // Если навыков нет (например, Телосложение), пустого места не будет,
            // так как спасбросок и разделитель теперь есть всегда.
        }
    }
}

@Composable
fun SavingThrowRow(
    name: String,
    score: Int,
    multiplier: Int,
    profBonus: Int,
    calculator: DndCalculator
) {
    val totalBonus = calculator.calculateSkillBonus(score, profBonus, multiplier)
    val sign = if (totalBonus >= 0) "+" else ""

    // Спасбросок не красим в зеленый фон полностью, красится только чекбокс (логика ниже)
    // Но вы можете изменить bgColor, если хотите подсвечивать всю строку
    val bgColor = ColorNoneBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Квадрат чекбокса
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
                    .background(if (multiplier > 0) Color.DarkGray else Color.Transparent), // Закрашиваем если есть владение
                contentAlignment = Alignment.Center
            ) {
                // Можно добавить галочку, но простая заливка тоже понятна
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = name,
                fontSize = 14.sp,
                color = Color.Black
            )
        }

        Text(
            text = "$sign$totalBonus",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun SkillRow(
    name: String,
    score: Int,
    multiplier: Int,
    profBonus: Int,
    calculator: DndCalculator
) {
    val totalBonus = calculator.calculateSkillBonus(score, profBonus, multiplier)
    val sign = if (totalBonus >= 0) "+" else ""

    val bgColor = when (multiplier) {
        1 -> ColorProficientBg
        2 -> ColorExpertiseBg
        else -> ColorNoneBg
    }

    // ЛОГИКА ШРИФТОВ:
    // Длинные слова уменьшаем, остальные оставляем 14sp
    val (fontSize, lineHeight) = when (name) {
        "Проницательность" -> 11.sp to 12.sp
        "Внимательность" -> 11.sp to 12.sp
        "Ловкость рук" -> 13.sp to 14.sp // Чуть меньше, на всякий случай
        else -> 14.sp to 16.sp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Текст навыка
        Text(
            text = name,
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = Color.Black,
            // modifier.weight(1f) заставляет текст занимать всё место слева от бонуса.
            // padding(start = 26.dp) выравнивает его с текстом спасброска (18 квадрат + 8 пробел)
            modifier = Modifier
                .weight(1f)
                .padding(start = 26.dp)
        )

        // Бонус
        Text(
            text = "$sign$totalBonus",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/SkillsTab.kt
```
------
```kotlin
// Имя файла: ui/screens/character_sheet/CharacterSheetScreen.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dnd.app.ui.components.DndActionTopBar
import com.dnd.app.ui.screens.character_sheet.tabs.BioTab
import com.dnd.app.ui.screens.character_sheet.tabs.CombatTab
import com.dnd.app.ui.screens.character_sheet.tabs.InventoryTab
import com.dnd.app.ui.screens.character_sheet.tabs.SkillsTab
import com.dnd.app.ui.screens.character_sheet.tabs.SpellsTab
import com.dnd.app.ui.screens.character_sheet.tabs.StatsTab
import com.dnd.app.ui.theme.DndBackground

@Composable
fun CharacterSheetScreen(
    navigateUp: () -> Unit,
    viewModel: CharacterSheetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(3) }

    val tabs = listOf(
        TabItem("Магия", Icons.Filled.Star),
        TabItem("Бой", Icons.Filled.Build),
        TabItem("Снаряж.", Icons.Filled.ShoppingCart),
        TabItem("Главная", Icons.Filled.AccountBox),
        TabItem("Навыки", Icons.Filled.List),
        TabItem("Личность", Icons.Filled.Face),
        TabItem("Заметки", Icons.Filled.Create)
    )

    Scaffold(
        topBar = {
            DndActionTopBar(
                title = state.character?.name ?: "...",
                onBack = navigateUp
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.LightGray)
                    .horizontalScroll(rememberScrollState())
            ) {
                tabs.forEachIndexed { index, item ->
                    CustomBottomNavItem(
                        item = item,
                        isSelected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        },
        containerColor = DndBackground
    ) { innerPadding ->
        if (state.isLoading || state.character == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val char = state.character!!

            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTabIndex) {
                    0 -> SpellsTab(state.spells)
                    1 -> CombatTab(state.weapons)
                    2 -> InventoryTab(state.weapons)
                    3 -> StatsTab(
                        character = char,
                        calculator = viewModel.calculator,
                        onHpChange = { delta -> viewModel.updateHp(delta) },
                        onMoneyChange = { type, delta -> viewModel.updateMoney(type, delta) }
                    )
                    4 -> SkillsTab(
                        character = char,
                        calculator = viewModel.calculator
                    )
                    5 -> BioTab(char.bio)
                    6 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Заметки (WIP)", color = Color.White) }
                }
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)

@Composable
fun CustomBottomNavItem(
    item: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color.White else Color.LightGray

    Column(
        modifier = Modifier
            .run { if (isSelected) height(60.dp) else height(56.dp) }
            .background(bg)
            .border(1.dp, Color.Gray)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
        Text(
            text = item.title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/CharacterSheetScreen.kt
```
------
```kotlin
// Имя файла: ui/screens/character_sheet/tabs/StatsTab.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.screens.character_sheet.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnd.app.domain.calculator.DndCalculator
import com.dnd.app.domain.model.CharacterDomain
import com.dnd.app.domain.model.Stats
import com.dnd.app.ui.theme.DndBonusGreen
import com.dnd.app.ui.theme.DndMalusRed

// ==========================================
// ===== НАСТРОЙКИ (LAYOUT SETTINGS) ========
// ==========================================
private object LayoutSettings {
    // Веса колонок (ширина)
    // Левая колонка теперь фиксирована математически (квадраты), поэтому здесь веса правой части

    // Вертикальные веса правой колонки (Сумма не обязана быть 1.0, это пропорции)
    const val WEIGHT_HEADER = 0.18f    // Шапка (Имя/Класс)
    const val WEIGHT_COMBAT = 0.20f    // КД и БМ
    const val WEIGHT_HP = 0.22f        // Здоровье
    const val WEIGHT_SEC_STATS = 0.14f // Инициатива/Скорость
    const val WEIGHT_MONEY = 0.16f     // Деньги
    const val WEIGHT_REST = 0.18f      // Отдых

    // Размеры шрифтов
    val FONT_STAT_NAME = 10.sp
    val FONT_STAT_VALUE = 32.sp
    val FONT_MONEY_LABEL = 12.sp
    val FONT_INPUT = 14.sp

    // Отступы
    val GAP_DEFAULT = 4.dp
    val GAP_SMALL = 2.dp
}

@Composable
fun StatsTab(
    character: CharacterDomain,
    calculator: DndCalculator,
    onHpChange: (Int) -> Unit,
    // ВАЖНО: Теперь это обязательный параметр, а не nullable ViewModel
    onMoneyChange: (String, Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(LayoutSettings.GAP_DEFAULT)
    ) {
        val totalHeight = maxHeight
        // Расчет размера квадрата: (Высота - Отступы) / 6 элементов
        val statSize = (totalHeight - 24.dp) / 6

        Row(modifier = Modifier.fillMaxSize()) {
            // --- ЛЕВАЯ КОЛОНКА ---
            Column(
                modifier = Modifier
                    .width(statSize)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val itemMod = Modifier.height(statSize).fillMaxWidth()
                StatBoxStrict(itemMod, "Сила", character.stats.strength, calculator)
                StatBoxStrict(itemMod, "Ловкость", character.stats.dexterity, calculator)
                StatBoxStrict(itemMod, "Телос.", character.stats.constitution, calculator)
                StatBoxStrict(itemMod, "Интеллект", character.stats.intelligence, calculator)
                StatBoxStrict(itemMod, "Мудрость", character.stats.wisdom, calculator)
                StatBoxStrict(itemMod, "Харизма", character.stats.charisma, calculator)
            }

            Spacer(modifier = Modifier.width(LayoutSettings.GAP_DEFAULT))

            // --- ПРАВАЯ КОЛОНКА ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. ШАПКА
                Column(
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_HEADER),
                    verticalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f).fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(character.name.ifBlank{"Герой"}, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                    }

                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).padding(6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) { Text("Плут", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                        Box(
                            modifier = Modifier.width(statSize).fillMaxHeight().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(character.level.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }

                // 2. БОЕВОЙ БЛОК
                Row(
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_COMBAT).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                ) {
                    val ac = calculator.calculateBaseAC(calculator.calculateModifier(character.stats.dexterity))
                    val prof = calculator.calculateProficiencyBonus(character.level)
                    SquareInfoBox("КД", ac.toString(), Modifier.weight(1f).fillMaxHeight())
                    SquareInfoBox("БМ", "+$prof", Modifier.weight(1f).fillMaxHeight(), isBonus = true)
                }

                // 3. ЗДОРОВЬЕ
                HealthControlWidget(
                    current = character.hpCurrent,
                    max = character.hpMax,
                    onChange = onHpChange,
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_HP)
                )

                // 4. ВТОРИЧНЫЕ СТАТЫ
                Row(
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_SEC_STATS).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                ) {
                    val init = calculator.calculateModifier(character.stats.dexterity)
                    val passPerc = 10 + calculator.calculateModifier(character.stats.wisdom)
                    CompactInfoBox("Инициатива", calculator.formatModifier(init), Modifier.weight(1f).fillMaxHeight())
                    CompactInfoBox("Пас. вним.", passPerc.toString(), Modifier.weight(1f).fillMaxHeight())
                    CompactInfoBox("Скорость", "30", Modifier.weight(1f).fillMaxHeight())
                }

                // 5. ДЕНЬГИ
                MoneyWidgetCalculator(
                    stats = character.stats,
                    onUpdate = onMoneyChange, // Передаем callback напрямую
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_MONEY)
                )

                // 6. ОТДЫХ
                Column(
                    modifier = Modifier.weight(LayoutSettings.WEIGHT_REST),
                    verticalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_SMALL)
                ) {
                    Text("Отдых", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_DEFAULT)
                    ) {
                        RestButton("Короткий", Modifier.weight(1f).fillMaxHeight())
                        RestButton("Длинный", Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

// --- КОМПОНЕНТЫ ---

@Composable
fun StatBoxStrict(modifier: Modifier, name: String, value: Int, calculator: DndCalculator) {
    val mod = calculator.calculateModifier(value)
    Column(
        modifier = modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, fontSize = LayoutSettings.FONT_STAT_NAME, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = calculator.formatModifier(mod),
                fontSize = LayoutSettings.FONT_STAT_VALUE,
                fontWeight = FontWeight.Black,
                color = if (mod > 0) DndBonusGreen else if (mod < 0) DndMalusRed else Color.Black,
                textAlign = TextAlign.Center
            )
        }
        Box(modifier = Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp)) {
            Text(value.toString(), fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MoneyWidgetCalculator(stats: Stats, onUpdate: (String, Int) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        MoneyRowCalculator("ЗМ", stats.gold, onUpdate)
        MoneyRowCalculator("СМ", stats.silver, onUpdate)
        MoneyRowCalculator("ММ", stats.copper, onUpdate)
    }
}

@Composable
fun MoneyRowCalculator(label: String, currentValue: Int, onUpdate: (String, Int) -> Unit) {
    val type = when(label) { "ЗМ" -> "GP"; "СМ" -> "SP"; else -> "CP" }
    var calcValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            fontSize = LayoutSettings.FONT_MONEY_LABEL,
            modifier = Modifier.width(32.dp)
        )

        // ПОЛЕ 1: Абсолютный ввод (Текущее значение)
        BasicTextField(
            value = currentValue.toString(),
            onValueChange = { newValue ->
                if (newValue.isEmpty()) {
                    // Если стерли всё, считаем что хотим 0
                    val delta = 0 - currentValue
                    onUpdate(type, delta)
                } else if (newValue.all { it.isDigit() }) {
                    val newInt = newValue.toIntOrNull() ?: 0
                    val delta = newInt - currentValue
                    if (delta != 0) {
                        onUpdate(type, delta)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            textStyle = TextStyle(fontSize = LayoutSettings.FONT_INPUT, color = Color.Black),
            modifier = Modifier
                .weight(1f)
                .background(Color.White)
                .border(1.dp, Color.LightGray)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(Modifier.width(LayoutSettings.GAP_DEFAULT))

        // ПОЛЕ 2: Калькулятор
        BasicTextField(
            value = calcValue,
            onValueChange = { if (it.all { char -> char.isDigit() }) calcValue = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            textStyle = TextStyle(fontSize = LayoutSettings.FONT_INPUT, textAlign = TextAlign.Center),
            modifier = Modifier
                .width(40.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(2.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(2.dp))
                .padding(vertical = 2.dp)
        )

        Spacer(Modifier.width(LayoutSettings.GAP_DEFAULT))

        // КНОПКИ
        Row(horizontalArrangement = Arrangement.spacedBy(LayoutSettings.GAP_SMALL)) {
            Box(
                Modifier
                    .border(1.dp, Color.Gray)
                    .clickable {
                        val amount = calcValue.toIntOrNull() ?: 0
                        if (amount > 0) {
                            onUpdate(type, -amount)
                            calcValue = ""
                            focusManager.clearFocus()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) { Text("-", fontWeight = FontWeight.Bold) }

            Box(
                Modifier
                    .border(1.dp, Color.Gray)
                    .clickable {
                        val amount = calcValue.toIntOrNull() ?: 0
                        if (amount > 0) {
                            onUpdate(type, amount)
                            calcValue = ""
                            focusManager.clearFocus()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) { Text("+", fontWeight = FontWeight.Bold) }
        }
    }
}

// --- СТАРЫЕ КОМПОНЕНТЫ (Без изменений) ---
@Composable
fun SquareInfoBox(title: String, value: String, modifier: Modifier, isBonus: Boolean = false) {
    Column(
        modifier = modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 14.sp, color = Color.DarkGray)
        Text(value, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = if(isBonus) DndBonusGreen else Color.Black)
    }
}

@Composable
fun CompactInfoBox(title: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 9.sp, lineHeight = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HealthControlWidget(current: Int, max: Int, onChange: (Int) -> Unit, modifier: Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(Modifier.weight(0.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HealthButton("-1", DndMalusRed, Modifier.weight(1f)) { onChange(-1) }
            HealthButton("-10", DndMalusRed, Modifier.weight(1f)) { onChange(-10) }
        }
        Column(Modifier.weight(0.6f).fillMaxHeight().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Здоровье", fontSize = 12.sp, color = Color.DarkGray)
            Text("$current/$max", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(0.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HealthButton("+1", DndBonusGreen, Modifier.weight(1f)) { onChange(1) }
            HealthButton("+10", DndBonusGreen, Modifier.weight(1f)) { onChange(10) }
        }
    }
}

@Composable
fun HealthButton(text: String, textColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
    }
}

@Composable
fun RestButton(text: String, modifier: Modifier) {
    Box(modifier = modifier.background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)).clickable{}, contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/screens/character_sheet/tabs/StatsTab.kt
```
------
```kotlin
// Имя файла: ui/theme/Color.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.theme

import androidx.compose.ui.graphics.Color

val DndPrimary = Color(0xFF424242) // Шапка
val DndBackground = Color(0xFF707070) // ТЕМНО-СЕРЫЙ фон (как в оригинале)
val DndSurface = Color(0xFFFFFFFF) // Белые плашки
val DndTextPrimary = Color(0xFF000000)

val DndBonusGreen = Color(0xFF2E7D32) // Темно-зеленый (читабельнее)
val DndMalusRed = Color(0xFFC62828)   // Темно-красный
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/theme/Color.kt
```
------
```kotlin
// Имя файла: ui/theme/Theme.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DndPrimary,
    background = Color.Black,
    surface = DndPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = DndPrimary,
    background = DndBackground,
    surface = DndSurface,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun DndTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: ui/theme/Theme.kt
```
---
