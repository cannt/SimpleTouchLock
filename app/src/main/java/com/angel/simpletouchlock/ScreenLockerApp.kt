package com.angel.simpletouchlock

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ScreenLockerApp(viewModel: ScreenLockerViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.hasOverlayPermission) {
        AndroidView(
            factory = { context ->
                FloatingLockButton(context) {
                    viewModel.toggleLockScreen()
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.updateLockedState(uiState.isScreenLocked)
            }
        )
    }
}