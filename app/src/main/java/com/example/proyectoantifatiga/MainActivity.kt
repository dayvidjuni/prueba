package com.example.proyectoantifatiga

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.proyectoantifatiga.screen.BlackScreenWithDetection
import com.example.proyectoantifatiga.ui.FatigueUI
import com.example.proyectoantifatiga.ui.theme.ProyectoAntifatigaTheme
import com.example.proyectoantifatiga.utils.BitmapUtils
import com.example.proyectoantifatiga.utils.EyeDrawer
import com.example.proyectoantifatiga.utils.FatigueDetector
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var faceLandmarker: FaceLandmarker
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var fatigueDetector: FatigueDetector
    private var latestBitmap: Bitmap? = null

    private val fatigueOverlayBitmap = mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            System.loadLibrary("opencv_java4")
            Log.d("OpenCV", "✅ OpenCV cargado correctamente")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("OpenCV", "❌ Error cargando OpenCV: ${e.message}")
        }

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
                val showBlackScreen = remember { mutableStateOf(false) }

                fatigueDetector = remember {
                    FatigueDetector(
                        context,
                        showFatigueMessage,
                        showYawnMessage
                    )
                }

                val startCameraLambda: (PreviewView) -> Unit = { previewView ->
                    startCamera(previewView)
                }

                if (showBlackScreen.value) {
                    BlackScreenWithDetection(
                        showFatigueMessage,
                        showYawnMessage,
                        onBackClick = { showBlackScreen.value = false },
                        startCamera = startCameraLambda
                    )
                } else {
                    FatigueUI(
                        showFatigueMessage,
                        showYawnMessage,
                        previewView = {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx)
                                        startCameraLambda(previewView)
                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                Button(
                                    onClick = { showBlackScreen.value = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp)
                                ) {
                                    Text("Pantalla Negra")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun setupFaceLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ ->
                    latestBitmap?.let { bitmap ->
                        fatigueDetector.checkFatigue(result, bitmap)

                        // Dibujar ojos y EAR sobre la imagen actual
                        val matBitmap = EyeDrawer.drawEyesAndEAR(BitmapUtils.bitmapToMat(bitmap), result)
                        fatigueOverlayBitmap.value = matBitmap
                    }
                }
                .setErrorListener { e -> Log.e("MediaPipe", "Error: ${e.message}") }
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(this, options)
            Log.d("FaceLandmarker", "✅ Inicializado correctamente")
        } catch (e: Exception) {
            Log.e("FaceLandmarker", "❌ Error inicializando: ${e.message}")
        }
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    try {
                        if (::faceLandmarker.isInitialized) {
                            val bitmap = BitmapUtils.imageProxyToBitmap(imageProxy)
                            latestBitmap = bitmap

                            val mpImage = BitmapImageBuilder(bitmap).build()
                            faceLandmarker.detectAsync(
                                mpImage,
                                ImageProcessingOptions.builder().build(),
                                System.currentTimeMillis()
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("Analyzer", "❌ Error procesando imagen: ${e.message}")
                    } finally {
                        imageProxy.close()
                    }
                }

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

                Log.d("Camera", "📷 Cámara iniciada")
            } catch (e: Exception) {
                Log.e("Camera", "❌ Error iniciando cámara: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
