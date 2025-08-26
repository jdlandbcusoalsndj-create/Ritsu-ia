package com.ritsu.aiassistant.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ritsu.aiassistant.ui.components.RitsuAvatar
import com.ritsu.aiassistant.ui.components.AvatarExpression
import com.ritsu.aiassistant.ui.components.AvatarAnimation
import com.ritsu.aiassistant.ui.launcher.LauncherActivity
import com.ritsu.aiassistant.ui.theme.RitsuAIAssistantTheme
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Manejar resultados de permisos
        permissions.entries.forEach { (permission, granted) ->
            if (granted) {
                onPermissionGranted(permission)
            } else {
                onPermissionDenied(permission)
            }
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            onOverlayPermissionGranted()
        }
    }

    private val accessibilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if accessibility is enabled
        onAccessibilityPermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            RitsuAIAssistantTheme {
                OnboardingScreen(
                    onRequestPermissions = { permissions ->
                        requestPermissions(permissions)
                    },
                    onRequestOverlayPermission = {
                        requestOverlayPermission()
                    },
                    onRequestAccessibilityPermission = {
                        requestAccessibilityPermission()
                    },
                    onOnboardingComplete = {
                        completeOnboarding()
                    }
                )
            }
        }
    }

    private fun requestPermissions(permissions: List<String>) {
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        accessibilityLauncher.launch(intent)
    }

    private fun onPermissionGranted(permission: String) {
        // Handle individual permission grants
    }

    private fun onPermissionDenied(permission: String) {
        // Handle individual permission denials
    }

    private fun onOverlayPermissionGranted() {
        // Handle overlay permission granted
    }

    private fun onAccessibilityPermissionResult() {
        // Check if accessibility service is enabled
    }

    private fun completeOnboarding() {
        // Mark onboarding as complete
        val prefs = getSharedPreferences("ritsu_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("first_time", false).apply()
        
        // Start launcher activity
        val intent = Intent(this, LauncherActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onRequestPermissions: (List<String>) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 5 })
    
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (currentPage + 1) / 5f },
            modifier = Modifier.fillMaxWidth(),
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> PermissionsPage(
                    onRequestPermissions = onRequestPermissions
                )
                2 -> OverlayPermissionPage(
                    onRequestOverlayPermission = onRequestOverlayPermission
                )
                3 -> AccessibilityPage(
                    onRequestAccessibilityPermission = onRequestAccessibilityPermission
                )
                4 -> CompletePage(
                    onComplete = onOnboardingComplete
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(currentPage - 1)
                        }
                    }
                ) {
                    Text("Anterior")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            
            if (currentPage < 4) {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(currentPage + 1)
                        }
                    }
                ) {
                    Text("Siguiente")
                }
            }
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ritsu avatar
        RitsuAvatar(
            modifier = Modifier.size(200.dp),
            expression = AvatarExpression.HAPPY,
            animation = AvatarAnimation.GREETING,
            isActive = true
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "¡Hai! Soy Ritsu",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tu nueva asistente de IA personal. Estoy aquí para hacer tu vida más fácil y divertida.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Puedo manejar llamadas, responder mensajes, abrir aplicaciones y mucho más, ¡todo completamente gratis!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PermissionsPage(
    onRequestPermissions: (List<String>) -> Unit
) {
    val context = LocalContext.current
    
    val requiredPermissions = remember {
        mutableListOf<String>().apply {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
        }
    }
    
    val permissionsGranted = remember(requiredPermissions) {
        requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RitsuAvatar(
            modifier = Modifier.size(150.dp),
            expression = if (permissionsGranted) AvatarExpression.HAPPY else AvatarExpression.THINKING,
            animation = AvatarAnimation.IDLE
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Permisos Necesarios",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Para funcionar correctamente, necesito algunos permisos:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val permissionDescriptions = listOf(
            "🎤 Micrófono - Para escuchar tus comandos de voz",
            "📞 Teléfono - Para manejar llamadas por ti",
            "📱 SMS - Para leer y responder mensajes",
            "👥 Contactos - Para identificar quién te llama",
            "📍 Ubicación - Para el clima y servicios locales",
            "📸 Cámara - Para funciones multimedia"
        )
        
        permissionDescriptions.forEach { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!permissionsGranted) {
            Button(
                onClick = { onRequestPermissions(requiredPermissions) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Conceder Permisos")
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "¡Perfecto! Todos los permisos concedidos ✓",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun OverlayPermissionPage(
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RitsuAvatar(
            modifier = Modifier.size(150.dp),
            expression = if (hasOverlayPermission) AvatarExpression.HAPPY else AvatarExpression.SURPRISED,
            animation = AvatarAnimation.IDLE,
            isActive = hasOverlayPermission
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Permiso de Overlay",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Este permiso me permite aparecer sobre otras aplicaciones para ayudarte en cualquier momento.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "¿Qué puedo hacer con este permiso?",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Aparece como avatar flotante en cualquier app")
                Text("• Responde rápidamente a tus comandos")
                Text("• Te ayuda sin interrumpir tu trabajo")
                Text("• Puedes moverme por la pantalla libremente")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!hasOverlayPermission) {
            Button(
                onClick = onRequestOverlayPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Conceder Permiso de Overlay")
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "¡Excelente! Permiso de overlay concedido ✓",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AccessibilityPage(
    onRequestAccessibilityPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RitsuAvatar(
            modifier = Modifier.size(150.dp),
            expression = AvatarExpression.THINKING,
            animation = AvatarAnimation.IDLE
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Servicio de Accesibilidad",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Este servicio me permite interactuar con otras aplicaciones para ayudarte mejor.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Funciones habilitadas:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Abrir aplicaciones por comando de voz")
                Text("• Leer notificaciones en voz alta")
                Text("• Ayudar con navegación de apps")
                Text("• Detectar cambios en la pantalla")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestAccessibilityPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Configurar Accesibilidad")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { /* Skip for now */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Omitir por ahora")
        }
    }
}

@Composable
fun CompletePage(
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RitsuAvatar(
            modifier = Modifier.size(200.dp),
            expression = AvatarExpression.HAPPY,
            animation = AvatarAnimation.CELEBRATING,
            isActive = true
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "¡Todo Listo!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "¡Sugoi! ¡Estoy lista para ayudarte!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Para empezar:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• Di 'Hey Ritsu' para activarme",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "• Toca mi avatar para opciones rápidas",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "• Configura tus preferencias en ajustes",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("¡Empezar con Ritsu!")
        }
    }
}