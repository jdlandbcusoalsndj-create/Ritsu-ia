package com.ritsu.aiassistant.core.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * Detector de wake word "Hey Ritsu" usando análisis de patrones de audio
 * Implementación simplificada que en un proyecto real usaría Porcupine, 
 * Snowboy o similar para mayor precisión
 */
class WakeWordDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "WakeWordDetector"
        private const val WAKE_WORD = "hey ritsu"
        private const val SAMPLE_RATE = 16000
        private const val DETECTION_WINDOW = 2.0 // 2 segundos
        private const val MIN_CONFIDENCE = 0.7
    }

    private var isInitialized = false
    private val wakeWordPattern = WakeWordPattern()
    private val audioHistory = CircularBuffer(SAMPLE_RATE * DETECTION_WINDOW.toInt())
    
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inicializando detector de wake word...")
                
                // Cargar patrón de "Hey Ritsu"
                wakeWordPattern.loadPattern()
                
                isInitialized = true
                Log.d(TAG, "Detector de wake word inicializado")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando detector de wake word", e)
                false
            }
        }
    }

    suspend fun detectWakeWord(audioData: ShortArray): Boolean {
        return withContext(Dispatchers.Default) {
            if (!isInitialized) return@withContext false

            try {
                // Agregar audio al buffer circular
                audioHistory.addData(audioData)
                
                // Solo procesar si tenemos suficientes datos
                if (audioHistory.isFull()) {
                    val confidence = analyzeForWakeWord(audioHistory.getData())
                    
                    if (confidence >= MIN_CONFIDENCE) {
                        Log.d(TAG, "Wake word detectada con confianza: $confidence")
                        return@withContext true
                    }
                }
                
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error detectando wake word", e)
                false
            }
        }
    }

    private fun analyzeForWakeWord(audioData: ShortArray): Double {
        // Análisis básico de patrón para "Hey Ritsu"
        
        // 1. Detectar dos segmentos de palabra separados por pausa
        val segments = detectWordSegments(audioData)
        if (segments.size < 2) return 0.0
        
        // 2. Analizar cada segmento
        val firstWordScore = analyzeSegment(segments[0], wakeWordPattern.heyPattern)
        val secondWordScore = analyzeSegment(segments[1], wakeWordPattern.ritsuPattern)
        
        // 3. Calcular confianza total
        return (firstWordScore + secondWordScore) / 2.0
    }

    private fun detectWordSegments(audioData: ShortArray): List<IntRange> {
        val segments = mutableListOf<IntRange>()
        val frameSize = 400 // 25ms frames
        val energyThreshold = 800000.0
        
        var inWord = false
        var wordStart = 0
        
        for (i in 0 until audioData.size - frameSize step frameSize) {
            val frame = audioData.sliceArray(i until i + frameSize)
            val energy = calculateEnergy(frame)
            
            if (energy > energyThreshold) {
                if (!inWord) {
                    inWord = true
                    wordStart = i
                }
            } else {
                if (inWord) {
                    inWord = false
                    if (i - wordStart > frameSize * 5) { // Mínimo 125ms
                        segments.add(wordStart until i)
                    }
                }
            }
        }
        
        // Agregar último segmento si termina con palabra
        if (inWord && audioData.size - wordStart > frameSize * 5) {
            segments.add(wordStart until audioData.size)
        }
        
        return segments
    }

    private fun analyzeSegment(segmentRange: IntRange, pattern: WordPattern): Double {
        val segment = audioHistory.getData().sliceArray(segmentRange)
        
        // Calcular características del segmento
        val energy = calculateEnergy(segment)
        val zcr = calculateZCR(segment)
        val spectralCentroid = calculateSpectralCentroid(segment)
        val duration = segment.size.toDouble() / SAMPLE_RATE
        
        // Comparar con patrón esperado
        var score = 0.0
        var factors = 0
        
        // Energía
        if (energy >= pattern.minEnergy && energy <= pattern.maxEnergy) {
            score += 0.3
        } else {
            // Penalizar desviaciones extremas
            val energyDeviation = minOf(
                abs(energy - pattern.minEnergy) / pattern.minEnergy,
                abs(energy - pattern.maxEnergy) / pattern.maxEnergy
            )
            score += 0.3 * exp(-energyDeviation)
        }
        factors++
        
        // ZCR
        if (zcr >= pattern.minZcr && zcr <= pattern.maxZcr) {
            score += 0.25
        } else {
            val zcrDeviation = minOf(
                abs(zcr - pattern.minZcr) / pattern.minZcr,
                abs(zcr - pattern.maxZcr) / pattern.maxZcr
            )
            score += 0.25 * exp(-zcrDeviation)
        }
        factors++
        
        // Centroide espectral
        if (spectralCentroid >= pattern.minSpectralCentroid && 
            spectralCentroid <= pattern.maxSpectralCentroid) {
            score += 0.25
        } else {
            val centroidDeviation = minOf(
                abs(spectralCentroid - pattern.minSpectralCentroid) / pattern.minSpectralCentroid,
                abs(spectralCentroid - pattern.maxSpectralCentroid) / pattern.maxSpectralCentroid
            )
            score += 0.25 * exp(-centroidDeviation)
        }
        factors++
        
        // Duración
        if (duration >= pattern.minDuration && duration <= pattern.maxDuration) {
            score += 0.2
        } else {
            val durationDeviation = minOf(
                abs(duration - pattern.minDuration) / pattern.minDuration,
                abs(duration - pattern.maxDuration) / pattern.maxDuration
            )
            score += 0.2 * exp(-durationDeviation)
        }
        factors++
        
        return score
    }

    private fun calculateEnergy(audioData: ShortArray): Double {
        var energy = 0.0
        for (sample in audioData) {
            energy += sample * sample
        }
        return energy / audioData.size
    }

    private fun calculateZCR(audioData: ShortArray): Double {
        var zeroCrossings = 0
        for (i in 1 until audioData.size) {
            if ((audioData[i] >= 0) != (audioData[i-1] >= 0)) {
                zeroCrossings++
            }
        }
        return zeroCrossings.toDouble() / audioData.size
    }

    private fun calculateSpectralCentroid(audioData: ShortArray): Double {
        // FFT simplificada para centroide espectral
        val fftSize = minOf(512, audioData.size)
        val fft = DoubleArray(fftSize)
        
        // Copiar datos aplicando ventana de Hamming
        for (i in 0 until fftSize) {
            val window = 0.54 - 0.46 * cos(2.0 * PI * i / (fftSize - 1))
            fft[i] = audioData[i].toDouble() * window
        }
        
        // Calcular magnitud del espectro (simplificado)
        var weightedSum = 0.0
        var totalMagnitude = 0.0
        
        for (i in 0 until fftSize / 2) {
            val magnitude = abs(fft[i])
            val frequency = i.toDouble() * SAMPLE_RATE / fftSize
            
            weightedSum += magnitude * frequency
            totalMagnitude += magnitude
        }
        
        return if (totalMagnitude > 0) weightedSum / totalMagnitude else 0.0
    }

    suspend fun shutdown() {
        withContext(Dispatchers.Default) {
            isInitialized = false
            audioHistory.clear()
            Log.d(TAG, "Detector de wake word cerrado")
        }
    }
}

/**
 * Patrón de características para "Hey Ritsu"
 */
private class WakeWordPattern {
    lateinit var heyPattern: WordPattern
    lateinit var ritsuPattern: WordPattern
    
    fun loadPattern() {
        // Patrón para "Hey" - típicamente más agudo y corto
        heyPattern = WordPattern(
            minEnergy = 600000.0,
            maxEnergy = 4000000.0,
            minZcr = 0.1,
            maxZcr = 0.4,
            minSpectralCentroid = 1000.0,
            maxSpectralCentroid = 3500.0,
            minDuration = 0.2,
            maxDuration = 0.8
        )
        
        // Patrón para "Ritsu" - más complejo, con múltiples sílabas
        ritsuPattern = WordPattern(
            minEnergy = 500000.0,
            maxEnergy = 3500000.0,
            minZcr = 0.05,
            maxZcr = 0.3,
            minSpectralCentroid = 800.0,
            maxSpectralCentroid = 3000.0,
            minDuration = 0.4,
            maxDuration = 1.2
        )
    }
}

private data class WordPattern(
    val minEnergy: Double,
    val maxEnergy: Double,
    val minZcr: Double,
    val maxZcr: Double,
    val minSpectralCentroid: Double,
    val maxSpectralCentroid: Double,
    val minDuration: Double,
    val maxDuration: Double
)

/**
 * Buffer circular para mantener historial de audio
 */
private class CircularBuffer(private val size: Int) {
    private val buffer = ShortArray(size)
    private var writeIndex = 0
    private var isFull = false
    
    fun addData(data: ShortArray) {
        for (sample in data) {
            buffer[writeIndex] = sample
            writeIndex = (writeIndex + 1) % size
            if (writeIndex == 0) isFull = true
        }
    }
    
    fun getData(): ShortArray {
        return if (isFull) {
            // Devolver datos en orden cronológico
            val result = ShortArray(size)
            System.arraycopy(buffer, writeIndex, result, 0, size - writeIndex)
            System.arraycopy(buffer, 0, result, size - writeIndex, writeIndex)
            result
        } else {
            buffer.sliceArray(0 until writeIndex)
        }
    }
    
    fun isFull(): Boolean = isFull
    
    fun clear() {
        writeIndex = 0
        isFull = false
    }
}