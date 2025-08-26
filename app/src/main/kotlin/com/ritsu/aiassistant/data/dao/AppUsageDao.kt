package com.ritsu.aiassistant.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.ritsu.aiassistant.data.model.AppUsageRecord

@Dao
interface AppUsageDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: AppUsageRecord): Long
    
    @Update
    suspend fun updateUsage(usage: AppUsageRecord)
    
    @Delete
    suspend fun deleteUsage(usage: AppUsageRecord)
    
    @Query("SELECT * FROM app_usage_records ORDER BY openTime DESC")
    fun getAllUsageRecords(): Flow<List<AppUsageRecord>>
    
    @Query("SELECT * FROM app_usage_records WHERE packageName = :packageName ORDER BY openTime DESC")
    fun getUsageByPackage(packageName: String): Flow<List<AppUsageRecord>>
    
    @Query("SELECT * FROM app_usage_records ORDER BY openTime DESC LIMIT :limit")
    fun getRecentUsage(limit: Int): Flow<List<AppUsageRecord>>
    
    @Query("SELECT * FROM app_usage_records WHERE openTime BETWEEN :startTime AND :endTime ORDER BY openTime DESC")
    suspend fun getUsageInDateRange(startTime: Long, endTime: Long): List<AppUsageRecord>
    
    @Query("SELECT packageName, SUM(usageDuration) as totalTime FROM app_usage_records GROUP BY packageName ORDER BY totalTime DESC LIMIT :limit")
    suspend fun getMostUsedApps(limit: Int): List<AppUsageStats>
    
    @Query("SELECT COUNT(*) FROM app_usage_records WHERE packageName = :packageName")
    suspend fun getAppOpenCount(packageName: String): Int
    
    @Query("SELECT AVG(usageDuration) FROM app_usage_records WHERE packageName = :packageName AND usageDuration > 0")
    suspend fun getAverageUsageTime(packageName: String): Long?
    
    @Query("DELETE FROM app_usage_records WHERE openTime < :cutoffTime")
    suspend fun deleteOldUsageRecords(cutoffTime: Long)
}

data class AppUsageStats(
    val packageName: String,
    val totalTime: Long
)