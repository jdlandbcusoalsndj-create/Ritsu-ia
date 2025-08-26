package com.ritsu.aiassistant.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.telecom.*
import android.telecom.Connection.VideoProvider
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ritsu.aiassistant.R
import com.ritsu.aiassistant.RitsuApplication
import com.ritsu.aiassistant.core.ai.AIEngine
import com.ritsu.aiassistant.core.voice.VoiceEngine
import com.ritsu.aiassistant.data.model.CallRecord
import com.ritsu.aiassistant.data.repository.CallRepository
import kotlinx.coroutines.*

class CallHandlingService : ConnectionService() {
    
    companion object {
        private const val TAG = "CallHandlingService"
        private const val NOTIFICATION_ID = 1002
    }

    private lateinit var aiEngine: AIEngine
    private lateinit var voiceEngine: VoiceEngine
    private lateinit var callRepository: CallRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val activeConnections = mutableMapOf<String, RitsuConnection>()

    override fun onCreate() {
        super.onCreate()
        
        val app = application as RitsuApplication
        aiEngine = app.aiEngine
        voiceEngine = app.voiceEngine
        callRepository = CallRepository(app.database.callDao())
        
        Log.d(TAG, "CallHandlingService creado")
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "Llamada entrante detectada")
        
        val connection = RitsuConnection(this, request?.address?.schemeSpecificPart ?: "Unknown")
        connection.setConnectionProperties(Connection.PROPERTY_SELF_MANAGED)
        connection.setAudioModeIsVoip(true)
        
        // Registrar conexión activa
        activeConnections[connection.callId] = connection
        
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "Llamada saliente detectada")
        
        val connection = RitsuConnection(this, request?.address?.schemeSpecificPart ?: "Unknown")
        connection.setConnectionProperties(Connection.PROPERTY_SELF_MANAGED)
        connection.setAudioModeIsVoip(true)
        
        // Registrar conexión activa
        activeConnections[connection.callId] = connection
        
        return connection
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "CallHandlingService destruido")
    }

    inner class RitsuConnection(
        private val context: Context,
        private val phoneNumber: String
    ) : Connection() {
        
        val callId = "call_${System.currentTimeMillis()}"
        private var isRitsuHandling = false
        private var conversationHistory = mutableListOf<String>()
        private var callStartTime = 0L
        private var isRecording = false
        
        init {
            Log.d(TAG, "Nueva conexión creada para: $phoneNumber")
            
            // Configurar capacidades de la conexión
            connectionCapabilities = CAPABILITY_SUPPORT_HOLD or 
                                   CAPABILITY_MUTE or 
                                   CAPABILITY_SUPPORTS_VT_LOCAL_RX or 
                                   CAPABILITY_SUPPORTS_VT_LOCAL_TX
            
            // Configurar información del llamante
            setCallerDisplayName(phoneNumber, TelecomManager.PRESENTATION_ALLOWED)
            setAddress(android.net.Uri.fromParts("tel", phoneNumber, null), TelecomManager.PRESENTATION_ALLOWED)
        }

        override fun onAnswer() {
            super.onAnswer()
            Log.d(TAG, "Llamada contestada para: $phoneNumber")
            
            callStartTime = System.currentTimeMillis()
            setActive()
            
            // Iniciar manejo de Ritsu
            serviceScope.launch {
                startRitsuCallHandling()
            }
        }

        override fun onReject() {
            super.onReject()
            Log.d(TAG, "Llamada rechazada para: $phoneNumber")
            
            serviceScope.launch {
                // Guardar registro de llamada rechazada
                saveCallRecord("Llamada rechazada", CallRecord.CallType.MISSED)
            }
            
            cleanup()
        }

        override fun onDisconnect() {
            super.onDisconnect()
            Log.d(TAG, "Llamada desconectada para: $phoneNumber")
            
            serviceScope.launch {
                endRitsuCallHandling()
            }
            
            cleanup()
        }

        override fun onHold() {
            super.onHold()
            Log.d(TAG, "Llamada en espera")
            setOnHold()
        }

        override fun onUnhold() {
            super.onUnhold()
            Log.d(TAG, "Llamada reanudada")
            setActive()
        }

        override fun onPlayDtmfTone(c: Char) {
            Log.d(TAG, "Tono DTMF: $c")
        }

        private suspend fun startRitsuCallHandling() {
            try {
                Log.d(TAG, "Iniciando manejo de llamada por Ritsu")
                
                isRitsuHandling = true
                
                // Mostrar notificación de llamada activa
                showCallNotification()
                
                // Saludar al llamante
                val greeting = generateGreeting()
                voiceEngine.speak(greeting)
                conversationHistory.add("Ritsu: $greeting")
                
                // Iniciar escucha continua
                startListeningLoop()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando manejo de llamada", e)
                onDisconnect()
            }
        }

        private suspend fun startListeningLoop() {
            while (isRitsuHandling && state == STATE_ACTIVE) {
                try {
                    // Iniciar reconocimiento de voz
                    voiceEngine.startListening()
                    
                    // Esperar a que se detecte voz o timeout
                    val spokenText = waitForSpeech(10000) // 10 segundos timeout
                    
                    if (spokenText.isNotBlank()) {
                        Log.d(TAG, "Llamante dijo: $spokenText")
                        conversationHistory.add("Llamante: $spokenText")
                        
                        // Generar respuesta usando IA
                        val response = generateCallResponse(spokenText)
                        
                        // Responder
                        voiceEngine.speak(response)
                        conversationHistory.add("Ritsu: $response")
                        
                        // Verificar si la llamada debe terminar
                        if (shouldEndCall(spokenText, response)) {
                            endCallGracefully()
                            break
                        }
                    } else {
                        // Timeout o silencio prolongado
                        handleSilence()
                    }
                    
                    delay(500) // Pequeña pausa entre ciclos
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error en loop de escucha", e)
                    break
                }
            }
        }

        private suspend fun waitForSpeech(timeoutMs: Long): String {
            return try {
                withTimeout(timeoutMs) {
                    var recognizedText = ""
                    
                    // Monitorear reconocimiento de voz
                    val job = serviceScope.launch {
                        voiceEngine.recognizedText.collect { text ->
                            if (text.isNotBlank()) {
                                recognizedText = text
                            }
                        }
                    }
                    
                    // Esperar hasta que se reconozca algo o timeout
                    while (recognizedText.isBlank() && isActive) {
                        delay(100)
                    }
                    
                    job.cancel()
                    voiceEngine.stopListening()
                    
                    recognizedText
                }
            } catch (e: TimeoutCancellationException) {
                voiceEngine.stopListening()
                ""
            }
        }

        private suspend fun generateGreeting(): String {
            val callerInfo = getCallerInfo(phoneNumber)
            val context = """
                Llamada telefónica entrante.
                Número: $phoneNumber
                Información del contacto: $callerInfo
                
                Instrucciones:
                - Saluda de manera profesional pero amigable
                - Pregunta en qué puedes ayudar
                - Mantén el saludo breve y natural
            """.trimIndent()
            
            return aiEngine.generateCallResponse("", callerInfo, context)
        }

        private suspend fun generateCallResponse(callerMessage: String): String {
            val callerInfo = getCallerInfo(phoneNumber)
            val conversationContext = conversationHistory.takeLast(6).joinToString("\n")
            
            val context = """
                Conversación telefónica en curso.
                Historial reciente:
                $conversationContext
                
                Instrucciones especiales:
                - Mantén respuestas naturales y conversacionales
                - Si preguntan por el usuario, explica que no está disponible
                - Ofrece tomar un mensaje si es apropiado
                - Si es urgente, pregunta detalles
                - Si es spam o ventas, termina la llamada educadamente
                - Mantén el tono profesional pero amigable
            """.trimIndent()
            
            return aiEngine.generateCallResponse(callerMessage, callerInfo, context)
        }

        private suspend fun handleSilence() {
            val silenceResponses = listOf(
                "¿Sigues ahí? ¿En qué puedo ayudarte?",
                "¿Hay algo específico que necesites?",
                "¿Me escuchas bien? ¿Puedo ayudarte en algo?"
            )
            
            val response = silenceResponses.random()
            voiceEngine.speak(response)
            conversationHistory.add("Ritsu: $response")
        }

        private fun shouldEndCall(callerMessage: String, ritsuResponse: String): Boolean {
            val endPhrases = listOf(
                "adiós", "chao", "hasta luego", "gracias", "colgar",
                "terminar", "fin", "bye", "goodbye"
            )
            
            return endPhrases.any { phrase ->
                callerMessage.contains(phrase, ignoreCase = true) ||
                ritsuResponse.contains(phrase, ignoreCase = true)
            }
        }

        private suspend fun endCallGracefully() {
            val farewell = "¡Perfecto! Que tengas un buen día. ¡Hasta luego!"
            voiceEngine.speak(farewell)
            conversationHistory.add("Ritsu: $farewell")
            
            delay(2000) // Esperar a que termine de hablar
            onDisconnect()
        }

        private suspend fun endRitsuCallHandling() {
            try {
                isRitsuHandling = false
                voiceEngine.stopListening()
                
                // Guardar registro de la llamada
                val callDuration = System.currentTimeMillis() - callStartTime
                val summary = generateCallSummary()
                saveCallRecord(summary, CallRecord.CallType.INCOMING, callDuration)
                
                Log.d(TAG, "Llamada finalizada. Duración: ${callDuration / 1000}s")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizando manejo de llamada", e)
            }
        }

        private fun generateCallSummary(): String {
            return if (conversationHistory.isNotEmpty()) {
                "Resumen de llamada:\n${conversationHistory.joinToString("\n")}"
            } else {
                "Llamada sin conversación registrada"
            }
        }

        private suspend fun saveCallRecord(summary: String, type: CallRecord.CallType, duration: Long = 0) {
            try {
                val callRecord = CallRecord(
                    phoneNumber = phoneNumber,
                    callType = type,
                    timestamp = System.currentTimeMillis(),
                    duration = duration,
                    wasHandledByRitsu = isRitsuHandling,
                    summary = summary,
                    conversationLog = conversationHistory.joinToString("\n")
                )
                
                callRepository.insertCall(callRecord)
                Log.d(TAG, "Registro de llamada guardado")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error guardando registro de llamada", e)
            }
        }

        private fun getCallerInfo(phoneNumber: String): String {
            // Obtener información del contacto si existe
            return try {
                // Aquí integrarías con ContactsContract para obtener el nombre
                "Número: $phoneNumber"
            } catch (e: Exception) {
                "Número desconocido: $phoneNumber"
            }
        }

        private fun showCallNotification() {
            val notification = NotificationCompat.Builder(context, RitsuApplication.CHANNEL_ID_CALLS)
                .setContentTitle("Ritsu manejando llamada")
                .setContentText("Conversando con $phoneNumber")
                .setSmallIcon(R.drawable.ic_phone)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            
            // Mostrar notificación (necesitaría acceso al NotificationManager)
        }

        private fun cleanup() {
            activeConnections.remove(callId)
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            destroy()
        }
    }
}