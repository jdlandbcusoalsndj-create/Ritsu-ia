package com.ritsu.aiassistant.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

enum class AvatarExpression {
    NEUTRAL, HAPPY, SAD, THINKING, SPEAKING, LISTENING, SURPRISED, SLEEPY
}

enum class AvatarAnimation {
    IDLE, TALKING, LISTENING, THINKING, GREETING, CELEBRATING
}

@Composable
fun RitsuAvatar(
    modifier: Modifier = Modifier,
    expression: AvatarExpression = AvatarExpression.NEUTRAL,
    animation: AvatarAnimation = AvatarAnimation.IDLE,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var currentExpression by remember { mutableStateOf(expression) }
    var isBlinking by remember { mutableStateOf(false) }
    var headRotation by remember { mutableStateOf(0f) }
    
    // Animación de respiración/latido
    val breathingScale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "breathing"
    )
    
    // Animación de rotación de cabeza
    val headRotationAnimation by animateFloatAsState(
        targetValue = headRotation,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "head_rotation"
    )
    
    // Efecto de parpadeo
    LaunchedEffect(Unit) {
        while (true) {
            delay((2000..5000).random().toLong()) // Parpadeo aleatorio cada 2-5 segundos
            isBlinking = true
            delay(150)
            isBlinking = false
        }
    }
    
    // Efecto de movimiento de cabeza aleatorio
    LaunchedEffect(Unit) {
        while (true) {
            delay((3000..8000).random().toLong())
            headRotation = (-15f..15f).random()
            delay(1000)
            headRotation = 0f
        }
    }
    
    // Actualizar expresión
    LaunchedEffect(expression) {
        currentExpression = expression
    }
    
    Box(
        modifier = modifier
            .scale(breathingScale)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Aura/Glow effect cuando está activa
        if (isActive) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawAuraEffect(size.minDimension)
            }
        }
        
        // Avatar principal
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = headRotationAnimation
                }
        ) {
            drawRitsuAvatar(
                expression = currentExpression,
                animation = animation,
                isBlinking = isBlinking,
                size = size.minDimension
            )
        }
        
        // Indicador de estado de voz
        if (animation == AvatarAnimation.TALKING || animation == AvatarAnimation.LISTENING) {
            VoiceIndicator(
                isListening = animation == AvatarAnimation.LISTENING,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-10).dp)
            )
        }
    }
}

@Composable
private fun VoiceIndicator(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_indicator")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    
    Box(
        modifier = modifier
            .size(20.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isListening) {
                    Color.Red.copy(alpha = alpha)
                } else {
                    Color.Green.copy(alpha = alpha)
                }
            )
    )
}

private fun DrawScope.drawAuraEffect(size: Float) {
    val center = Offset(this.size.width / 2, this.size.height / 2)
    val radius = size / 2
    
    // Gradiente circular para el aura
    val gradient = RadialGradient(
        colors = listOf(
            Color(0xFF8A2BE2).copy(alpha = 0.3f), // Púrpura
            Color(0xFF00BFFF).copy(alpha = 0.2f), // Azul cielo
            Color.Transparent
        ),
        radius = radius * 1.2f,
        center = center
    )
    
    drawCircle(
        brush = gradient,
        radius = radius * 1.1f,
        center = center
    )
}

private fun DrawScope.drawRitsuAvatar(
    expression: AvatarExpression,
    animation: AvatarAnimation,
    isBlinking: Boolean,
    size: Float
) {
    val center = Offset(this.size.width / 2, this.size.height / 2)
    val faceRadius = size * 0.35f
    
    // Cara (base)
    drawCircle(
        color = Color(0xFFFFDBB5), // Tono de piel anime
        radius = faceRadius,
        center = center
    )
    
    // Contorno de la cara
    drawCircle(
        color = Color(0xFFE6C2A6),
        radius = faceRadius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
    
    // Cabello
    drawArc(
        color = Color(0xFF8B4513), // Castaño
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - faceRadius * 1.1f, center.y - faceRadius * 1.1f),
        size = androidx.compose.ui.geometry.Size(faceRadius * 2.2f, faceRadius * 2.2f)
    )
    
    // Ojos
    val eyeY = center.y - faceRadius * 0.2f
    val eyeOffsetX = faceRadius * 0.3f
    val eyeSize = faceRadius * 0.15f
    
    if (!isBlinking) {
        // Ojo izquierdo
        drawEye(
            center = Offset(center.x - eyeOffsetX, eyeY),
            size = eyeSize,
            expression = expression
        )
        
        // Ojo derecho
        drawEye(
            center = Offset(center.x + eyeOffsetX, eyeY),
            size = eyeSize,
            expression = expression
        )
    } else {
        // Ojos cerrados (parpadeo)
        drawBlinkingEyes(
            leftCenter = Offset(center.x - eyeOffsetX, eyeY),
            rightCenter = Offset(center.x + eyeOffsetX, eyeY),
            size = eyeSize
        )
    }
    
    // Nariz
    drawCircle(
        color = Color(0xFFE6C2A6),
        radius = faceRadius * 0.03f,
        center = Offset(center.x, center.y)
    )
    
    // Boca según expresión
    drawMouth(
        center = Offset(center.x, center.y + faceRadius * 0.3f),
        expression = expression,
        faceRadius = faceRadius
    )
    
    // Mejillas (rubor opcional)
    if (expression == AvatarExpression.HAPPY || expression == AvatarExpression.SURPRISED) {
        drawBlush(
            leftCenter = Offset(center.x - faceRadius * 0.6f, center.y + faceRadius * 0.1f),
            rightCenter = Offset(center.x + faceRadius * 0.6f, center.y + faceRadius * 0.1f),
            size = faceRadius * 0.1f
        )
    }
}

private fun DrawScope.drawEye(
    center: Offset,
    size: Float,
    expression: AvatarExpression
) {
    // Blanco del ojo
    drawCircle(
        color = Color.White,
        radius = size,
        center = center
    )
    
    // Iris
    val irisColor = Color(0xFF4169E1) // Azul real
    val irisSize = size * 0.7f
    
    drawCircle(
        color = irisColor,
        radius = irisSize,
        center = center
    )
    
    // Pupila
    val pupilSize = when (expression) {
        AvatarExpression.SURPRISED -> irisSize * 0.8f
        AvatarExpression.SLEEPY -> irisSize * 0.3f
        else -> irisSize * 0.5f
    }
    
    drawCircle(
        color = Color.Black,
        radius = pupilSize,
        center = center
    )
    
    // Brillo en el ojo
    drawCircle(
        color = Color.White,
        radius = size * 0.2f,
        center = Offset(center.x - size * 0.3f, center.y - size * 0.3f)
    )
    
    // Contorno del ojo
    drawCircle(
        color = Color.Black,
        radius = size,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawBlinkingEyes(
    leftCenter: Offset,
    rightCenter: Offset,
    size: Float
) {
    // Líneas horizontales para ojos cerrados
    drawLine(
        color = Color.Black,
        start = Offset(leftCenter.x - size, leftCenter.y),
        end = Offset(leftCenter.x + size, leftCenter.y),
        strokeWidth = 3.dp.toPx()
    )
    
    drawLine(
        color = Color.Black,
        start = Offset(rightCenter.x - size, rightCenter.y),
        end = Offset(rightCenter.x + size, rightCenter.y),
        strokeWidth = 3.dp.toPx()
    )
}

private fun DrawScope.drawMouth(
    center: Offset,
    expression: AvatarExpression,
    faceRadius: Float
) {
    when (expression) {
        AvatarExpression.HAPPY -> {
            // Sonrisa
            drawArc(
                color = Color.Black,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - faceRadius * 0.2f, center.y - faceRadius * 0.1f),
                size = androidx.compose.ui.geometry.Size(faceRadius * 0.4f, faceRadius * 0.2f),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        AvatarExpression.SAD -> {
            // Boca triste
            drawArc(
                color = Color.Black,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - faceRadius * 0.15f, center.y),
                size = androidx.compose.ui.geometry.Size(faceRadius * 0.3f, faceRadius * 0.15f),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        AvatarExpression.SURPRISED -> {
            // Boca abierta circular
            drawCircle(
                color = Color.Black,
                radius = faceRadius * 0.08f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        AvatarExpression.SPEAKING -> {
            // Boca abierta ovalada
            drawOval(
                color = Color.Black,
                topLeft = Offset(center.x - faceRadius * 0.1f, center.y - faceRadius * 0.05f),
                size = androidx.compose.ui.geometry.Size(faceRadius * 0.2f, faceRadius * 0.1f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        else -> {
            // Boca neutral (línea pequeña)
            drawLine(
                color = Color.Black,
                start = Offset(center.x - faceRadius * 0.1f, center.y),
                end = Offset(center.x + faceRadius * 0.1f, center.y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawBlush(
    leftCenter: Offset,
    rightCenter: Offset,
    size: Float
) {
    val blushColor = Color(0xFFFF69B4).copy(alpha = 0.4f)
    
    drawCircle(
        color = blushColor,
        radius = size,
        center = leftCenter
    )
    
    drawCircle(
        color = blushColor,
        radius = size,
        center = rightCenter
    )
}