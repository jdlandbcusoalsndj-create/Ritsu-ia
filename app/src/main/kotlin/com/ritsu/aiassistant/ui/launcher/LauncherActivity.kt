package com.ritsu.aiassistant.ui.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ritsu.aiassistant.RitsuApplication
import com.ritsu.aiassistant.services.RitsuOverlayService
import com.ritsu.aiassistant.ui.components.AppIcon
import com.ritsu.aiassistant.ui.components.RitsuAvatar
import com.ritsu.aiassistant.ui.components.WeatherWidget
import com.ritsu.aiassistant.ui.components.SearchBar
import com.ritsu.aiassistant.ui.onboarding.OnboardingActivity
import com.ritsu.aiassistant.ui.theme.RitsuAIAssistantTheme
import com.ritsu.aiassistant.data.model.AppInfo
import kotlinx.coroutines.launch

class LauncherActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startOverlayService()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            startOverlayService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Verificar si es la primera vez que se abre la app
        val sharedPrefs = getSharedPreferences("ritsu_prefs", MODE_PRIVATE)
        val isFirstTime = sharedPrefs.getBoolean("first_time", true)
        
        if (isFirstTime) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContent {
            RitsuAIAssistantTheme {
                LauncherScreen(
                    onRequestPermissions = { requestNecessaryPermissions() },
                    onStartOverlayService = { startOverlayService() }
                )
            }
        }

        // Solicitar permisos necesarios
        requestNecessaryPermissions()
    }

    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>().apply {
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

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startOverlayService()
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, RitsuOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = viewModel(),
    onRequestPermissions: () -> Unit,
    onStartOverlayService: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apps by viewModel.installedApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Barra de búsqueda
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Avatar de Ritsu (posición central)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            RitsuAvatar(
                modifier = Modifier.size(150.dp),
                onClick = {
                    scope.launch {
                        onStartOverlayService()
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Widget del clima
        WeatherWidget(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Grid de aplicaciones
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps.filter { app ->
                if (searchQuery.isBlank()) true
                else app.label.contains(searchQuery, ignoreCase = true)
            }) { app ->
                AppIcon(
                    app = app,
                    onClick = {
                        scope.launch {
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            intent?.let { context.startActivity(it) }
                        }
                    }
                )
            }
        }
    }
}