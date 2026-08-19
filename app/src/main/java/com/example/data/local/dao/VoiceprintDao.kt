package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.VoiceprintEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceprintDao {
    @Query("SELECT * FROM voiceprint_profiles ORDER BY sampleIndex ASC")
    fun getAllVoiceprints(): Flow<List<VoiceprintEntity>>

    @Query("SELECT * FROM voiceprint_profiles ORDER BY sampleIndex ASC")
    suspend fun getVoiceprintsSync(): List<VoiceprintEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceprint(voiceprint: VoiceprintEntity): Long

    @Query("DELETE FROM voiceprint_profiles")
    suspend fun clearVoiceprints()
}
