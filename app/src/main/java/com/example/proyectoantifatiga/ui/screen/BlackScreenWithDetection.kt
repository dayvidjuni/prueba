package com.example.proyectoantifatiga.ui.screen

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.proyectoantifatiga.camera.CameraController
import com.example.proyectoantifatiga.detector.EyeDrawer
import com.example.proyectoantifatiga.detector.FatigueDetector
import com.example.proyectoantifatiga.service.ServicioFatiga
import com.example.proyectoantifatiga.ui.FatigueUI
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BlackScreenWithDetection() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showCamera by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("") }
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }

    // 👉 Variables compartidas
    val ultimaLandmarks = remember { mutableStateOf<List<NormalizedLandmark>?>(null) }
    val estaFatigado = remember { mutableStateOf(false) }
    val estaBostezo = remember { mutableStateOf(false) }
    val nivelFatiga = remember { mutableStateOf(0f) }
    val advertenciaDistancia = remember { mutableStateOf(false) }
    val tasaParpadeo = remember { mutableStateOf(0) }
    val ear = remember { mutableStateOf(0.25f) }
    val rostroDetectado = remember { mutableStateOf(false) }

    val detector = remember {
        FatigueDetector(context).apply {
            onFatigueDetected = { resultado ->
                ultimaLandmarks.value = resultado.landmarks
                ear.value = resultado.ear
                estaFatigado.value = resultado.estaFatigado
                // Puedes ajustar nivelFatiga o tasaParpadeo aquí si lo usas

                if (resultado.estaFatigado) {
                    ServicioFatiga.iniciarAlarma(context)
                } else {
                    ServicioFatiga.detenerAlarma()
                }

                if (bitmap.value != null && resultado.landmarks != null) {
                    val bmpConOjos = EyeDrawer.drawEyes(bitmap.value!!, resultado.landmarks)

                    bitmap.value = bmpConOjos
                }
            }
        }
    }

    val cameraController = remember {
        CameraController(
            context = context,
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            detector = detector
        ).apply {
            setOnBitmapReadyListener { bmp ->
                bitmap.value = bmp
            }
        }
    }

    val cameraView = remember {
        cameraController.startAndGetView(lifecycleOwner)
    }

    // ⏱ Actualiza la hora cada segundo si la cámara está oculta
    LaunchedEffect(showCamera) {
        while (!showCamera) {
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    // 🖤 UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (showCamera) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { cameraView }
            )

            FatigueUI(
                showFatigueMessage = estaFatigado,
                showYawnMessage = estaBostezo,
                previewView = {}, // Vista ya está mostrada arriba
                eyeLandmarks = ultimaLandmarks.value,
                fatigueLevel = nivelFatiga,
                distanceWarning = advertenciaDistancia,
                blinkRate = tasaParpadeo,
                eyeAspectRatio = ear,
                faceDetected = rostroDetectado
            )
        } else {
            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 48.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Button(
            onClick = { showCamera = !showCamera },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Text(if (showCamera) "Ocultar Cámara" else "Mostrar Cámara")
        }
    }
    LaunchedEffect(bitmap.value) {
        bitmap.value?.let {
            detector.detectAsync(it)
        }
    }
}
