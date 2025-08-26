package com.ritsu.aiassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val conversationId: String = "default",
    val messageType: MessageType = MessageType.TEXT,
    val metadata: String = "",
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)

enum class MessageType {
    TEXT,
    VOICE,
    IMAGE,
    SYSTEM,
    CALL_SUMMARY,
    SMS_RESPONSE
}

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    val id: String,
    val title: String,
    val lastMessageTime: Long,
    val participantType: ParticipantType,
    val participantInfo: String = "", // Información del contacto, número, etc.
    val isActive: Boolean = true
)

enum class ParticipantType {
    USER,
    PHONE_CALLER,
    SMS_CONTACT,
    WHATSAPP_CONTACT,
    SYSTEM
}