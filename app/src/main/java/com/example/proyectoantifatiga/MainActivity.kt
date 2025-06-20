package com.example.proyectoantifatiga

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.proyectoantifatiga.ui.FatigueUI
import com.example.proyectoantifatiga.ui.theme.ProyectoAntifatigaTheme
import com.example.proyectoantifatiga.utils.BitmapUtils
import com.example.proyectoantifatiga.utils.FatigueDetector
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var faceLandmarker: FaceLandmarker
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var fatigueDetector: FatigueDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
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
            ProyectoAntifatigaTheme {
                val context = LocalContext.current
                val showFatigueMessage = remember { mutableStateOf(false) }
                val showYawnMessage = remember { mutableStateOf(false) }

                fatigueDetector = remember {
                    FatigueDetector(
                        context,
                        showFatigueMessage,
                        showYawnMessage
                    )
                }

                FatigueUI(
                    showFatigueMessage = showFatigueMessage,
                    showYawnMessage = showYawnMessage,
                    previewView = {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                startCamera(previewView)
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            }
        }
    }

    private fun setupFaceLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ -> onResult(result) }
            .setErrorListener { e -> Log.e("MediaPipe", "Error: ${e.message}") }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(this, options)
    }

    private fun onResult(result: FaceLandmarkerResult) {
        fatigueDetector.checkFatigue(result)
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
                    val mpImage = BitmapImageBuilder(bitmap).build()
                    faceLandmarker.detectAsync(
                        mpImage,
                        ImageProcessingOptions.builder().build(),
                        System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e("Analyzer", "Error: ${e.message}")
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
