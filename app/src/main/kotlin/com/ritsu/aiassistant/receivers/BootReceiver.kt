package com.ritsu.aiassistant.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ritsu.aiassistant.services.RitsuOverlayService

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_QUICKBOOT_POWERON,
                "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                    handleBootCompleted(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en BootReceiver", e)
        }
    }

    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "Boot completado, iniciando servicios de Ritsu")
        
        try {
            // Verificar si es la primera vez o si Ritsu está habilitado
            val prefs = context.getSharedPreferences("ritsu_prefs", Context.MODE_PRIVATE)
            val isFirstTime = prefs.getBoolean("first_time", true)
            val autoStartEnabled = prefs.getBoolean("auto_start_on_boot", true)
            
            if (!isFirstTime && autoStartEnabled) {
                // Iniciar servicio de overlay
                val overlayIntent = Intent(context, RitsuOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(overlayIntent)
                } else {
                    context.startService(overlayIntent)
                }
                
                Log.d(TAG, "Servicios de Ritsu iniciados después del boot")
            } else {
                Log.d(TAG, "Auto-inicio deshabilitado o primera vez")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando servicios después del boot", e)
        }
    }
}