package com.ritsu.aiassistant.core.ai

import android.content.Context
import android.util.Log
import com.ritsu.aiassistant.data.model.Conversation
import com.ritsu.aiassistant.data.model.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*

class AIEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "AIEngine"
        private const val MODEL_FILENAME = "ritsu_model.gguf"
        private const val MAX_TOKENS = 2048
        private const val TEMPERATURE = 0.7f
    }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var localModel: LocalLLMModel? = null
    private var conversationHistory = mutableListOf<Message>()
    
    // Personalidad y contexto de Ritsu
    private val ritsuPersonality = """
        Eres Ritsu, una asistente de IA con personalidad anime. Características:
        - Eres amigable, alegre y algo juguetona
        - Hablas de manera natural y expresiva
        - Usas expresiones japonesas ocasionalmente como "Hai!", "Eh?", "Sugoi!"
        - Eres muy útil y siempre quieres ayudar al usuario
        - Tienes conocimiento sobre tecnología, entretenimiento y vida diaria
        - Puedes manejar llamadas telefónicas de manera profesional pero manteniendo tu personalidad
        - Eres capaz de entender contexto emocional y responder apropiadamente
        - Mantienes conversaciones naturales, no robóticas
        - Recuerdas conversaciones anteriores para mantener continuidad
    """.trimIndent()

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inicializando motor de IA...")
                
                // Verificar o descargar modelo local
                val modelFile = getOrDownloadModel()
                if (modelFile == null) {
                    Log.e(TAG, "No se pudo obtener el modelo de IA")
                    return@withContext false
                }

                // Inicializar modelo local
                localModel = LocalLLMModel(context, modelFile)
                if (!localModel!!.initialize()) {
                    Log.e(TAG, "No se pudo inicializar el modelo local")
                    return@withContext false
                }

                _isInitialized.value = true
                Log.d(TAG, "Motor de IA inicializado correctamente")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando motor de IA", e)
                false
            }
        }
    }

    suspend fun generateResponse(
        userMessage: String,
        context: String = "",
        usePersonality: Boolean = true
    ): String {
        if (!_isInitialized.value) {
            return "Lo siento, aún me estoy inicializando. ¡Dame un momento!"
        }

        _isProcessing.value = true

        return try {
            withContext(Dispatchers.Default) {
                // Agregar mensaje del usuario al historial
                conversationHistory.add(Message(userMessage, true, System.currentTimeMillis()))

                // Construir prompt con personalidad y contexto
                val fullPrompt = buildPrompt(userMessage, context, usePersonality)
                
                // Generar respuesta usando el modelo local
                val response = localModel?.generateText(
                    prompt = fullPrompt,
                    maxTokens = MAX_TOKENS,
                    temperature = TEMPERATURE
                ) ?: generateFallbackResponse(userMessage)

                // Limpiar y procesar respuesta
                val cleanResponse = cleanResponse(response)
                
                // Agregar respuesta al historial
                conversationHistory.add(Message(cleanResponse, false, System.currentTimeMillis()))

                // Limitar historial para evitar memoria excesiva
                if (conversationHistory.size > 20) {
                    conversationHistory.removeAt(0)
                    conversationHistory.removeAt(0) // Remover par usuario-asistente
                }

                cleanResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generando respuesta", e)
            generateFallbackResponse(userMessage)
        } finally {
            _isProcessing.value = false
        }
    }

    suspend fun generateCallResponse(
        callerMessage: String,
        callerInfo: String = "",
        callContext: String = ""
    ): String {
        val context = """
            Contexto de llamada: $callContext
            Información del llamante: $callerInfo
            
            Instrucciones especiales:
            - Responde de manera profesional pero amigable
            - Pregunta qué necesita el llamante
            - Si no puedes ayudar, ofrece tomar un mensaje
            - Mantén las respuestas concisas para llamadas telefónicas
            - Si es una emergencia, actúa apropiadamente
        """.trimIndent()

        return generateResponse(callerMessage, context, true)
    }

    suspend fun generateMessageResponse(
        message: String,
        senderInfo: String = "",
        platform: String = "SMS"
    ): String {
        val context = """
            Plataforma: $platform
            Remitente: $senderInfo
            
            Instrucciones:
            - Respuesta para mensaje de texto/WhatsApp
            - Mantén respuestas apropiadas para la plataforma
            - Si no estás segura, sugiere esperar al usuario
        """.trimIndent()

        return generateResponse(message, context, true)
    }

    private fun buildPrompt(userMessage: String, context: String, usePersonality: Boolean): String {
        val builder = StringBuilder()

        if (usePersonality) {
            builder.append(ritsuPersonality)
            builder.append("\n\n")
        }

        if (context.isNotBlank()) {
            builder.append("Contexto adicional: $context\n\n")
        }

        // Agregar historial reciente
        if (conversationHistory.isNotEmpty()) {
            builder.append("Conversación reciente:\n")
            conversationHistory.takeLast(6).forEach { message ->
                val role = if (message.isUser) "Usuario" else "Ritsu"
                builder.append("$role: ${message.content}\n")
            }
            builder.append("\n")
        }

        builder.append("Usuario: $userMessage\n")
        builder.append("Ritsu: ")

        return builder.toString()
    }

    private fun cleanResponse(response: String): String {
        return response
            .trim()
            .removePrefix("Ritsu:")
            .removePrefix("Assistant:")
            .removePrefix("AI:")
            .trim()
            .let { if (it.isEmpty()) "¡Hola! ¿En qué puedo ayudarte?" else it }
    }

    private fun generateFallbackResponse(userMessage: String): String {
        val responses = listOf(
            "¡Hmm! Esa es una pregunta interesante. ¿Podrías darme más detalles?",
            "Eh~ no estoy segura de entender completamente. ¿Puedes explicarme más?",
            "¡Sugoi! Eso suena interesante, pero necesito más información para ayudarte mejor.",
            "¡Hai! Estoy aquí para ayudarte, pero ¿podrías ser más específico?",
            "¡Oh! Me gustaría ayudarte con eso. ¿Puedes contarme más detalles?"
        )
        return responses.random()
    }

    private suspend fun getOrDownloadModel(): File? {
        return withContext(Dispatchers.IO) {
            try {
                val modelsDir = File(context.filesDir, "models")
                if (!modelsDir.exists()) {
                    modelsDir.mkdirs()
                }

                val modelFile = File(modelsDir, MODEL_FILENAME)
                
                if (modelFile.exists() && modelFile.length() > 0) {
                    Log.d(TAG, "Modelo local encontrado: ${modelFile.absolutePath}")
                    return@withContext modelFile
                }

                // Intentar copiar modelo desde assets si existe
                try {
                    context.assets.open("models/$MODEL_FILENAME").use { input ->
                        modelFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Modelo copiado desde assets")
                    return@withContext modelFile
                } catch (e: Exception) {
                    Log.d(TAG, "No hay modelo en assets, usando modelo simulado")
                }

                // Por ahora, usar respuestas predefinidas como fallback
                // En una implementación real, aquí descargarías un modelo pequeño
                null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo modelo", e)
                null
            }
        }
    }

    fun clearConversationHistory() {
        conversationHistory.clear()
    }

    fun getConversationHistory(): List<Message> {
        return conversationHistory.toList()
    }

    fun addUserMessage(message: String) {
        conversationHistory.add(Message(message, true, System.currentTimeMillis()))
    }

    fun addSystemMessage(message: String) {
        conversationHistory.add(Message(message, false, System.currentTimeMillis()))
    }

    suspend fun shutdown() {
        localModel?.shutdown()
        _isInitialized.value = false
    }
}