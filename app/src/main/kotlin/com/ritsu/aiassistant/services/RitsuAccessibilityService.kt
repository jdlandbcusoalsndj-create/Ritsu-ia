package com.ritsu.aiassistant.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ritsu.aiassistant.RitsuApplication
import com.ritsu.aiassistant.core.ai.AIEngine
import com.ritsu.aiassistant.services.RitsuOverlayService
import com.ritsu.aiassistant.ui.components.AvatarExpression
import com.ritsu.aiassistant.ui.components.AvatarAnimation
import kotlinx.coroutines.*

class RitsuAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RitsuAccessibilityService"
    }

    private lateinit var aiEngine: AIEngine
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val app = application as RitsuApplication
        aiEngine = app.aiEngine
        Log.d(TAG, "RitsuAccessibilityService creado")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Servicio de accesibilidad conectado")
        
        // Actualizar avatar para mostrar que está activo
        RitsuOverlayService.updateExpression(this, AvatarExpression.HAPPY)
        RitsuOverlayService.updateAnimation(this, AvatarAnimation.GREETING)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { handleAccessibilityEvent(it) }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Servicio de accesibilidad interrumpido")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "RitsuAccessibilityService destruido")
    }

    private fun handleAccessibilityEvent(event: AccessibilityEvent) {
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                    handleNotification(event)
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    handleWindowChange(event)
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    handleViewClick(event)
                }
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    handleTextChange(event)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando evento de accesibilidad", e)
        }
    }

    private fun handleNotification(event: AccessibilityEvent) {
        try {
            val packageName = event.packageName?.toString() ?: return
            val text = event.text?.joinToString(" ") ?: return
            
            Log.d(TAG, "Notificación de $packageName: $text")
            
            // Verificar configuración del usuario
            val prefs = getSharedPreferences("ritsu_prefs", MODE_PRIVATE)
            val readNotifications = prefs.getBoolean("read_notifications_aloud", false)
            val handleNotifications = prefs.getBoolean("ritsu_handle_notifications", true)
            
            if (!handleNotifications) return
            
            // Filtrar notificaciones importantes
            if (isImportantNotification(packageName, text)) {
                if (readNotifications) {
                    serviceScope.launch {
                        val announcement = "Nueva notificación de ${getAppName(packageName)}: $text"
                        // Aquí integrarías con VoiceEngine para leer en voz alta
                        Log.d(TAG, "Leyendo notificación: $announcement")
                    }
                }
                
                // Actualizar avatar para mostrar notificación
                RitsuOverlayService.updateExpression(this, AvatarExpression.SURPRISED)
                RitsuOverlayService.updateAnimation(this, AvatarAnimation.LISTENING)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando notificación", e)
        }
    }

    private fun handleWindowChange(event: AccessibilityEvent) {
        try {
            val packageName = event.packageName?.toString() ?: return
            val className = event.className?.toString() ?: return
            
            Log.d(TAG, "Cambio de ventana: $packageName - $className")
            
            // Registrar uso de aplicación
            recordAppUsage(packageName)
            
            // Actualizar contexto de Ritsu
            updateRitsuContext(packageName, className)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando cambio de ventana", e)
        }
    }

    private fun handleViewClick(event: AccessibilityEvent) {
        try {
            val contentDescription = event.contentDescription?.toString()
            val text = event.text?.joinToString(" ")
            
            if (contentDescription != null || text != null) {
                Log.d(TAG, "Click detectado: $contentDescription - $text")
                
                // Analizar si es una acción relevante para Ritsu
                analyzeUserAction(contentDescription, text, event.packageName?.toString())
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando click", e)
        }
    }

    private fun handleTextChange(event: AccessibilityEvent) {
        try {
            val text = event.text?.joinToString(" ") ?: return
            
            // Detectar si el usuario está escribiendo algo relevante
            if (isRelevantTextInput(text, event.packageName?.toString())) {
                Log.d(TAG, "Texto relevante detectado: ${text.take(50)}...")
                
                // Aquí podrías ofrecer sugerencias o ayuda contextual
                offerContextualHelp(text, event.packageName?.toString())
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando cambio de texto", e)
        }
    }

    private fun isImportantNotification(packageName: String, text: String): Boolean {
        val importantApps = setOf(
            "com.whatsapp",
            "com.telegram.messenger",
            "com.google.android.gm",
            "com.android.dialer",
            "com.google.android.apps.messaging"
        )
        
        val urgentKeywords = listOf(
            "urgente", "importante", "emergency", "llamada perdida"
        )
        
        return importantApps.contains(packageName) || 
               urgentKeywords.any { text.lowercase().contains(it) }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun recordAppUsage(packageName: String) {
        serviceScope.launch {
            try {
                // Registrar uso de aplicación en la base de datos
                // Implementación pendiente con AppUsageDao
                Log.d(TAG, "Registrando uso de app: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error registrando uso de app", e)
            }
        }
    }

    private fun updateRitsuContext(packageName: String, className: String) {
        serviceScope.launch {
            try {
                // Actualizar el contexto de Ritsu sobre qué está haciendo el usuario
                val context = "Usuario está usando $packageName"
                Log.d(TAG, "Actualizando contexto de Ritsu: $context")
                
                // Cambiar expresión del avatar según la app
                when (packageName) {
                    "com.android.camera", "com.google.android.GoogleCamera" -> {
                        RitsuOverlayService.updateExpression(this@RitsuAccessibilityService, AvatarExpression.HAPPY)
                    }
                    "com.spotify.music", "com.google.android.youtube" -> {
                        RitsuOverlayService.updateAnimation(this@RitsuAccessibilityService, AvatarAnimation.TALKING)
                    }
                    "com.whatsapp", "com.telegram.messenger" -> {
                        RitsuOverlayService.updateExpression(this@RitsuAccessibilityService, AvatarExpression.LISTENING)
                    }
                    else -> {
                        RitsuOverlayService.updateExpression(this@RitsuAccessibilityService, AvatarExpression.NEUTRAL)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando contexto", e)
            }
        }
    }

    private fun analyzeUserAction(contentDescription: String?, text: String?, packageName: String?) {
        serviceScope.launch {
            try {
                // Analizar si la acción del usuario sugiere que necesita ayuda
                val actionContext = "$contentDescription $text"
                
                if (suggestsNeedForHelp(actionContext)) {
                    Log.d(TAG, "Usuario podría necesitar ayuda con: $actionContext")
                    
                    // Activar Ritsu para ofrecer ayuda
                    RitsuOverlayService.updateExpression(this@RitsuAccessibilityService, AvatarExpression.THINKING)
                    RitsuOverlayService.updateAnimation(this@RitsuAccessibilityService, AvatarAnimation.THINKING)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error analizando acción del usuario", e)
            }
        }
    }

    private fun isRelevantTextInput(text: String, packageName: String?): Boolean {
        // Detectar si el texto de entrada es relevante para Ritsu
        val relevantKeywords = listOf(
            "ritsu", "help", "ayuda", "asistente", "hey ritsu"
        )
        
        return relevantKeywords.any { text.lowercase().contains(it) } ||
               (packageName?.contains("messaging") == true && text.length > 10)
    }

    private fun offerContextualHelp(text: String, packageName: String?) {
        serviceScope.launch {
            try {
                // Ofrecer ayuda contextual basada en lo que está escribiendo el usuario
                when {
                    text.contains("email", ignoreCase = true) -> {
                        Log.d(TAG, "Usuario escribiendo email, ofreciendo ayuda")
                    }
                    text.contains("mensaje", ignoreCase = true) -> {
                        Log.d(TAG, "Usuario escribiendo mensaje, ofreciendo ayuda")
                    }
                    text.length > 100 -> {
                        Log.d(TAG, "Texto largo detectado, ofreciendo ayuda de escritura")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error ofreciendo ayuda contextual", e)
            }
        }
    }

    private fun suggestsNeedForHelp(actionContext: String): Boolean {
        val helpIndicators = listOf(
            "error", "problema", "no funciona", "help", "ayuda",
            "cómo", "where", "dónde", "búsqueda"
        )
        
        return helpIndicators.any { actionContext.lowercase().contains(it) }
    }

    // Métodos públicos para interacción externa
    fun performGlobalAction(action: Int): Boolean {
        return performGlobalAction(action)
    }

    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        return findNodeByText(rootNode, text)
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            if (result != null) {
                child.recycle()
                return result
            }
            child.recycle()
        }
        
        return null
    }

    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun openApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo app: $packageName", e)
            false
        }
    }
}