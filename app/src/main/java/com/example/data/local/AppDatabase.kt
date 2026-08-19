package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ConfigDao
import com.example.data.local.dao.InsightDao
import com.example.data.local.dao.ShortcutDao
import com.example.data.local.dao.TelemetryDao
import com.example.data.local.dao.VoiceprintDao
import com.example.data.local.entity.ActionShortcutEntity
import com.example.data.local.entity.AssistantConfigEntity
import com.example.data.local.entity.BehaviorInsightEntity
import com.example.data.local.entity.TelemetryLogEntity
import com.example.data.local.entity.VoiceprintEntity

@Database(
    entities = [
        TelemetryLogEntity::class,
        ActionShortcutEntity::class,
        BehaviorInsightEntity::class,
        AssistantConfigEntity::class,
        VoiceprintEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun insightDao(): InsightDao
    abstract fun configDao(): ConfigDao
    abstract fun voiceprintDao(): VoiceprintDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_assistant_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
