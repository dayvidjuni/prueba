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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
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
                val showBlackScreen = remember { mutableStateOf(false) }

                fatigueDetector = remember {
                    FatigueDetector(
                        context,
                        showFatigueMessage,
                        showYawnMessage
                    )
                }

                if (showBlackScreen.value) {
                    // Pantalla negra con detección activa
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Cámara invisible pero funcionando
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                startCamera(previewView)
                                previewView
                            },
                            modifier = Modifier
                                .size(1.dp) // Casi invisible
                                .align(Alignment.TopStart)
                        )

                        // Pantalla negra encima
                        BlackScreenWithDetection(
                            showFatigueMessage = showFatigueMessage,
                            showYawnMessage = showYawnMessage,
                            onBackClick = { showBlackScreen.value = false }
                        )
                    }
                } else {
                    // Pantalla de cámara con botón
                    FatigueUI(
                        showFatigueMessage = showFatigueMessage,
                        showYawnMessage = showYawnMessage,
                        previewView = {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Vista de la cámara
                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx)
                                        startCamera(previewView)
                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Botón para ir a pantalla negra
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
                    Log.d("FaceLandmarker", "Face detected: ${result.faceLandmarks().size} faces")
                    onResult(result)
                }
                .setErrorListener { e -> Log.e("MediaPipe", "Error: ${e.message}") }
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(this, options)
            Log.d("FaceLandmarker", "FaceLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e("FaceLandmarker", "Failed to initialize: ${e.message}")
        }
    }

    private fun onResult(result: FaceLandmarkerResult) {
        if (::fatigueDetector.isInitialized) {
            fatigueDetector.checkFatigue(result)
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
                            val mpImage = BitmapImageBuilder(bitmap).build()
                            faceLandmarker.detectAsync(
                                mpImage,
                                ImageProcessingOptions.builder().build(),
                                System.currentTimeMillis()
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("Analyzer", "Error processing image: ${e.message}")
                    } finally {
                        imageProxy.close()
                    }
                }

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

                Log.d("Camera", "Camera started successfully")
            } catch (e: Exception) {
                Log.e("Camera", "Failed to start camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }
}

@Composable
fun BlackScreenWithDetection(
    showFatigueMessage: MutableState<Boolean>,
    showYawnMessage: MutableState<Boolean>,
    onBackClick: () -> Unit
) {
    // Estado para actualizar la hora cada segundo
    var currentTime by remember { mutableStateOf(getCurrentTime()) }

    // Actualizar la hora cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = getCurrentTime()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Hora en el centro
        Text(
            text = currentTime,
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        // Mensajes de fatiga en la parte superior
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showFatigueMessage.value) {
                Card(
                    modifier = Modifier.padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = "¡FATIGA DETECTADA!",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showYawnMessage.value) {
                Card(
                    modifier = Modifier.padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = "Bostezo detectado",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Botón para volver
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Volver a Cámara", color = Color.White)
        }
    }
}

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}