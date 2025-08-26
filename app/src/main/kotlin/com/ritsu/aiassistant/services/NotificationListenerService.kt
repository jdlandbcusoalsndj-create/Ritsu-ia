package com.ritsu.aiassistant.services

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ritsu.aiassistant.RitsuApplication
import com.ritsu.aiassistant.core.ai.AIEngine
import com.ritsu.aiassistant.core.voice.VoiceEngine
import kotlinx.coroutines.*

class NotificationListenerService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "NotificationListenerService"
    }

    private lateinit var aiEngine: AIEngine
    private lateinit var voiceEngine: VoiceEngine
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val app = application as RitsuApplication
        aiEngine = app.aiEngine
        voiceEngine = app.voiceEngine
        Log.d(TAG, "NotificationListenerService creado")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { handleNotificationPosted(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let { handleNotificationRemoved(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "NotificationListenerService destruido")
    }

    private fun handleNotificationPosted(sbn: StatusBarNotification) {
        try {
            val packageName = sbn.packageName
            val notification = sbn.notification
            
            // Filtrar notificaciones de la propia app
            if (packageName == this.packageName) return
            
            val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
            
            Log.d(TAG, "Notificación de $packageName: $title - $bigText")
            
            // Verificar configuración del usuario
            val prefs = getSharedPreferences("ritsu_prefs", MODE_PRIVATE)
            val handleNotifications = prefs.getBoolean("ritsu_handle_notifications", true)
            val readNotificationsAloud = prefs.getBoolean("read_notifications_aloud", false)
            val smartNotifications = prefs.getBoolean("smart_notifications", true)
            
            if (!handleNotifications) return
            
            serviceScope.launch {
                processNotification(packageName, title, bigText, readNotificationsAloud, smartNotifications)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación", e)
        }
    }

    private fun handleNotificationRemoved(sbn: StatusBarNotification) {
        try {
            Log.d(TAG, "Notificación removida: ${sbn.packageName}")
            // Manejar limpieza si es necesario
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando notificación removida", e)
        }
    }

    private suspend fun processNotification(
        packageName: String,
        title: String,
        text: String,
        readAloud: Boolean,
        smartMode: Boolean
    ) {
        try {
            val importance = determineNotificationImportance(packageName, title, text)
            
            when (importance) {
                NotificationImportance.CRITICAL -> {
                    handleCriticalNotification(packageName, title, text, readAloud)
                }
                NotificationImportance.HIGH -> {
                    handleHighImportanceNotification(packageName, title, text, readAloud)
                }
                NotificationImportance.MEDIUM -> {
                    if (smartMode) {
                        handleMediumImportanceNotification(packageName, title, text, readAloud)
                    }
                }
                NotificationImportance.LOW -> {
                    if (smartMode) {
                        handleLowImportanceNotification(packageName, title, text)
                    }
                }
                NotificationImportance.SPAM -> {
                    handleSpamNotification(packageName, title, text)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación", e)
        }
    }

    private fun determineNotificationImportance(
        packageName: String,
        title: String,
        text: String
    ): NotificationImportance {
        val fullText = "$title $text".lowercase()
        
        // Notificaciones críticas
        val criticalKeywords = listOf(
            "emergency", "emergencia", "urgente", "urgent", "911",
            "llamada perdida", "missed call", "battery low", "batería baja"
        )
        if (criticalKeywords.any { fullText.contains(it) }) {
            return NotificationImportance.CRITICAL
        }
        
        // Aplicaciones importantes
        val highImportanceApps = setOf(
            "com.whatsapp",
            "com.telegram.messenger",
            "com.google.android.gm",
            "com.android.dialer",
            "com.google.android.apps.messaging"
        )
        if (highImportanceApps.contains(packageName)) {
            return NotificationImportance.HIGH
        }
        
        // Detectar spam
        val spamKeywords = listOf(
            "promoción", "descuento", "oferta", "gratis", "premio",
            "felicidades", "ganaste", "click aquí", "limited time"
        )
        if (spamKeywords.any { fullText.contains(it) }) {
            return NotificationImportance.SPAM
        }
        
        // Aplicaciones de redes sociales (importancia media)
        val socialApps = setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.linkedin.android"
        )
        if (socialApps.contains(packageName)) {
            return NotificationImportance.MEDIUM
        }
        
        return NotificationImportance.LOW
    }

    private suspend fun handleCriticalNotification(
        packageName: String,
        title: String,
        text: String,
        readAloud: Boolean
    ) {
        Log.d(TAG, "Notificación crítica: $title")
        
        // Actualizar avatar para mostrar urgencia
        RitsuOverlayService.updateExpression(this, com.ritsu.aiassistant.ui.components.AvatarExpression.SURPRISED)
        RitsuOverlayService.updateAnimation(this, com.ritsu.aiassistant.ui.components.AvatarAnimation.GREETING)
        
        if (readAloud) {
            val announcement = "¡Atención! Notificación importante de ${getAppName(packageName)}: $title"
            voiceEngine.speak(announcement, android.speech.tts.TextToSpeech.QUEUE_FLUSH)
        }
        
        // Crear notificación de alta prioridad para Ritsu
        showRitsuNotification("Notificación Crítica", "$title - $text", true)
    }

    private suspend fun handleHighImportanceNotification(
        packageName: String,
        title: String,
        text: String,
        readAloud: Boolean
    ) {
        Log.d(TAG, "Notificación importante: $title")
        
        // Actualizar avatar
        RitsuOverlayService.updateExpression(this, com.ritsu.aiassistant.ui.components.AvatarExpression.LISTENING)
        
        if (readAloud && isUserAvailable()) {
            val announcement = "Nuevo mensaje de ${getAppName(packageName)}: $title"
            voiceEngine.speak(announcement)
        }
        
        // Analizar si necesita respuesta automática
        if (needsAutoResponse(packageName, text)) {
            generateAutoResponse(packageName, title, text)
        }
    }

    private suspend fun handleMediumImportanceNotification(
        packageName: String,
        title: String,
        text: String,
        readAloud: Boolean
    ) {
        Log.d(TAG, "Notificación media: $title")
        
        if (readAloud && isUserAvailable() && shouldInterrupt()) {
            val announcement = "Notificación de ${getAppName(packageName)}"
            voiceEngine.speak(announcement)
        }
    }

    private suspend fun handleLowImportanceNotification(
        packageName: String,
        title: String,
        text: String
    ) {
        Log.d(TAG, "Notificación baja prioridad: $title")
        // Solo registrar para estadísticas
    }

    private suspend fun handleSpamNotification(
        packageName: String,
        title: String,
        text: String
    ) {
        Log.d(TAG, "Notificación spam detectada: $title")
        // Posiblemente bloquear futuras notificaciones de esta fuente
    }

    private suspend fun generateAutoResponse(packageName: String, title: String, text: String) {
        try {
            when (packageName) {
                "com.whatsapp" -> {
                    // Generar respuesta para WhatsApp
                    val response = aiEngine.generateMessageResponse(text, "", "WhatsApp")
                    Log.d(TAG, "Respuesta sugerida para WhatsApp: $response")
                }
                "com.google.android.gm" -> {
                    // Generar respuesta para Gmail
                    val response = aiEngine.generateMessageResponse(text, "", "Gmail")
                    Log.d(TAG, "Respuesta sugerida para Gmail: $response")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generando respuesta automática", e)
        }
    }

    private fun needsAutoResponse(packageName: String, text: String): Boolean {
        val responseApps = setOf("com.whatsapp", "com.telegram.messenger")
        val questionKeywords = listOf("?", "pregunta", "question", "cómo", "cuándo", "dónde")
        
        return responseApps.contains(packageName) && 
               questionKeywords.any { text.lowercase().contains(it) }
    }

    private suspend fun isUserAvailable(): Boolean {
        // Determinar si el usuario está disponible
        val prefs = getSharedPreferences("ritsu_prefs", MODE_PRIVATE)
        return !prefs.getBoolean("do_not_disturb", false)
    }

    private suspend fun shouldInterrupt(): Boolean {
        // Determinar si es apropiado interrumpir al usuario
        return true // Implementar lógica más sofisticada
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun showRitsuNotification(title: String, content: String, isUrgent: Boolean = false) {
        // Implementar notificación de Ritsu
        Log.d(TAG, "Mostrando notificación de Ritsu: $title")
    }

    enum class NotificationImportance {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        SPAM
    }
}