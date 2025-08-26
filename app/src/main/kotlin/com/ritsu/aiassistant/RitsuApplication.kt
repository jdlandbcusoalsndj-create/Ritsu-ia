package com.ritsu.aiassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.ritsu.aiassistant.data.database.RitsuDatabase
import com.ritsu.aiassistant.core.ai.AIEngine
import com.ritsu.aiassistant.core.voice.VoiceEngine
import com.ritsu.aiassistant.core.avatar.AvatarEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class RitsuApplication : Application() {

    // Coroutine scope para la aplicación
    val applicationScope = CoroutineScope(SupervisorJob())

    // Base de datos
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            RitsuDatabase::class.java,
            "ritsu_database"
        ).build()
    }

    // Motores principales
    val aiEngine by lazy { AIEngine(this) }
    val voiceEngine by lazy { VoiceEngine(this) }
    val avatarEngine by lazy { AvatarEngine(this) }

    companion object {
        const val CHANNEL_ID_OVERLAY = "ritsu_overlay"
        const val CHANNEL_ID_CALLS = "ritsu_calls"
        const val CHANNEL_ID_MESSAGES = "ritsu_messages"
        const val CHANNEL_ID_SYSTEM = "ritsu_system"
        
        const val NOTIFICATION_ID_OVERLAY = 1001
        const val NOTIFICATION_ID_CALL = 1002
        const val NOTIFICATION_ID_MESSAGE = 1003
    }

    override fun onCreate() {
        super.onCreate()
        
        // Crear canales de notificación
        createNotificationChannels()
        
        // Inicializar motores principales
        initializeEngines()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Canal para overlay de Ritsu
            val overlayChannel = NotificationChannel(
                CHANNEL_ID_OVERLAY,
                "Ritsu Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones del overlay de Ritsu"
                setShowBadge(false)
            }
            
            // Canal para llamadas
            val callsChannel = NotificationChannel(
                CHANNEL_ID_CALLS,
                "Manejo de Llamadas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para manejo de llamadas"
            }
            
            // Canal para mensajes
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Mensajes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de mensajes y respuestas automáticas"
            }
            
            // Canal del sistema
            val systemChannel = NotificationChannel(
                CHANNEL_ID_SYSTEM,
                "Sistema",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones del sistema de Ritsu"
            }
            
            notificationManager.createNotificationChannels(listOf(
                overlayChannel, callsChannel, messagesChannel, systemChannel
            ))
        }
    }

    private fun initializeEngines() {
        // Inicializar motores en segundo plano
        Thread {
            try {
                aiEngine.initialize()
                voiceEngine.initialize()
                avatarEngine.initialize()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}