package com.seanw.webcam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.seanw.webcam.ui.CameraScreen
import com.seanw.webcam.ui.theme.SeCamTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CameraViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                finish()
                return@registerForActivityResult
            }
            viewModel.setCameraPermissionGranted(true)
            viewModel.startCamera(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.setCameraPermissionGranted(true)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            SeCamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    cameraScreenWithPermission()
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun cameraScreenWithPermission() {
        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

        LaunchedEffect(cameraPermissionState.status) {
            if (cameraPermissionState.status.isGranted) {
                viewModel.setCameraPermissionGranted(true)
                viewModel.startCamera(this@MainActivity)
            }
        }

        CameraScreen(
            viewModel = viewModel,
            onCameraClick = {
                viewModel.switchCamera(this@MainActivity)
            },
            onStartStopClick = {
                viewModel.toggleServer()
            },
            onSettingsClick = {
                // TODO: Implement settings functionality
                Toast.makeText(this@MainActivity, "Settings not implemented yet", Toast.LENGTH_SHORT).show()
            },
        )
    }
}
