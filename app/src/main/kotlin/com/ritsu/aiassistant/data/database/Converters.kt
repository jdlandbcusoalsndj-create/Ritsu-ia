package com.ritsu.aiassistant.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ritsu.aiassistant.data.model.CallRecord
import com.ritsu.aiassistant.data.model.MessageType
import com.ritsu.aiassistant.data.model.ParticipantType

class Converters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromCallType(callType: CallRecord.CallType): String {
        return callType.name
    }
    
    @TypeConverter
    fun toCallType(callType: String): CallRecord.CallType {
        return CallRecord.CallType.valueOf(callType)
    }
    
    @TypeConverter
    fun fromMessageType(messageType: MessageType): String {
        return messageType.name
    }
    
    @TypeConverter
    fun toMessageType(messageType: String): MessageType {
        return MessageType.valueOf(messageType)
    }
    
    @TypeConverter
    fun fromParticipantType(participantType: ParticipantType): String {
        return participantType.name
    }
    
    @TypeConverter
    fun toParticipantType(participantType: String): ParticipantType {
        return ParticipantType.valueOf(participantType)
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }
}