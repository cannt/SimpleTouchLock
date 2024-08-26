package com.angel.simpletouchlock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.angel.simpletouchlock.ui.theme.SimpleTouchLockTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ScreenLockerViewModel by viewModels()

    companion object {
        var floatingButton: FloatingLockButton? = null
    }

    private val requestOverlayPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Settings.canDrawOverlays(this)) {
            initializeApp()
        } else {
            showPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestOverlayPermission()
    }

    private fun checkAndRequestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            requestOverlayPermission.launch(intent)
        } else {
            initializeApp()
        }
    }

    private fun initializeApp() {
        Log.d("ScreenLocker", "Initializing app")
        setContent {
            Log.d("ScreenLocker", "Setting content")
            SimpleTouchLockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScreenLockerApp(viewModel)
                    DebugText()
                }
            }
        }

        Log.d("ScreenLocker", "Creating FloatingLockButton")
        floatingButton = FloatingLockButton(this) { isLocked ->
            if (isLocked) {
                startLockScreenService()
            } else {
                stopLockScreenService()
            }
        }
        Log.d("ScreenLocker", "Attaching FloatingLockButton")
        floatingButton?.attach()
        Log.d("ScreenLocker", "App initialization complete")
    }

    private fun startLockScreenService() {
        val intent = Intent(this, LockScreenService::class.java).apply {
            action = "LOCK"
        }
        startForegroundService(intent)
        Toast.makeText(this, "Screen locked", Toast.LENGTH_SHORT).show()
    }

    private fun stopLockScreenService() {
        val intent = Intent(this, LockScreenService::class.java).apply {
            action = "UNLOCK"
        }
        startService(intent)
        Toast.makeText(this, "Screen unlocked", Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "Overlay permission is required for the app to function", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingButton?.detach()
    }

    @Composable
    private fun DebugText() {
        Text("Touch Lock App is running")
    }

}