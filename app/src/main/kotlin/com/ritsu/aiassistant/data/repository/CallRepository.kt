package com.ritsu.aiassistant.data.repository

import com.ritsu.aiassistant.data.dao.CallDao
import com.ritsu.aiassistant.data.model.CallRecord
import com.ritsu.aiassistant.data.model.CallPreference
import kotlinx.coroutines.flow.Flow

class CallRepository(private val callDao: CallDao) {
    
    // Call Records
    suspend fun insertCall(callRecord: CallRecord): Long {
        return callDao.insertCall(callRecord)
    }
    
    suspend fun updateCall(callRecord: CallRecord) {
        callDao.updateCall(callRecord)
    }
    
    suspend fun deleteCall(callRecord: CallRecord) {
        callDao.deleteCall(callRecord)
    }
    
    fun getAllCalls(): Flow<List<CallRecord>> {
        return callDao.getAllCalls()
    }
    
    fun getCallsByNumber(phoneNumber: String): Flow<List<CallRecord>> {
        return callDao.getCallsByNumber(phoneNumber)
    }
    
    fun getCallsByType(callType: CallRecord.CallType): Flow<List<CallRecord>> {
        return callDao.getCallsByType(callType)
    }
    
    fun getCallsHandledByRitsu(): Flow<List<CallRecord>> {
        return callDao.getCallsHandledByRitsu()
    }
    
    fun getRecentCalls(limit: Int = 20): Flow<List<CallRecord>> {
        return callDao.getRecentCalls(limit)
    }
    
    suspend fun getCallById(id: Long): CallRecord? {
        return callDao.getCallById(id)
    }
    
    suspend fun getCallsInDateRange(startTime: Long, endTime: Long): List<CallRecord> {
        return callDao.getCallsInDateRange(startTime, endTime)
    }
    
    // Call Preferences
    suspend fun insertOrUpdatePreference(preference: CallPreference) {
        callDao.insertOrUpdatePreference(preference)
    }
    
    suspend fun deletePreference(preference: CallPreference) {
        callDao.deletePreference(preference)
    }
    
    suspend fun getPreference(phoneNumber: String): CallPreference? {
        return callDao.getPreference(phoneNumber)
    }
    
    fun getAllPreferences(): Flow<List<CallPreference>> {
        return callDao.getAllPreferences()
    }
    
    fun getBlockedNumbers(): Flow<List<CallPreference>> {
        return callDao.getBlockedNumbers()
    }
    
    fun getVipContacts(): Flow<List<CallPreference>> {
        return callDao.getVipContacts()
    }
    
    fun getAutoAnswerContacts(): Flow<List<CallPreference>> {
        return callDao.getAutoAnswerContacts()
    }
    
    // Statistics and Analytics
    suspend fun getTotalCallCount(): Int {
        return callDao.getTotalCallCount()
    }
    
    suspend fun getRitsuHandledCallCount(): Int {
        return callDao.getRitsuHandledCallCount()
    }
    
    suspend fun getAverageCallDuration(): Long {
        return callDao.getAverageCallDuration() ?: 0L
    }
    
    suspend fun getCallCountByType(): Map<CallRecord.CallType, Int> {
        val incoming = callDao.getCallCountByType(CallRecord.CallType.INCOMING)
        val outgoing = callDao.getCallCountByType(CallRecord.CallType.OUTGOING)
        val missed = callDao.getCallCountByType(CallRecord.CallType.MISSED)
        
        return mapOf(
            CallRecord.CallType.INCOMING to incoming,
            CallRecord.CallType.OUTGOING to outgoing,
            CallRecord.CallType.MISSED to missed
        )
    }
    
    suspend fun getMostCalledNumbers(limit: Int = 10): List<Pair<String, Int>> {
        return callDao.getMostCalledNumbers(limit)
    }
    
    suspend fun getCallsToday(): List<CallRecord> {
        val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000)
        return getCallsInDateRange(startOfDay, endOfDay)
    }
    
    suspend fun getCallsThisWeek(): List<CallRecord> {
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return getCallsInDateRange(weekStart, System.currentTimeMillis())
    }
    
    // Utility methods
    suspend fun clearOldCalls(olderThanDays: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        callDao.deleteCallsOlderThan(cutoffTime)
    }
    
    suspend fun exportCallHistory(): List<CallRecord> {
        return callDao.getAllCallsAsList()
    }
    
    suspend fun searchCalls(query: String): List<CallRecord> {
        return callDao.searchCalls("%$query%")
    }
}