package com.angel.simpletouchlock

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScreenLockerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ScreenLockerUiState())
    val uiState: StateFlow<ScreenLockerUiState> = _uiState

    init {
        checkOverlayPermission()
    }

    private fun checkOverlayPermission() {
        val hasPermission = Settings.canDrawOverlays(getApplication())
        _uiState.value = _uiState.value.copy(hasOverlayPermission = hasPermission)
    }

    fun toggleLockScreen() {
        viewModelScope.launch {
            val newLockState = !_uiState.value.isScreenLocked
            _uiState.value = _uiState.value.copy(isScreenLocked = newLockState)
            val context = getApplication<Application>()
            val intent = Intent(context, LockScreenService::class.java)
            intent.action = if (newLockState) "LOCK" else "UNLOCK"
            context.startForegroundService(intent)
        }
    }
}

data class ScreenLockerUiState(
    val hasOverlayPermission: Boolean = false,
    val isScreenLocked: Boolean = false
)