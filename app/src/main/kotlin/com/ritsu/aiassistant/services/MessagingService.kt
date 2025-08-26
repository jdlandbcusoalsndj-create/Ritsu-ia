package com.ritsu.aiassistant.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ritsu.aiassistant.R
import com.ritsu.aiassistant.RitsuApplication
import com.ritsu.aiassistant.core.ai.AIEngine
import com.ritsu.aiassistant.core.voice.VoiceEngine
import com.ritsu.aiassistant.data.model.MessageRecord
import com.ritsu.aiassistant.data.repository.MessageRepository
import com.ritsu.aiassistant.services.RitsuOverlayService
import com.ritsu.aiassistant.ui.components.AvatarExpression
import com.ritsu.aiassistant.ui.components.AvatarAnimation
import kotlinx.coroutines.*

class MessagingService : Service() {
    
    companion object {
        private const val TAG = "MessagingService"
        private const val NOTIFICATION_ID = 1003
        
        const val ACTION_PROCESS_SMS = "process_sms"
        const val ACTION_SEND_RESPONSE = "send_response"
        const val ACTION_READ_MESSAGE = "read_message"
        
        const val EXTRA_SENDER = "sender"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_PLATFORM = "platform"
    }

    private lateinit var aiEngine: AIEngine
    private lateinit var voiceEngine: VoiceEngine
    private lateinit var messageRepository: MessageRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        val app = application as RitsuApplication
        aiEngine = app.aiEngine
        voiceEngine = app.voiceEngine
        messageRepository = MessageRepository(app.database.messageDao())
        
        Log.d(TAG, "MessagingService creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "MessagingService destruido")
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PROCESS_SMS -> {
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: return
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return
                val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                val platform = intent.getStringExtra(EXTRA_PLATFORM) ?: "SMS"
                
                serviceScope.launch {
                    processIncomingMessage(sender, message, timestamp, platform)
                }
            }
            ACTION_SEND_RESPONSE -> {
                val recipient = intent.getStringExtra(EXTRA_SENDER) ?: return
                val response = intent.getStringExtra(EXTRA_MESSAGE) ?: return
                val platform = intent.getStringExtra(EXTRA_PLATFORM) ?: "SMS"
                
                serviceScope.launch {
                    sendMessage(recipient, response, platform)
                }
            }
            ACTION_READ_MESSAGE -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: "Remitente desconocido"
                
                serviceScope.launch {
                    readMessageAloud(sender, message)
                }
            }
        }
    }

    private suspend fun processIncomingMessage(
        sender: String, 
        message: String, 
        timestamp: Long, 
        platform: String
    ) {
        try {
            Log.d(TAG, "Procesando mensaje de $sender en $platform: $message")
            
            // Actualizar avatar para mostrar mensaje entrante
            RitsuOverlayService.updateExpression(this, AvatarExpression.SURPRISED)
            RitsuOverlayService.updateAnimation(this, AvatarAnimation.LISTENING)
            
            // Guardar mensaje entrante
            val messageRecord = MessageRecord(
                sender = sender,
                content = message,
                timestamp = timestamp,
                platform = platform,
                isIncoming = true,
                wasReadByRitsu = false
            )
            messageRepository.insertMessage(messageRecord)
            
            // Verificar configuración del usuario
            val prefs = getSharedPreferences("ritsu_prefs", MODE_PRIVATE)
            val autoRespond = prefs.getBoolean("auto_respond_messages", false)
            val readMessagesAloud = prefs.getBoolean("read_messages_aloud", true)
            val ritsuHandleMessages = prefs.getBoolean("ritsu_handle_messages", true)
            
            if (!ritsuHandleMessages) {
                Log.d(TAG, "Manejo de mensajes por Ritsu deshabilitado")
                return
            }
            
            // Determinar si es spam o mensaje importante
            val messageType = analyzeMessageType(message, sender)
            
            when (messageType) {
                MessageType.SPAM -> {
                    Log.d(TAG, "Mensaje identificado como spam, ignorando")
                    showSpamMessageNotification(sender, message)
                    return
                }
                MessageType.URGENT -> {
                    // Mensaje urgente - leer inmediatamente y notificar
                    if (readMessagesAloud) {
                        readMessageAloud(sender, message)
                    }
                    showUrgentMessageNotification(sender, message)
                    
                    if (autoRespond) {
                        generateAndSendResponse(sender, message, platform, isUrgent = true)
                    }
                }
                MessageType.NORMAL -> {
                    // Mensaje normal
                    if (readMessagesAloud && isUserAvailable()) {
                        readMessageAloud(sender, message)
                    } else {
                        showRegularMessageNotification(sender, message)
                    }
                    
                    if (autoRespond && shouldAutoRespond(sender)) {
                        generateAndSendResponse(sender, message, platform, isUrgent = false)
                    }
                }
                MessageType.PERSONAL -> {
                    // Mensaje personal - siempre notificar pero ser cuidadoso con auto-respuesta
                    if (readMessagesAloud) {
                        readMessageAloud(sender, message)
                    }
                    showPersonalMessageNotification(sender, message)
                }
            }
            
            // Actualizar marca de tiempo de último mensaje para el contacto
            updateContactLastSeen(sender)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje entrante", e)
        }
    }

    private suspend fun generateAndSendResponse(
        sender: String, 
        message: String, 
        platform: String,
        isUrgent: Boolean
    ) {
        try {
            // Obtener contexto del remitente
            val senderInfo = getSenderInfo(sender)
            val conversationHistory = getRecentConversation(sender, platform)
            
            // Generar respuesta usando IA
            val response = aiEngine.generateMessageResponse(message, senderInfo, platform)
            
            // Verificar si la respuesta es apropiada
            if (isResponseAppropriate(response, message, isUrgent)) {
                // Enviar respuesta
                sendMessage(sender, response, platform)
                
                // Actualizar avatar
                RitsuOverlayService.updateExpression(this, AvatarExpression.HAPPY)
                RitsuOverlayService.updateAnimation(this, AvatarAnimation.TALKING)
                
                // Guardar respuesta enviada
                val responseRecord = MessageRecord(
                    sender = sender,
                    content = response,
                    timestamp = System.currentTimeMillis(),
                    platform = platform,
                    isIncoming = false,
                    wasGeneratedByRitsu = true
                )
                messageRepository.insertMessage(responseRecord)
                
                Log.d(TAG, "Respuesta automática enviada a $sender: $response")
                
            } else {
                Log.d(TAG, "Respuesta no apropiada, enviando notificación al usuario")
                showResponseNeededNotification(sender, message)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generando respuesta automática", e)
        }
    }

    private suspend fun sendMessage(recipient: String, message: String, platform: String) {
        try {
            when (platform.uppercase()) {
                "SMS" -> sendSMS(recipient, message)
                "WHATSAPP" -> sendWhatsAppMessage(recipient, message)
                "TELEGRAM" -> sendTelegramMessage(recipient, message)
                else -> {
                    Log.w(TAG, "Plataforma no soportada: $platform")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando mensaje", e)
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            
            // Dividir mensaje largo si es necesario
            val parts = smsManager.divideMessage(message)
            
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            
            Log.d(TAG, "SMS enviado a $phoneNumber")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando SMS", e)
        }
    }

    private suspend fun sendWhatsAppMessage(contact: String, message: String) {
        try {
            // Para WhatsApp necesitarías integración con WhatsApp Business API
            // o usar intents para abrir WhatsApp
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Log.d(TAG, "WhatsApp intent enviado")
            } else {
                Log.w(TAG, "WhatsApp no está instalado")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando mensaje de WhatsApp", e)
        }
    }

    private suspend fun sendTelegramMessage(contact: String, message: String) {
        try {
            // Implementación para Telegram usando Bot API o intent
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Log.d(TAG, "Telegram intent enviado")
            } else {
                Log.w(TAG, "Telegram no está instalado")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando mensaje de Telegram", e)
        }
    }

    private suspend fun readMessageAloud(sender: String, message: String) {
        try {
            val announcement = "Mensaje de $sender: $message"
            voiceEngine.speak(announcement)
            
            // Marcar como leído por Ritsu
            messageRepository.markAsReadByRitsu(sender, message)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo mensaje en voz alta", e)
        }
    }

    private fun analyzeMessageType(message: String, sender: String): MessageType {
        val lowerMessage = message.lowercase()
        
        // Detectar spam
        val spamKeywords = listOf(
            "felicidades", "ganaste", "premio", "promoción", "descuento",
            "gratis", "oferta", "limitada", "urgente", "banco", "tarjeta"
        )
        if (spamKeywords.any { lowerMessage.contains(it) } && !isKnownContact(sender)) {
            return MessageType.SPAM
        }
        
        // Detectar urgencia
        val urgentKeywords = listOf(
            "urgente", "emergencia", "ayuda", "importante", "asap", "ya", "ahora"
        )
        if (urgentKeywords.any { lowerMessage.contains(it) }) {
            return MessageType.URGENT
        }
        
        // Detectar personal
        if (isKnownContact(sender) || isPersonalMessage(message)) {
            return MessageType.PERSONAL
        }
        
        return MessageType.NORMAL
    }

    private fun isKnownContact(sender: String): Boolean {
        // Verificar si está en contactos
        return try {
            // Implementación pendiente - verificar en ContactsContract
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun isPersonalMessage(message: String): Boolean {
        val personalIndicators = listOf(
            "amor", "cariño", "querido", "familia", "papá", "mamá", 
            "hermano", "hermana", "hijo", "hija", "esposo", "esposa"
        )
        return personalIndicators.any { message.lowercase().contains(it) }
    }

    private suspend fun isUserAvailable(): Boolean {
        // Determinar si el usuario está disponible basado en varios factores
        val prefs = getSharedPreferences("ritsu_prefs", MODE_PRIVATE)
        val doNotDisturb = prefs.getBoolean("do_not_disturb", false)
        
        if (doNotDisturb) return false
        
        // Verificar si el usuario está usando el teléfono activamente
        // Esto podría implementarse verificando la última actividad
        return true
    }

    private suspend fun shouldAutoRespond(sender: String): Boolean {
        // Verificar configuración específica del contacto
        val preference = messageRepository.getContactPreference(sender)
        return preference?.autoRespond ?: false
    }

    private fun isResponseAppropriate(response: String, originalMessage: String, isUrgent: Boolean): Boolean {
        // Verificar que la respuesta sea apropiada
        val inappropriateWords = listOf(
            "no sé", "no entiendo", "error", "problema"
        )
        
        return !inappropriateWords.any { response.lowercase().contains(it) }
    }

    private suspend fun getSenderInfo(sender: String): String {
        return try {
            // Obtener información del contacto
            "Contacto: $sender"
        } catch (e: Exception) {
            "Remitente: $sender"
        }
    }

    private suspend fun getRecentConversation(sender: String, platform: String): String {
        return try {
            val recentMessages = messageRepository.getRecentMessages(sender, platform, 5)
            recentMessages.joinToString("\n") { "${if (it.isIncoming) "Ellos" else "Yo"}: ${it.content}" }
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun updateContactLastSeen(sender: String) {
        try {
            messageRepository.updateContactLastSeen(sender, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando último contacto", e)
        }
    }

    // Métodos de notificación
    private fun showSpamMessageNotification(sender: String, message: String) {
        Log.d(TAG, "Mensaje spam bloqueado de $sender")
    }

    private fun showUrgentMessageNotification(sender: String, message: String) {
        Log.d(TAG, "Mensaje urgente de $sender: ${message.take(50)}...")
    }

    private fun showRegularMessageNotification(sender: String, message: String) {
        Log.d(TAG, "Mensaje regular de $sender: ${message.take(50)}...")
    }

    private fun showPersonalMessageNotification(sender: String, message: String) {
        Log.d(TAG, "Mensaje personal de $sender: ${message.take(50)}...")
    }

    private fun showResponseNeededNotification(sender: String, message: String) {
        Log.d(TAG, "Respuesta manual necesaria para $sender")
    }

    enum class MessageType {
        SPAM,
        URGENT,
        NORMAL,
        PERSONAL
    }
    
    companion object {
        fun processMessage(context: Context, sender: String, message: String, platform: String = "SMS") {
            val intent = Intent(context, MessagingService::class.java).apply {
                action = ACTION_PROCESS_SMS
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                putExtra(EXTRA_PLATFORM, platform)
            }
            context.startService(intent)
        }
        
        fun readMessage(context: Context, sender: String, message: String) {
            val intent = Intent(context, MessagingService::class.java).apply {
                action = ACTION_READ_MESSAGE
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_MESSAGE, message)
            }
            context.startService(intent)
        }
    }
}