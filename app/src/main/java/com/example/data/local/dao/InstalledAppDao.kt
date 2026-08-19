package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.InstalledAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {
    @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getAppByPackage(pkg: String): InstalledAppEntity?

    @Query("SELECT * FROM installed_apps WHERE keywords LIKE '%' || :query || '%' OR appName LIKE '%' || :query || '%' OR appNameArabic LIKE '%' || :query || '%' LIMIT 5")
    suspend fun findMatchingApps(query: String): List<InstalledAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<InstalledAppEntity>)

    @Query("DELETE FROM installed_apps")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM installed_apps")
    fun getAppCount(): Flow<Int>
}
