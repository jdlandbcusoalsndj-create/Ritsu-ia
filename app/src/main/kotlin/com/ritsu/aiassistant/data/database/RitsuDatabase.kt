package com.ritsu.aiassistant.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.ritsu.aiassistant.data.dao.*
import com.ritsu.aiassistant.data.model.*

@Database(
    entities = [
        Message::class,
        Conversation::class,
        CallRecord::class,
        CallPreference::class,
        MessageRecord::class,
        ContactPreference::class,
        AutoResponse::class,
        ConversationContext::class,
        UserPreference::class,
        AppUsageRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RitsuDatabase : RoomDatabase() {
    
    abstract fun messageDao(): MessageDao
    abstract fun callDao(): CallDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun appUsageDao(): AppUsageDao
    
    companion object {
        @Volatile
        private var INSTANCE: RitsuDatabase? = null
        
        fun getDatabase(context: Context): RitsuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RitsuDatabase::class.java,
                    "ritsu_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}