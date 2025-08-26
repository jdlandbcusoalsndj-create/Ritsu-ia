package com.ritsu.aiassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_records")
data class CallRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val callType: CallType,
    val timestamp: Long,
    val duration: Long = 0, // en milisegundos
    val wasHandledByRitsu: Boolean = false,
    val summary: String = "",
    val conversationLog: String = "",
    val callerName: String = "",
    val wasAnswered: Boolean = false,
    val wasRejected: Boolean = false,
    val rating: Int = 0, // 0-5 estrellas para calificar el manejo de Ritsu
    val notes: String = ""
) {
    enum class CallType {
        INCOMING,
        OUTGOING,
        MISSED
    }
}

@Entity(tableName = "call_preferences")
data class CallPreference(
    @PrimaryKey
    val phoneNumber: String,
    val contactName: String = "",
    val autoAnswer: Boolean = false,
    val allowRitsuHandling: Boolean = true,
    val customInstructions: String = "",
    val isBlocked: Boolean = false,
    val isVip: Boolean = false, // VIP contacts bypass spam filters
    val lastInteractionTime: Long = 0
)