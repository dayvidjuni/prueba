package com.example.proyectoantifatiga.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPreviewCreated: (PreviewView) -> Unit
) {
    AndroidView(
        factory = { ctx: Context ->
            val previewView = PreviewView(ctx)
            onPreviewCreated(previewView)
            previewView
        },
        modifier = modifier
    )
}
