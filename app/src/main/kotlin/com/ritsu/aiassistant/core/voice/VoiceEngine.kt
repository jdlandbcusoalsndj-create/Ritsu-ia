package com.ritsu.aiassistant.core.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*

class VoiceEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceEngine"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val WAKE_WORD = "hey ritsu"
    }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: OfflineSpeechRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private var wakeWordDetector: WakeWordDetector? = null
    
    private var isRecording = false
    private var recordingJob: Job? = null

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "Inicializando motor de voz...")
                
                // Inicializar TTS
                if (!initializeTTS()) {
                    Log.e(TAG, "Error inicializando TTS")
                    return@withContext false
                }

                // Inicializar reconocimiento de voz offline
                speechRecognizer = OfflineSpeechRecognizer(context)
                if (!speechRecognizer!!.initialize()) {
                    Log.e(TAG, "Error inicializando reconocimiento de voz")
                    return@withContext false
                }

                // Inicializar detector de wake word
                wakeWordDetector = WakeWordDetector(context)
                if (!wakeWordDetector!!.initialize()) {
                    Log.e(TAG, "Error inicializando detector de wake word")
                    return@withContext false
                }

                _isInitialized.value = true
                Log.d(TAG, "Motor de voz inicializado correctamente")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando motor de voz", e)
                false
            }
        }
    }

    private suspend fun initializeTTS(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            textToSpeech = TextToSpeech(context) { status ->
                when (status) {
                    TextToSpeech.SUCCESS -> {
                        // Configurar idioma español con fallback a inglés
                        val spanishResult = textToSpeech?.setLanguage(Locale("es", "ES"))
                        if (spanishResult == TextToSpeech.LANG_MISSING_DATA || 
                            spanishResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            textToSpeech?.setLanguage(Locale.US)
                        }
                        
                        // Configurar listener para saber cuándo termina de hablar
                        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
                            }
                            
                            override fun onDone(utteranceId: String?) {
                                _isSpeaking.value = false
                            }
                            
                            override fun onError(utteranceId: String?) {
                                _isSpeaking.value = false
                            }
                        })
                        
                        continuation.resume(true) {}
                    }
                    else -> {
                        Log.e(TAG, "Error inicializando TTS: $status")
                        continuation.resume(false) {}
                    }
                }
            }
        }
    }

    suspend fun speak(text: String, priority: Int = TextToSpeech.QUEUE_FLUSH): Boolean {
        return withContext(Dispatchers.Main) {
            if (!_isInitialized.value || textToSpeech == null) {
                Log.w(TAG, "TTS no inicializado")
                return@withContext false
            }

            try {
                val utteranceId = "ritsu_${System.currentTimeMillis()}"
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                
                val result = textToSpeech?.speak(text, priority, params, utteranceId)
                result == TextToSpeech.SUCCESS
                
            } catch (e: Exception) {
                Log.e(TAG, "Error en TTS", e)
                false
            }
        }
    }

    suspend fun startListening(): Boolean {
        if (!_isInitialized.value || _isListening.value) {
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                // Configurar AudioRecord
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
                )
                
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord no se pudo inicializar")
                    return@withContext false
                }

                audioRecord?.startRecording()
                isRecording = true
                _isListening.value = true

                // Comenzar grabación en background
                recordingJob = CoroutineScope(Dispatchers.IO).launch {
                    processAudioStream()
                }

                Log.d(TAG, "Inicio de escucha exitoso")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando escucha", e)
                false
            }
        }
    }

    suspend fun stopListening() {
        _isListening.value = false
        isRecording = false
        recordingJob?.cancel()

        withContext(Dispatchers.IO) {
            try {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                Log.d(TAG, "Escucha detenida")
            } catch (e: Exception) {
                Log.e(TAG, "Error deteniendo escucha", e)
            }
        }
    }

    private suspend fun processAudioStream() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        )
        val audioBuffer = ShortArray(bufferSize)
        val audioData = mutableListOf<Short>()

        while (isRecording && audioRecord != null) {
            val readResult = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
            
            if (readResult > 0) {
                // Agregar datos al buffer
                audioData.addAll(audioBuffer.take(readResult))
                
                // Procesar en chunks para detección de wake word y reconocimiento
                if (audioData.size >= SAMPLE_RATE) { // 1 segundo de audio
                    val chunk = audioData.take(SAMPLE_RATE).toShortArray()
                    audioData.clear()
                    
                    // Detectar wake word
                    if (wakeWordDetector?.detectWakeWord(chunk) == true) {
                        Log.d(TAG, "Wake word detectada!")
                        handleWakeWordDetected()
                    }
                    
                    // Reconocimiento continuo de voz
                    speechRecognizer?.processAudioChunk(chunk)?.let { text ->
                        if (text.isNotBlank()) {
                            _recognizedText.value = text
                            Log.d(TAG, "Texto reconocido: $text")
                        }
                    }
                }
            }
            
            delay(10) // Pequeña pausa para evitar uso excesivo de CPU
        }
    }

    private suspend fun handleWakeWordDetected() {
        withContext(Dispatchers.Main) {
            // Activar Ritsu visualmente
            // Esto se conectará con el overlay service
            _recognizedText.value = "¡Hey Ritsu detectado!"
            
            // Opcional: respuesta de confirmación
            speak("¡Hai! ¿En qué puedo ayudarte?")
        }
    }

    suspend fun recognizeSpeech(audioFile: File): String? {
        return withContext(Dispatchers.IO) {
            speechRecognizer?.recognizeFile(audioFile)
        }
    }

    suspend fun processCommand(command: String): String? {
        return withContext(Dispatchers.Default) {
            when {
                command.contains("para", ignoreCase = true) || 
                command.contains("detente", ignoreCase = true) -> {
                    if (_isSpeaking.value) {
                        textToSpeech?.stop()
                        "¡Hai! Me detengo."
                    } else null
                }
                
                command.contains("silencio", ignoreCase = true) -> {
                    textToSpeech?.stop()
                    stopListening()
                    "Modo silencio activado."
                }
                
                command.contains("escucha", ignoreCase = true) -> {
                    startListening()
                    "¡Estoy escuchando!"
                }
                
                else -> null
            }
        }
    }

    fun setLanguage(language: Locale): Boolean {
        return try {
            val result = textToSpeech?.setLanguage(language)
            result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
        } catch (e: Exception) {
            Log.e(TAG, "Error cambiando idioma", e)
            false
        }
    }

    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch)
    }

    suspend fun shutdown() {
        stopListening()
        
        withContext(Dispatchers.Main) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
        
        speechRecognizer?.shutdown()
        wakeWordDetector?.shutdown()
        
        _isInitialized.value = false
        Log.d(TAG, "Motor de voz cerrado")
    }
}