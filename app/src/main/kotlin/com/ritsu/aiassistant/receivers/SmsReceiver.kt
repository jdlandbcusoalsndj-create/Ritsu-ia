package com.ritsu.aiassistant.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.ritsu.aiassistant.services.MessagingService

class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                    handleSmsReceived(context, intent)
                }
                Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION -> {
                    handleMmsReceived(context, intent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando SMS/MMS", e)
        }
    }

    private fun handleSmsReceived(context: Context, intent: Intent) {
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (message in messages) {
                val sender = message.originatingAddress ?: "Desconocido"
                val messageBody = message.messageBody ?: ""
                val timestamp = message.timestampMillis
                
                Log.d(TAG, "SMS recibido de $sender: $messageBody")
                
                // Verificar configuración del usuario
                val prefs = context.getSharedPreferences("ritsu_prefs", Context.MODE_PRIVATE)
                val ritsuHandleSms = prefs.getBoolean("ritsu_handle_sms", true)
                
                if (ritsuHandleSms) {
                    // Enviar al servicio de mensajería para procesamiento
                    MessagingService.processMessage(context, sender, messageBody, "SMS")
                } else {
                    Log.d(TAG, "Manejo de SMS por Ritsu deshabilitado")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando SMS recibido", e)
        }
    }

    private fun handleMmsReceived(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "MMS recibido - procesamiento básico")
            
            // Para MMS el procesamiento es más complejo
            // Aquí implementarías la lógica para leer MMS
            val prefs = context.getSharedPreferences("ritsu_prefs", Context.MODE_PRIVATE)
            val ritsuHandleMms = prefs.getBoolean("ritsu_handle_mms", true)
            
            if (ritsuHandleMms) {
                // Implementación pendiente para MMS
                Log.d(TAG, "Procesamiento de MMS pendiente de implementar")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando MMS", e)
        }
    }
}