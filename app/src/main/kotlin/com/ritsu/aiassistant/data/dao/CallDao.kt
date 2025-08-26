package com.ritsu.aiassistant.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.ritsu.aiassistant.data.model.CallRecord
import com.ritsu.aiassistant.data.model.CallPreference

@Dao
interface CallDao {
    
    // Call Records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(callRecord: CallRecord): Long
    
    @Update
    suspend fun updateCall(callRecord: CallRecord)
    
    @Delete
    suspend fun deleteCall(callRecord: CallRecord)
    
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallRecord>>
    
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    suspend fun getAllCallsAsList(): List<CallRecord>
    
    @Query("SELECT * FROM call_records WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getCallsByNumber(phoneNumber: String): Flow<List<CallRecord>>
    
    @Query("SELECT * FROM call_records WHERE callType = :callType ORDER BY timestamp DESC")
    fun getCallsByType(callType: CallRecord.CallType): Flow<List<CallRecord>>
    
    @Query("SELECT * FROM call_records WHERE wasHandledByRitsu = 1 ORDER BY timestamp DESC")
    fun getCallsHandledByRitsu(): Flow<List<CallRecord>>
    
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCalls(limit: Int): Flow<List<CallRecord>>
    
    @Query("SELECT * FROM call_records WHERE id = :id")
    suspend fun getCallById(id: Long): CallRecord?
    
    @Query("SELECT * FROM call_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getCallsInDateRange(startTime: Long, endTime: Long): List<CallRecord>
    
    @Query("DELETE FROM call_records WHERE timestamp < :cutoffTime")
    suspend fun deleteCallsOlderThan(cutoffTime: Long)
    
    @Query("SELECT * FROM call_records WHERE phoneNumber LIKE :query OR callerName LIKE :query OR summary LIKE :query ORDER BY timestamp DESC")
    suspend fun searchCalls(query: String): List<CallRecord>
    
    // Call Preferences
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePreference(preference: CallPreference)
    
    @Delete
    suspend fun deletePreference(preference: CallPreference)
    
    @Query("SELECT * FROM call_preferences WHERE phoneNumber = :phoneNumber")
    suspend fun getPreference(phoneNumber: String): CallPreference?
    
    @Query("SELECT * FROM call_preferences ORDER BY lastInteractionTime DESC")
    fun getAllPreferences(): Flow<List<CallPreference>>
    
    @Query("SELECT * FROM call_preferences WHERE isBlocked = 1 ORDER BY lastInteractionTime DESC")
    fun getBlockedNumbers(): Flow<List<CallPreference>>
    
    @Query("SELECT * FROM call_preferences WHERE isVip = 1 ORDER BY lastInteractionTime DESC")
    fun getVipContacts(): Flow<List<CallPreference>>
    
    @Query("SELECT * FROM call_preferences WHERE autoAnswer = 1 ORDER BY lastInteractionTime DESC")
    fun getAutoAnswerContacts(): Flow<List<CallPreference>>
    
    // Statistics
    @Query("SELECT COUNT(*) FROM call_records")
    suspend fun getTotalCallCount(): Int
    
    @Query("SELECT COUNT(*) FROM call_records WHERE wasHandledByRitsu = 1")
    suspend fun getRitsuHandledCallCount(): Int
    
    @Query("SELECT AVG(duration) FROM call_records WHERE duration > 0")
    suspend fun getAverageCallDuration(): Long?
    
    @Query("SELECT COUNT(*) FROM call_records WHERE callType = :callType")
    suspend fun getCallCountByType(callType: CallRecord.CallType): Int
    
    @Query("SELECT phoneNumber, COUNT(*) as count FROM call_records GROUP BY phoneNumber ORDER BY count DESC LIMIT :limit")
    suspend fun getMostCalledNumbers(limit: Int): List<PhoneNumberCount>
}

data class PhoneNumberCount(
    val phoneNumber: String,
    val count: Int
) {
    fun toPair(): Pair<String, Int> = Pair(phoneNumber, count)
}