package com.ritsu.aiassistant.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.ritsu.aiassistant.data.model.UserPreference

@Dao
interface UserPreferenceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: UserPreference)
    
    @Update
    suspend fun updatePreference(preference: UserPreference)
    
    @Delete
    suspend fun deletePreference(preference: UserPreference)
    
    @Query("SELECT * FROM user_preferences WHERE key = :key")
    suspend fun getPreference(key: String): UserPreference?
    
    @Query("SELECT * FROM user_preferences WHERE category = :category ORDER BY key ASC")
    fun getPreferencesByCategory(category: String): Flow<List<UserPreference>>
    
    @Query("SELECT * FROM user_preferences ORDER BY category ASC, key ASC")
    fun getAllPreferences(): Flow<List<UserPreference>>
    
    @Query("DELETE FROM user_preferences WHERE key = :key")
    suspend fun deletePreferenceByKey(key: String)
    
    @Query("SELECT value FROM user_preferences WHERE key = :key")
    suspend fun getPreferenceValue(key: String): String?
    
    @Query("SELECT COUNT(*) FROM user_preferences WHERE category = :category")
    suspend fun getPreferenceCountByCategory(category: String): Int
}