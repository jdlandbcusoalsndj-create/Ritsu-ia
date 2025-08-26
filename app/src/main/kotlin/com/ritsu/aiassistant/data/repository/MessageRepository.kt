package com.ritsu.aiassistant.data.repository

import com.ritsu.aiassistant.data.dao.MessageDao
import com.ritsu.aiassistant.data.model.*
import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDao) {
    
    // Message Records
    suspend fun insertMessage(message: MessageRecord): Long {
        return messageDao.insertMessage(message)
    }
    
    suspend fun updateMessage(message: MessageRecord) {
        messageDao.updateMessage(message)
    }
    
    suspend fun deleteMessage(message: MessageRecord) {
        messageDao.deleteMessage(message)
    }
    
    fun getAllMessages(): Flow<List<MessageRecord>> {
        return messageDao.getAllMessages()
    }
    
    fun getMessagesByContact(contactId: String): Flow<List<MessageRecord>> {
        return messageDao.getMessagesByContact(contactId)
    }
    
    fun getMessagesByPlatform(platform: String): Flow<List<MessageRecord>> {
        return messageDao.getMessagesByPlatform(platform)
    }
    
    fun getRecentMessages(limit: Int = 50): Flow<List<MessageRecord>> {
        return messageDao.getRecentMessages(limit)
    }
    
    suspend fun getRecentMessages(contactId: String, platform: String, limit: Int): List<MessageRecord> {
        return messageDao.getRecentMessagesByContact(contactId, platform, limit)
    }
    
    suspend fun markAsReadByRitsu(sender: String, content: String) {
        messageDao.markAsReadByRitsu(sender, content)
    }
    
    fun getUnreadMessages(): Flow<List<MessageRecord>> {
        return messageDao.getUnreadMessages()
    }
    
    fun getUrgentMessages(): Flow<List<MessageRecord>> {
        return messageDao.getUrgentMessages()
    }
    
    fun getSpamMessages(): Flow<List<MessageRecord>> {
        return messageDao.getSpamMessages()
    }
    
    fun getRitsuGeneratedMessages(): Flow<List<MessageRecord>> {
        return messageDao.getRitsuGeneratedMessages()
    }
    
    // Contact Preferences
    suspend fun insertOrUpdateContactPreference(preference: ContactPreference) {
        messageDao.insertOrUpdateContactPreference(preference)
    }
    
    suspend fun deleteContactPreference(preference: ContactPreference) {
        messageDao.deleteContactPreference(preference)
    }
    
    suspend fun getContactPreference(contactId: String): ContactPreference? {
        return messageDao.getContactPreference(contactId)
    }
    
    fun getAllContactPreferences(): Flow<List<ContactPreference>> {
        return messageDao.getAllContactPreferences()
    }
    
    fun getVipContacts(): Flow<List<ContactPreference>> {
        return messageDao.getVipContacts()
    }
    
    fun getBlockedContacts(): Flow<List<ContactPreference>> {
        return messageDao.getBlockedContacts()
    }
    
    fun getAutoRespondContacts(): Flow<List<ContactPreference>> {
        return messageDao.getAutoRespondContacts()
    }
    
    suspend fun updateContactLastSeen(contactId: String, timestamp: Long) {
        messageDao.updateContactLastSeen(contactId, timestamp)
    }
    
    // Auto Responses
    suspend fun insertAutoResponse(autoResponse: AutoResponse): Long {
        return messageDao.insertAutoResponse(autoResponse)
    }
    
    suspend fun updateAutoResponse(autoResponse: AutoResponse) {
        messageDao.updateAutoResponse(autoResponse)
    }
    
    suspend fun deleteAutoResponse(autoResponse: AutoResponse) {
        messageDao.deleteAutoResponse(autoResponse)
    }
    
    fun getAllAutoResponses(): Flow<List<AutoResponse>> {
        return messageDao.getAllAutoResponses()
    }
    
    fun getActiveAutoResponses(): Flow<List<AutoResponse>> {
        return messageDao.getActiveAutoResponses()
    }
    
    suspend fun getAutoResponseByTrigger(trigger: String, platform: String): AutoResponse? {
        return messageDao.getAutoResponseByTrigger(trigger, platform)
    }
    
    suspend fun incrementAutoResponseUsage(id: Long) {
        messageDao.incrementAutoResponseUsage(id)
    }
    
    // Conversation Contexts
    suspend fun insertOrUpdateConversationContext(context: ConversationContext) {
        messageDao.insertOrUpdateConversationContext(context)
    }
    
    suspend fun deleteConversationContext(context: ConversationContext) {
        messageDao.deleteConversationContext(context)
    }
    
    suspend fun getConversationContext(conversationId: String): ConversationContext? {
        return messageDao.getConversationContext(conversationId)
    }
    
    fun getAllConversationContexts(): Flow<List<ConversationContext>> {
        return messageDao.getAllConversationContexts()
    }
    
    fun getActiveConversations(): Flow<List<ConversationContext>> {
        return messageDao.getActiveConversations()
    }
    
    suspend fun updateConversationMessageCount(conversationId: String, increment: Int = 1) {
        messageDao.updateConversationMessageCount(conversationId, increment)
    }
    
    // Statistics and Analytics
    suspend fun getTotalMessageCount(): Int {
        return messageDao.getTotalMessageCount()
    }
    
    suspend fun getRitsuResponseCount(): Int {
        return messageDao.getRitsuResponseCount()
    }
    
    suspend fun getAverageResponseTime(): Long {
        return messageDao.getAverageResponseTime() ?: 0L
    }
    
    suspend fun getMessageCountByPlatform(): Map<String, Int> {
        val platforms = listOf("SMS", "WhatsApp", "Telegram", "Other")
        return platforms.associateWith { platform ->
            messageDao.getMessageCountByPlatform(platform)
        }
    }
    
    suspend fun getMostActiveContacts(limit: Int = 10): List<Pair<String, Int>> {
        return messageDao.getMostActiveContacts(limit)
    }
    
    suspend fun getMessagesToday(): List<MessageRecord> {
        val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000)
        return messageDao.getMessagesInDateRange(startOfDay, endOfDay)
    }
    
    suspend fun getMessagesThisWeek(): List<MessageRecord> {
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return messageDao.getMessagesInDateRange(weekStart, System.currentTimeMillis())
    }
    
    // Search and Filter
    suspend fun searchMessages(query: String): List<MessageRecord> {
        return messageDao.searchMessages("%$query%")
    }
    
    suspend fun searchMessagesByContact(contactId: String, query: String): List<MessageRecord> {
        return messageDao.searchMessagesByContact(contactId, "%$query%")
    }
    
    fun getMessagesByDateRange(startTime: Long, endTime: Long): Flow<List<MessageRecord>> {
        return messageDao.getMessagesByDateRange(startTime, endTime)
    }
    
    fun getMessagesBySentiment(sentiment: String): Flow<List<MessageRecord>> {
        return messageDao.getMessagesBySentiment(sentiment)
    }
    
    // Maintenance and Cleanup
    suspend fun clearOldMessages(olderThanDays: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        messageDao.deleteMessagesOlderThan(cutoffTime)
    }
    
    suspend fun clearSpamMessages() {
        messageDao.deleteSpamMessages()
    }
    
    suspend fun exportMessageHistory(): List<MessageRecord> {
        return messageDao.getAllMessagesAsList()
    }
    
    // Smart Features
    suspend fun getFrequentlyContactedPeople(limit: Int = 5): List<String> {
        return messageDao.getFrequentlyContactedPeople(limit)
    }
    
    suspend fun getPendingResponses(): List<MessageRecord> {
        // Mensajes que Ritsu no ha respondido y que podrían necesitar respuesta
        return messageDao.getPendingResponses()
    }
    
    suspend fun getSentimentTrend(contactId: String, days: Int = 7): List<Pair<Long, String>> {
        val startTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return messageDao.getSentimentTrend(contactId, startTime)
    }
    
    suspend fun getConversationSummary(contactId: String, days: Int = 1): String {
        val messages = getRecentMessages(contactId, "ALL", 20)
        return if (messages.isNotEmpty()) {
            "Conversación reciente con $contactId: ${messages.size} mensajes intercambiados"
        } else {
            "Sin conversación reciente"
        }
    }
}