package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ActionShortcutEntity
import com.example.data.local.entity.AssistantConfigEntity
import com.example.data.local.entity.BehaviorInsightEntity
import com.example.data.local.entity.TelemetryLogEntity
import com.example.data.local.entity.TelemetryType
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<TelemetryLogEntity>>

    @Query("SELECT * FROM telemetry_logs WHERE type = :type ORDER BY timestamp DESC LIMIT 100")
    fun getLogsByType(type: TelemetryType): Flow<List<TelemetryLogEntity>>

    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp DESC LIMIT 30")
    suspend fun getRecentLogsForAi(): List<TelemetryLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TelemetryLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<TelemetryLogEntity>)

    @Query("DELETE FROM telemetry_logs WHERE id NOT IN (SELECT id FROM telemetry_logs ORDER BY timestamp DESC LIMIT 300)")
    suspend fun purgeOldLogs()

    @Query("DELETE FROM telemetry_logs")
    suspend fun clearAll()
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM action_shortcuts ORDER BY executionCount DESC, id DESC")
    fun getAllShortcuts(): Flow<List<ActionShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: ActionShortcutEntity): Long

    @Update
    suspend fun updateShortcut(shortcut: ActionShortcutEntity)

    @Query("UPDATE action_shortcuts SET executionCount = executionCount + 1, lastExecutedAt = :now WHERE id = :id")
    suspend fun incrementExecution(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM action_shortcuts WHERE id = :id")
    suspend fun deleteShortcut(id: Long)
}

@Dao
interface InsightDao {
    @Query("SELECT * FROM behavior_insights ORDER BY timestamp DESC")
    fun getAllInsights(): Flow<List<BehaviorInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: BehaviorInsightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<BehaviorInsightEntity>)

    @Query("UPDATE behavior_insights SET isApplied = 1 WHERE id = :id")
    suspend fun markApplied(id: Long)

    @Query("DELETE FROM behavior_insights")
    suspend fun clearAll()
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM assistant_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<AssistantConfigEntity?>

    @Query("SELECT * FROM assistant_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AssistantConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: AssistantConfigEntity)
}
