package com.ritsu.aiassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_records")
data class MessageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val platform: String, // SMS, WhatsApp, Telegram, etc.
    val isIncoming: Boolean,
    val wasReadByRitsu: Boolean = false,
    val wasGeneratedByRitsu: Boolean = false,
    val messageType: String = "text", // text, image, voice, etc.
    val conversationId: String = sender, // Agrupa mensajes por conversación
    val isUrgent: Boolean = false,
    val isSpam: Boolean = false,
    val responseTime: Long = 0, // Tiempo que tardó Ritsu en responder
    val sentiment: String = "neutral" // positive, negative, neutral
)

@Entity(tableName = "contact_preferences")
data class ContactPreference(
    @PrimaryKey
    val contactId: String, // Número de teléfono o ID del contacto
    val displayName: String = "",
    val autoRespond: Boolean = false,
    val readAloud: Boolean = true,
    val customInstructions: String = "",
    val isBlocked: Boolean = false,
    val isVip: Boolean = false,
    val lastInteractionTime: Long = 0,
    val preferredResponseStyle: String = "friendly", // formal, friendly, brief
    val platform: String = "SMS"
)

@Entity(tableName = "auto_responses")
data class AutoResponse(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trigger: String, // Palabra clave o patrón que activa la respuesta
    val response: String,
    val isActive: Boolean = true,
    val platform: String = "ALL", // SMS, WhatsApp, ALL
    val priority: Int = 0, // Mayor número = mayor prioridad
    val usageCount: Int = 0,
    val lastUsed: Long = 0
)

@Entity(tableName = "conversation_contexts")
data class ConversationContext(
    @PrimaryKey
    val conversationId: String,
    val contactId: String,
    val platform: String,
    val currentTopic: String = "",
    val lastMessageTime: Long,
    val messageCount: Int = 0,
    val isActive: Boolean = true,
    val context: String = "", // JSON con contexto de la conversación
    val summary: String = ""
)