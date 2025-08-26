package com.ritsu.aiassistant.ui.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritsu.aiassistant.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel : ViewModel() {

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadInstalledApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val apps = withContext(Dispatchers.IO) {
                    getInstalledApplications(context)
                }
                _installedApps.value = apps
            } catch (e: Exception) {
                e.printStackTrace()
                _installedApps.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private suspend fun getInstalledApplications(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val apps = packageManager.queryIntentActivities(mainIntent, 0)
        
        return apps.mapNotNull { resolveInfo ->
            try {
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val packageName = appInfo.packageName
                val label = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                
                // Filtrar aplicaciones del sistema que no deberían aparecer
                if (isUserApp(appInfo) || isImportantSystemApp(packageName)) {
                    AppInfo(
                        packageName = packageName,
                        label = label,
                        icon = icon,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }

    private fun isUserApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    }

    private fun isImportantSystemApp(packageName: String): Boolean {
        val importantSystemApps = setOf(
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.apps.photos",
            "com.android.gallery3d",
            "com.android.settings",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.messaging",
            "com.google.android.apps.messaging",
            "com.android.email",
            "com.google.android.gm",
            "com.android.calculator2",
            "com.android.calendar",
            "com.google.android.calendar",
            "com.android.chrome",
            "com.google.android.youtube",
            "com.spotify.music",
            "com.whatsapp",
            "com.telegram.messenger",
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android"
        )
        return importantSystemApps.contains(packageName)
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent?.let {
                context.startActivity(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAppsByCategory(): Map<String, List<AppInfo>> {
        val apps = _installedApps.value
        return apps.groupBy { app ->
            when {
                isSocialApp(app.packageName) -> "Social"
                isMediaApp(app.packageName) -> "Media"
                isProductivityApp(app.packageName) -> "Productividad"
                isGameApp(app.packageName) -> "Juegos"
                isUtilityApp(app.packageName) -> "Utilidades"
                else -> "Otras"
            }
        }
    }

    private fun isSocialApp(packageName: String): Boolean {
        val socialApps = setOf(
            "com.whatsapp", "com.telegram.messenger", "com.instagram.android",
            "com.facebook.katana", "com.twitter.android", "com.snapchat.android",
            "com.discord", "com.tiktok", "com.linkedin.android"
        )
        return socialApps.any { packageName.contains(it) }
    }

    private fun isMediaApp(packageName: String): Boolean {
        val mediaApps = setOf(
            "com.spotify.music", "com.google.android.youtube", "com.netflix.mediaclient",
            "com.android.camera", "com.google.android.apps.photos", "com.amazon.mp3"
        )
        return mediaApps.any { packageName.contains(it) }
    }

    private fun isProductivityApp(packageName: String): Boolean {
        val productivityApps = setOf(
            "com.microsoft.office", "com.google.android.apps.docs", "com.adobe.reader",
            "com.evernote", "com.todoist", "com.any.do", "com.trello"
        )
        return productivityApps.any { packageName.contains(it) }
    }

    private fun isGameApp(packageName: String): Boolean {
        // Esta es una heurística simple, en una implementación real
        // podrías usar la categoría de Google Play Store
        val gameKeywords = setOf("game", "play", "puzzle", "adventure", "action")
        return gameKeywords.any { packageName.contains(it, ignoreCase = true) }
    }

    private fun isUtilityApp(packageName: String): Boolean {
        val utilityApps = setOf(
            "com.android.settings", "com.android.calculator2", "com.android.calendar",
            "com.google.android.apps.maps", "com.android.email"
        )
        return utilityApps.any { packageName.contains(it) }
    }
}