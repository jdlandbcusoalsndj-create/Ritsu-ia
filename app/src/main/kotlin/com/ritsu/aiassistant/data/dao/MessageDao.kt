package com.ritsu.aiassistant.data.dao

import androidx.room.*
import androidx.room.Dao
import kotlinx.coroutines.flow.Flow
import com.ritsu.aiassistant.data.model.*

@Dao
interface MessageDao {
    
    // Message Records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageRecord): Long
    
    @Update
    suspend fun updateMessage(message: MessageRecord)
    
    @Delete
    suspend fun deleteMessage(message: MessageRecord)
    
    @Query("SELECT * FROM message_records ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records ORDER BY timestamp DESC")
    suspend fun getAllMessagesAsList(): List<MessageRecord>
    
    @Query("SELECT * FROM message_records WHERE sender = :contactId ORDER BY timestamp DESC")
    fun getMessagesByContact(contactId: String): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records WHERE platform = :platform ORDER BY timestamp DESC")
    fun getMessagesByPlatform(platform: String): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records WHERE sender = :contactId AND platform = :platform ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesByContact(contactId: String, platform: String, limit: Int): List<MessageRecord>
    
    @Query("UPDATE message_records SET wasReadByRitsu = 1 WHERE sender = :sender AND content = :content")
    suspend fun markAsReadByRitsu(sender: String, content: String)
    
    @Query("SELECT * FROM message_records WHERE wasReadByRitsu = 0 AND isIncoming = 1 ORDER BY timestamp DESC")
    fun getUnreadMessages(): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records WHERE isUrgent = 1 ORDER BY timestamp DESC")
    fun getUrgentMessages(): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records WHERE isSpam = 1 ORDER BY timestamp DESC")
    fun getSpamMessages(): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records WHERE wasGeneratedByRitsu = 1 ORDER BY timestamp DESC")
    fun getRitsuGeneratedMessages(): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getMessagesByDateRange(startTime: Long, endTime: Long): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getMessagesInDateRange(startTime: Long, endTime: Long): List<MessageRecord>
    
    @Query("SELECT * FROM message_records WHERE sentiment = :sentiment ORDER BY timestamp DESC")
    fun getMessagesBySentiment(sentiment: String): Flow<List<MessageRecord>>
    
    @Query("SELECT * FROM message_records WHERE content LIKE :query ORDER BY timestamp DESC")
    suspend fun searchMessages(query: String): List<MessageRecord>
    
    @Query("SELECT * FROM message_records WHERE sender = :contactId AND content LIKE :query ORDER BY timestamp DESC")
    suspend fun searchMessagesByContact(contactId: String, query: String): List<MessageRecord>
    
    @Query("DELETE FROM message_records WHERE timestamp < :cutoffTime")
    suspend fun deleteMessagesOlderThan(cutoffTime: Long)
    
    @Query("DELETE FROM message_records WHERE isSpam = 1")
    suspend fun deleteSpamMessages()
    
    // Contact Preferences
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContactPreference(preference: ContactPreference)
    
    @Delete
    suspend fun deleteContactPreference(preference: ContactPreference)
    
    @Query("SELECT * FROM contact_preferences WHERE contactId = :contactId")
    suspend fun getContactPreference(contactId: String): ContactPreference?
    
    @Query("SELECT * FROM contact_preferences ORDER BY lastInteractionTime DESC")
    fun getAllContactPreferences(): Flow<List<ContactPreference>>
    
    @Query("SELECT * FROM contact_preferences WHERE isVip = 1 ORDER BY lastInteractionTime DESC")
    fun getVipContacts(): Flow<List<ContactPreference>>
    
    @Query("SELECT * FROM contact_preferences WHERE isBlocked = 1 ORDER BY lastInteractionTime DESC")
    fun getBlockedContacts(): Flow<List<ContactPreference>>
    
    @Query("SELECT * FROM contact_preferences WHERE autoRespond = 1 ORDER BY lastInteractionTime DESC")
    fun getAutoRespondContacts(): Flow<List<ContactPreference>>
    
    @Query("UPDATE contact_preferences SET lastInteractionTime = :timestamp WHERE contactId = :contactId")
    suspend fun updateContactLastSeen(contactId: String, timestamp: Long)
    
    // Auto Responses
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutoResponse(autoResponse: AutoResponse): Long
    
    @Update
    suspend fun updateAutoResponse(autoResponse: AutoResponse)
    
    @Delete
    suspend fun deleteAutoResponse(autoResponse: AutoResponse)
    
    @Query("SELECT * FROM auto_responses ORDER BY priority DESC, id ASC")
    fun getAllAutoResponses(): Flow<List<AutoResponse>>
    
    @Query("SELECT * FROM auto_responses WHERE isActive = 1 ORDER BY priority DESC, id ASC")
    fun getActiveAutoResponses(): Flow<List<AutoResponse>>
    
    @Query("SELECT * FROM auto_responses WHERE trigger = :trigger AND (platform = :platform OR platform = 'ALL') AND isActive = 1 ORDER BY priority DESC LIMIT 1")
    suspend fun getAutoResponseByTrigger(trigger: String, platform: String): AutoResponse?
    
    @Query("UPDATE auto_responses SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementAutoResponseUsage(id: Long, timestamp: Long = System.currentTimeMillis())
    
    // Conversation Contexts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConversationContext(context: ConversationContext)
    
    @Delete
    suspend fun deleteConversationContext(context: ConversationContext)
    
    @Query("SELECT * FROM conversation_contexts WHERE conversationId = :conversationId")
    suspend fun getConversationContext(conversationId: String): ConversationContext?
    
    @Query("SELECT * FROM conversation_contexts ORDER BY lastMessageTime DESC")
    fun getAllConversationContexts(): Flow<List<ConversationContext>>
    
    @Query("SELECT * FROM conversation_contexts WHERE isActive = 1 ORDER BY lastMessageTime DESC")
    fun getActiveConversations(): Flow<List<ConversationContext>>
    
    @Query("UPDATE conversation_contexts SET messageCount = messageCount + :increment WHERE conversationId = :conversationId")
    suspend fun updateConversationMessageCount(conversationId: String, increment: Int)
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM message_records")
    suspend fun getTotalMessageCount(): Int
    
    @Query("SELECT COUNT(*) FROM message_records WHERE wasGeneratedByRitsu = 1")
    suspend fun getRitsuResponseCount(): Int
    
    @Query("SELECT AVG(responseTime) FROM message_records WHERE responseTime > 0")
    suspend fun getAverageResponseTime(): Long?
    
    @Query("SELECT COUNT(*) FROM message_records WHERE platform = :platform")
    suspend fun getMessageCountByPlatform(platform: String): Int
    
    @Query("SELECT sender, COUNT(*) as count FROM message_records GROUP BY sender ORDER BY count DESC LIMIT :limit")
    suspend fun getMostActiveContacts(limit: Int): List<ContactMessageCount>
    
    @Query("SELECT sender FROM message_records GROUP BY sender ORDER BY COUNT(*) DESC LIMIT :limit")
    suspend fun getFrequentlyContactedPeople(limit: Int): List<String>
    
    @Query("SELECT * FROM message_records WHERE isIncoming = 1 AND wasGeneratedByRitsu = 0 AND timestamp > :cutoffTime ORDER BY timestamp DESC")
    suspend fun getPendingResponses(cutoffTime: Long = System.currentTimeMillis() - 3600000): List<MessageRecord> // 1 hour
    
    @Query("SELECT timestamp, sentiment FROM message_records WHERE sender = :contactId AND timestamp > :startTime ORDER BY timestamp ASC")
    suspend fun getSentimentTrend(contactId: String, startTime: Long): List<SentimentPoint>
}

// Data classes for complex query results
data class ContactMessageCount(
    val sender: String,
    val count: Int
) {
    fun toPair(): Pair<String, Int> = Pair(sender, count)
}

data class SentimentPoint(
    val timestamp: Long,
    val sentiment: String
) {
    fun toPair(): Pair<Long, String> = Pair(timestamp, sentiment)
}