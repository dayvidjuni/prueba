package com.example.proyectoantifatiga

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.proyectoantifatiga.utils.FatigueUI
import com.example.proyectoantifatiga.utils.BitmapUtils
import com.example.proyectoantifatiga.utils.FatigueDetector
import com.example.proyectoantifatiga.detection.FaceLandmarkerManager
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var faceLandmarkerManager: FaceLandmarkerManager
    private lateinit var fatigueDetector: FatigueDetector
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) setupFaceLandmarker()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            setupFaceLandmarker()
        }

        setContent {
            val showFatigueMessageState = remember { mutableStateOf(false) }

            // Inicializa detector con referencia a estado mutable
            fatigueDetector = FatigueDetector(this, showFatigueMessageState)

            FatigueUI(
                showFatigueMessage = showFatigueMessageState.value,
                onCameraReady = { previewView -> startCamera(previewView) }
            )
        }
    }

    private fun setupFaceLandmarker() {
        faceLandmarkerManager = FaceLandmarkerManager(this) { result ->
            fatigueDetector.checkFatigue(result)
        }
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                try {
                    val bitmap = BitmapUtils.imageProxyToBitmap(imageProxy)
                    val mpImg = BitmapImageBuilder(bitmap).build()
                    faceLandmarkerManager.faceLandmarker.detectAsync(
                        mpImg,
                        ImageProcessingOptions.builder().build(),
                        System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    imageProxy.close()
                }
            }

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

        }, ContextCompat.getMainExecutor(this))
    }
}
