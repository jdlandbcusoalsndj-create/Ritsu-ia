package com.ritsu.aiassistant.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.ritsu.aiassistant.services.CallHandlingService
import com.ritsu.aiassistant.services.RitsuOverlayService
import com.ritsu.aiassistant.ui.components.AvatarExpression
import com.ritsu.aiassistant.ui.components.AvatarAnimation

class CallReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "CallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                    handlePhoneStateChanged(context, intent)
                }
                Intent.ACTION_NEW_OUTGOING_CALL -> {
                    handleOutgoingCall(context, intent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando evento de llamada", e)
        }
    }

    private fun handlePhoneStateChanged(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        
        Log.d(TAG, "Estado de teléfono cambió: $state, Número: $phoneNumber")
        
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                handleIncomingCall(context, phoneNumber)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                handleCallStarted(context, phoneNumber)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                handleCallEnded(context)
            }
        }
    }

    private fun handleIncomingCall(context: Context, phoneNumber: String?) {
        Log.d(TAG, "Llamada entrante detectada: $phoneNumber")
        
        try {
            // Verificar configuración del usuario
            val prefs = context.getSharedPreferences("ritsu_prefs", Context.MODE_PRIVATE)
            val autoAnswerEnabled = prefs.getBoolean("auto_answer_calls", true)
            val ritsuHandlingEnabled = prefs.getBoolean("ritsu_call_handling", true)
            
            if (!ritsuHandlingEnabled) {
                Log.d(TAG, "Manejo de llamadas por Ritsu deshabilitado")
                return
            }
            
            // Actualizar avatar de Ritsu para mostrar llamada entrante
            RitsuOverlayService.updateExpression(context, AvatarExpression.SURPRISED)
            RitsuOverlayService.updateAnimation(context, AvatarAnimation.GREETING)
            
            // Determinar si es una llamada conocida o spam
            val callerType = determineCallerType(context, phoneNumber)
            
            when (callerType) {
                CallerType.KNOWN_CONTACT -> {
                    if (autoAnswerEnabled) {
                        Log.d(TAG, "Auto-respondiendo llamada de contacto conocido")
                        // Activar servicio de manejo de llamadas
                        startCallHandling(context, phoneNumber)
                    } else {
                        // Mostrar notificación para que el usuario decida
                        showIncomingCallNotification(context, phoneNumber, true)
                    }
                }
                CallerType.UNKNOWN -> {
                    // Preguntarle al usuario qué hacer
                    showIncomingCallNotification(context, phoneNumber, false)
                }
                CallerType.SPAM -> {
                    Log.d(TAG, "Llamada identificada como spam, rechazando automáticamente")
                    // Aquí podrías rechazar automáticamente si tienes los permisos
                    showSpamCallNotification(context, phoneNumber)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando llamada entrante", e)
        }
    }

    private fun handleOutgoingCall(context: Context, intent: Intent) {
        val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        Log.d(TAG, "Llamada saliente detectada: $phoneNumber")
        
        try {
            // Actualizar avatar para mostrar llamada saliente
            RitsuOverlayService.updateExpression(context, AvatarExpression.HAPPY)
            RitsuOverlayService.updateAnimation(context, AvatarAnimation.TALKING)
            
            // Verificar si Ritsu debe manejar la llamada saliente
            val prefs = context.getSharedPreferences("ritsu_prefs", Context.MODE_PRIVATE)
            val handleOutgoingCalls = prefs.getBoolean("handle_outgoing_calls", false)
            
            if (handleOutgoingCalls) {
                startCallHandling(context, phoneNumber)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando llamada saliente", e)
        }
    }

    private fun handleCallStarted(context: Context, phoneNumber: String?) {
        Log.d(TAG, "Llamada iniciada: $phoneNumber")
        
        // Actualizar avatar para mostrar que está en llamada
        RitsuOverlayService.updateExpression(context, AvatarExpression.SPEAKING)
        RitsuOverlayService.updateAnimation(context, AvatarAnimation.TALKING)
    }

    private fun handleCallEnded(context: Context) {
        Log.d(TAG, "Llamada terminada")
        
        // Restablecer avatar a estado normal
        RitsuOverlayService.updateExpression(context, AvatarExpression.NEUTRAL)
        RitsuOverlayService.updateAnimation(context, AvatarAnimation.IDLE)
    }

    private fun startCallHandling(context: Context, phoneNumber: String?) {
        try {
            val intent = Intent(context, CallHandlingService::class.java).apply {
                putExtra("phone_number", phoneNumber)
                putExtra("action", "handle_call")
            }
            context.startService(intent)
            
            Log.d(TAG, "Servicio de manejo de llamadas iniciado")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando servicio de llamadas", e)
        }
    }

    private fun determineCallerType(context: Context, phoneNumber: String?): CallerType {
        if (phoneNumber.isNullOrBlank()) return CallerType.UNKNOWN
        
        try {
            // Verificar si es un contacto conocido
            if (isKnownContact(context, phoneNumber)) {
                return CallerType.KNOWN_CONTACT
            }
            
            // Verificar patrones de spam conocidos
            if (isSpamNumber(phoneNumber)) {
                return CallerType.SPAM
            }
            
            return CallerType.UNKNOWN
            
        } catch (e: Exception) {
            Log.e(TAG, "Error determinando tipo de llamante", e)
            return CallerType.UNKNOWN
        }
    }

    private fun isKnownContact(context: Context, phoneNumber: String): Boolean {
        // Aquí verificarías en ContactsContract si el número está en contactos
        // Implementación simplificada
        return try {
            // Simular verificación de contactos
            false // Placeholder
        } catch (e: Exception) {
            false
        }
    }

    private fun isSpamNumber(phoneNumber: String): Boolean {
        // Patrones básicos para identificar spam
        val spamPatterns = listOf(
            "^\\+?1?8[0-9]{2}[0-9]{3}[0-9]{4}$", // Números 800
            "^\\+?1?900[0-9]{7}$", // Números 900
            "^\\+?52[0-9]{10}$" // Algunos patrones internacionales sospechosos
        )
        
        return spamPatterns.any { pattern ->
            phoneNumber.matches(Regex(pattern))
        }
    }

    private fun showIncomingCallNotification(context: Context, phoneNumber: String?, isKnownContact: Boolean) {
        // Crear notificación interactiva para que el usuario decida
        // Implementación pendiente - requiere NotificationManager
        Log.d(TAG, "Mostrando notificación de llamada entrante: $phoneNumber (Conocido: $isKnownContact)")
    }

    private fun showSpamCallNotification(context: Context, phoneNumber: String?) {
        // Notificar que se bloqueó una llamada de spam
        Log.d(TAG, "Notificando llamada de spam bloqueada: $phoneNumber")
    }

    enum class CallerType {
        KNOWN_CONTACT,
        UNKNOWN,
        SPAM
    }
}