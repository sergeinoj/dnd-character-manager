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