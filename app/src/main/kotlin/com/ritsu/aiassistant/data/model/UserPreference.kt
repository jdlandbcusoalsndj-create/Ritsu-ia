package com.ritsu.aiassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey
    val key: String,
    val value: String,
    val type: PreferenceType,
    val category: String = "general",
    val lastModified: Long = System.currentTimeMillis()
) {
    enum class PreferenceType {
        BOOLEAN,
        STRING,
        INT,
        FLOAT,
        JSON
    }
}

@Entity(tableName = "app_usage_records")
data class AppUsageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val openTime: Long,
    val closeTime: Long = 0,
    val usageDuration: Long = 0, // en milisegundos
    val wasOpenedByRitsu: Boolean = false,
    val category: String = "other"
)