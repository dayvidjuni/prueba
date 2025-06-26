package com.example.proyectoantifatiga.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.proyectoantifatiga.detector.FatigueDetector

// camera/CameraController.kt
class CameraController(
    private val context: Context,
    private val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    private val detector: FatigueDetector

) {
    private var onBitmapReady: ((Bitmap) -> Unit)? = null

    fun setOnBitmapReadyListener(listener: (Bitmap) -> Unit) {
        onBitmapReady = listener
    }
    fun startAndGetView(lifecycleOwner: LifecycleOwner): PreviewView {
        val previewView = PreviewView(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder().build().apply {
                setAnalyzer(ContextCompat.getMainExecutor(context), ImageAnalyzer(detector))
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer)
        }, ContextCompat.getMainExecutor(context))

        return previewView
    }
}




