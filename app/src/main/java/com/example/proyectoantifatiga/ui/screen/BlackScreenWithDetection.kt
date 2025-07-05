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
import com.example.proyectoantifatiga.detector.HeadDownDetector
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
    val cabezaAgachada = remember { mutableStateOf(false) }


    // 👉 Variables compartidas
    val ultimaLandmarks = remember { mutableStateOf<List<NormalizedLandmark>?>(null) }
    val estaFatigado = remember { mutableStateOf(false) }
    val estaBostezo = remember { mutableStateOf(false) }
    val nivelFatiga = remember { mutableStateOf(0f) }
    val advertenciaDistancia = remember { mutableStateOf(false) }
    val tasaParpadeo = remember { mutableStateOf(0) }
    val ear = remember { mutableStateOf(0.25f) }
    val rostroDetectado = remember { mutableStateOf(false) }
    val showFatigueMessage = remember { mutableStateOf(false) }
    val headDownDetector = remember { HeadDownDetector(context) }


    val detector = remember {
        FatigueDetector(context).apply {
            onFatigueDetected = { resultado ->
                ultimaLandmarks.value = resultado.landmarks
                ear.value = resultado.ear
                estaFatigado.value = resultado.estaFatigado
                showFatigueMessage.value = resultado.estaFatigado // 👈 AÑADIR ESTO

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
            detector = detector,
            headDownDetector = headDownDetector
        ).apply {
            setOnBitmapReadyListener { bmp ->
                bitmap.value = bmp
            }
        }
    }


    LaunchedEffect(Unit) {
        headDownDetector.onHeadDownDetected = {
            cabezaAgachada.value = true
            ServicioFatiga.iniciarAlarma(context) // o muestra un mensaje visual
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
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Cámara siempre activa (invisible si se cubre con capa negra)
        AndroidView(
            factory = { cameraView },
            modifier = Modifier.fillMaxSize()
        )


        // 3. Pantalla negra solo como máscara visual (NO debe tapar FatigueUI)
        if (!showCamera) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = 48.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        // 2. Interfaz de alertas y visualización de fatiga
        FatigueUI(
            showFatigueMessage = showFatigueMessage,
            showYawnMessage = estaBostezo,
            previewView = {}, // no mostramos la cámara aquí
            eyeLandmarks = if (showCamera) ultimaLandmarks.value else null, // oculta puntos cuando la cámara no está visible
            fatigueLevel = nivelFatiga,
            distanceWarning = advertenciaDistancia,
            blinkRate = tasaParpadeo,
            eyeAspectRatio = ear,
            faceDetected = rostroDetectado,
            headDownDetected = cabezaAgachada
        )
        // 4. Botón de mostrar/ocultar cámara
        Button(
            onClick = { showCamera = !showCamera },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            Text(if (showCamera) "Ocultar Cámara" else "Mostrar Cámara")
        }
    }


    LaunchedEffect(Unit) {
        headDownDetector.onHeadDownDetected = {
            cabezaAgachada.value = true
            ServicioFatiga.iniciarAlarma(context)
        }
        headDownDetector.onHeadUpDetected = {
            cabezaAgachada.value = false
            ServicioFatiga.detenerAlarma()
        }
    }
}
