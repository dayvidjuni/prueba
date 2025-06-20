package com.example.proyectoantifatiga.utils

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyectoantifatiga.camera.CameraPreview
import androidx.camera.view.PreviewView

@Composable
fun FatigueUI(
    showFatigueMessage: Boolean,
    onCameraReady: (PreviewView) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CameraPreview(modifier = Modifier.fillMaxSize(), onPreviewCreated = onCameraReady)
        }

        if (showFatigueMessage) {
            Text(
                text = "\u00a1Fatiga detectada!",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
