package com.seanw.webcam.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import com.seanw.webcam.CameraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onCameraClick: () -> Unit,
    onStartStopClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                viewModel.setPreviewView(previewView)
            }
        )
        
        // IP Address Text
        Text(
            text = uiState.ipAddress,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        )
        
        // Floating Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Settings FAB (visible when expanded)
            if (uiState.isFabExpanded) {
                FloatingActionButton(
                    onClick = onSettingsClick,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                }
            }
            
            // Start/Stop FAB (visible when expanded)
            if (uiState.isFabExpanded) {
                FloatingActionButton(
                    onClick = onStartStopClick,
                    containerColor = if (uiState.isServerRunning) 
                        MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.primary,
                    contentColor = if (uiState.isServerRunning) 
                        MaterialTheme.colorScheme.onError 
                        else MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        if (uiState.isServerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isServerRunning) "Stop" else "Start"
                    )
                }
            }
            
            // Camera Switch FAB (visible when expanded)
            if (uiState.isFabExpanded) {
                FloatingActionButton(
                    onClick = onCameraClick,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Switch Camera")
                }
            }
            
            // Main FAB
            FloatingActionButton(
                onClick = { viewModel.toggleFabExpansion() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    if (uiState.isFabExpanded) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = if (uiState.isFabExpanded) "Close" else "Menu"
                )
            }
        }
    }
}
