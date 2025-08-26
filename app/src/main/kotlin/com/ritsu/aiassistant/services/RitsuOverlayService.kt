package com.ritsu.aiassistant.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ritsu.aiassistant.R
import com.ritsu.aiassistant.RitsuApplication
import com.ritsu.aiassistant.ui.components.AvatarExpression
import com.ritsu.aiassistant.ui.components.AvatarAnimation
import com.ritsu.aiassistant.ui.components.RitsuAvatar
import com.ritsu.aiassistant.ui.theme.RitsuAIAssistantTheme
import kotlinx.coroutines.*

class RitsuOverlayService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_SHOW_OVERLAY = "show_overlay"
        const val ACTION_HIDE_OVERLAY = "hide_overlay"
        const val ACTION_TOGGLE_OVERLAY = "toggle_overlay"
        const val ACTION_UPDATE_EXPRESSION = "update_expression"
        const val ACTION_UPDATE_ANIMATION = "update_animation"
        
        const val EXTRA_EXPRESSION = "expression"
        const val EXTRA_ANIMATION = "animation"
        const val EXTRA_MESSAGE = "message"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private var composeView: ComposeView? = null
    
    private var isOverlayVisible = false
    private var overlayX = 0
    private var overlayY = 0
    
    // Estados del avatar
    private var currentExpression = mutableStateOf(AvatarExpression.NEUTRAL)
    private var currentAnimation = mutableStateOf(AvatarAnimation.IDLE)
    private var isActive = mutableStateOf(false)
    private var currentMessage = mutableStateOf("")
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        intent?.let { handleIntent(it) }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        serviceScope.cancel()
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
            ACTION_TOGGLE_OVERLAY -> toggleOverlay()
            ACTION_UPDATE_EXPRESSION -> {
                val expression = intent.getStringExtra(EXTRA_EXPRESSION)
                expression?.let { updateExpression(AvatarExpression.valueOf(it)) }
            }
            ACTION_UPDATE_ANIMATION -> {
                val animation = intent.getStringExtra(EXTRA_ANIMATION)
                animation?.let { updateAnimation(AvatarAnimation.valueOf(it)) }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RitsuApplication.CHANNEL_ID_OVERLAY,
                "Ritsu Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación para mantener a Ritsu activa"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val hideIntent = Intent(this, RitsuOverlayService::class.java).apply {
            action = ACTION_HIDE_OVERLAY
        }
        val hidePendingIntent = PendingIntent.getService(
            this, 0, hideIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(this, RitsuOverlayService::class.java).apply {
            action = ACTION_SHOW_OVERLAY
        }
        val showPendingIntent = PendingIntent.getService(
            this, 1, showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, RitsuApplication.CHANNEL_ID_OVERLAY)
            .setContentTitle("Ritsu AI Assistant")
            .setContentText("Tu asistente está activa")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_visibility_off,
                "Ocultar",
                hidePendingIntent
            )
            .addAction(
                R.drawable.ic_visibility,
                "Mostrar",
                showPendingIntent
            )
            .build()
    }

    private fun showOverlay() {
        if (isOverlayVisible || overlayView != null) return

        try {
            val layoutParams = WindowManager.LayoutParams().apply {
                width = 120
                height = 120
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                x = overlayX
                y = overlayY
            }

            overlayView = FrameLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@RitsuOverlayService as androidx.lifecycle.LifecycleOwner)
                setViewTreeViewModelStoreOwner(this@RitsuOverlayService as androidx.lifecycle.ViewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(this@RitsuOverlayService as androidx.savedstate.SavedStateRegistryOwner)
                
                setContent {
                    RitsuAIAssistantTheme {
                        OverlayContent(
                            expression = currentExpression.value,
                            animation = currentAnimation.value,
                            isActive = isActive.value,
                            onDrag = { deltaX, deltaY ->
                                updateOverlayPosition(deltaX, deltaY)
                            },
                            onClick = {
                                handleAvatarClick()
                            }
                        )
                    }
                }
            }

            overlayView?.addView(composeView)
            windowManager?.addView(overlayView, layoutParams)
            isOverlayVisible = true

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideOverlay() {
        if (!isOverlayVisible || overlayView == null) return

        try {
            windowManager?.removeView(overlayView)
            overlayView = null
            composeView = null
            isOverlayVisible = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleOverlay() {
        if (isOverlayVisible) {
            hideOverlay()
        } else {
            showOverlay()
        }
    }

    private fun updateOverlayPosition(deltaX: Float, deltaY: Float) {
        if (!isOverlayVisible || overlayView == null) return

        overlayX += deltaX.toInt()
        overlayY += deltaY.toInt()

        val layoutParams = overlayView?.layoutParams as? WindowManager.LayoutParams
        layoutParams?.let {
            it.x = overlayX
            it.y = overlayY
            windowManager?.updateViewLayout(overlayView, it)
        }
    }

    private fun handleAvatarClick() {
        // Activar Ritsu y mostrar opciones
        serviceScope.launch {
            isActive.value = true
            currentExpression.value = AvatarExpression.HAPPY
            currentAnimation.value = AvatarAnimation.GREETING
            
            delay(2000)
            
            isActive.value = false
            currentExpression.value = AvatarExpression.NEUTRAL
            currentAnimation.value = AvatarAnimation.IDLE
        }
    }

    // Métodos públicos para controlar el avatar
    fun updateExpression(expression: AvatarExpression) {
        currentExpression.value = expression
    }

    fun updateAnimation(animation: AvatarAnimation) {
        currentAnimation.value = animation
    }

    fun setActive(active: Boolean) {
        isActive.value = active
    }

    fun showMessage(message: String) {
        currentMessage.value = message
        serviceScope.launch {
            delay(3000) // Mostrar mensaje por 3 segundos
            currentMessage.value = ""
        }
    }

    // Métodos estáticos para controlar el servicio desde otras partes de la app
    companion object {
        fun showOverlay(context: Context) {
            val intent = Intent(context, RitsuOverlayService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            }
            context.startService(intent)
        }

        fun hideOverlay(context: Context) {
            val intent = Intent(context, RitsuOverlayService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            context.startService(intent)
        }

        fun updateExpression(context: Context, expression: AvatarExpression) {
            val intent = Intent(context, RitsuOverlayService::class.java).apply {
                action = ACTION_UPDATE_EXPRESSION
                putExtra(EXTRA_EXPRESSION, expression.name)
            }
            context.startService(intent)
        }

        fun updateAnimation(context: Context, animation: AvatarAnimation) {
            val intent = Intent(context, RitsuOverlayService::class.java).apply {
                action = ACTION_UPDATE_ANIMATION
                putExtra(EXTRA_ANIMATION, animation.name)
            }
            context.startService(intent)
        }
    }
}

@Composable
private fun OverlayContent(
    expression: AvatarExpression,
    animation: AvatarAnimation,
    isActive: Boolean,
    onDrag: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        if (!isDragging) {
                            onClick()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            }
    ) {
        RitsuAvatar(
            modifier = Modifier.fillMaxSize(),
            expression = expression,
            animation = animation,
            isActive = isActive,
            onClick = if (!isDragging) onClick else null
        )
    }
}