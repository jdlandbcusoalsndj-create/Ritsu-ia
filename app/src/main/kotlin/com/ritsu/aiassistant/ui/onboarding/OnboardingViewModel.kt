package com.ritsu.aiassistant.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _permissionsGranted = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionsGranted: StateFlow<Map<String, Boolean>> = _permissionsGranted.asStateFlow()

    private val _overlayPermissionGranted = MutableStateFlow(false)
    val overlayPermissionGranted: StateFlow<Boolean> = _overlayPermissionGranted.asStateFlow()

    private val _accessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    fun updatePermissionStatus(permission: String, granted: Boolean) {
        viewModelScope.launch {
            val currentPermissions = _permissionsGranted.value.toMutableMap()
            currentPermissions[permission] = granted
            _permissionsGranted.value = currentPermissions
        }
    }

    fun updateOverlayPermission(granted: Boolean) {
        _overlayPermissionGranted.value = granted
    }

    fun updateAccessibilityStatus(enabled: Boolean) {
        _accessibilityEnabled.value = enabled
    }

    fun nextStep() {
        if (_currentStep.value < 4) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }

    fun completeOnboarding(context: Context) {
        viewModelScope.launch {
            // Guardar que el onboarding está completo
            val prefs = context.getSharedPreferences("ritsu_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("first_time", false)
                putBoolean("onboarding_complete", true)
                putLong("onboarding_completed_time", System.currentTimeMillis())
                apply()
            }
            
            _isComplete.value = true
        }
    }

    fun isReadyToComplete(): Boolean {
        val essentialPermissions = _permissionsGranted.value.values.count { it }
        return essentialPermissions >= 3 && _overlayPermissionGranted.value
    }
}