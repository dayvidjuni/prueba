package com.example.proyectoantifatiga

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.example.proyectoantifatiga.camera.CameraController
import com.example.proyectoantifatiga.detector.FatigueDetector
import com.example.proyectoantifatiga.service.ServicioFatiga
import com.example.proyectoantifatiga.ui.FatigueUI
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PantallaReposo : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PantallaReposo", "Entrando a PantallaReposo")

        val detector = FatigueDetector(this)

        val cameraController = CameraController(
            context = this,
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            detector = detector
        )

        val ear = mutableStateOf(0f)
        val estaFatigado = mutableStateOf(false)
        val estaBostezo = mutableStateOf(false)
        val advertenciaDistancia = mutableStateOf(false)
        val tasaParpadeo = mutableStateOf(0)
        val nivelFatiga = mutableStateOf(0f)
        val rostroDetectado = mutableStateOf(false)
        val ultimaLandmarks = mutableStateOf<List<NormalizedLandmark>>(emptyList())
        var onEyeLandmarksUpdated: ((List<NormalizedLandmark>) -> Unit)? = null
        var onFatigueLevelUpdated: ((Float) -> Unit)? = null
        var onBlinkRateUpdated: ((Int) -> Unit)? = null
        var onDistanceWarning: ((Boolean) -> Unit)? = null
        var onFaceDetected: ((Boolean) -> Unit)? = null
        var onYawnDetected: (() -> Unit)? = null

        // Configura los callbacks del detector
        detector.apply {
            onFatigueDetected = {
                Log.d("PantallaReposo", "Fatiga detectada: ${it.ear}, ${it.estaFatigado}")
                ear.value = it.ear
                estaFatigado.value = it.estaFatigado
            }
            onYawnDetected = {
                Log.d("PantallaReposo", "Bostezo detectado")
                estaBostezo.value = true
            }
            onEyeLandmarksUpdated = {
                ultimaLandmarks.value = it
            }
            onBlinkRateUpdated = {
                tasaParpadeo.value = it
            }
            onFatigueLevelUpdated = {
                nivelFatiga.value = it
            }
            onDistanceWarning = {
                advertenciaDistancia.value = it
            }
            onFaceDetected = {
                rostroDetectado.value = it
            }
        }

        setContent {
            Log.d("PantallaReposo", "Comenzando Composable")
            FatigueUI(
                showFatigueMessage = estaFatigado,
                showYawnMessage = estaBostezo,
                previewView = {
                    AndroidView(factory = {
                        Log.d("PantallaReposo", "Creando PreviewView")
                        cameraController.startAndGetView(this)
                    })
                },
                eyeLandmarks = ultimaLandmarks.value,
                fatigueLevel = nivelFatiga,
                distanceWarning = advertenciaDistancia,
                blinkRate = tasaParpadeo,
                eyeAspectRatio = ear,
                faceDetected = rostroDetectado
            )
        }
    }
}

@Composable
fun PantallaNegraConFatigueUI(context: Context) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val showFatigueMessage = remember { mutableStateOf(false) }
    val showYawnMessage = remember { mutableStateOf(false) }
    val eyeLandmarks = remember { mutableStateOf<List<NormalizedLandmark>?>(null) }
    val fatigueLevel = remember { mutableStateOf(0f) }
    val distanceWarning = remember { mutableStateOf(false) }
    val blinkRate = remember { mutableStateOf(0) }
    val eyeAspectRatio = remember { mutableStateOf(0.25f) }
    val faceDetected = remember { mutableStateOf(false) }
    var onEyeLandmarksUpdated: ((List<NormalizedLandmark>) -> Unit)? = null
    var onFatigueLevelUpdated: ((Float) -> Unit)? = null
    var onBlinkRateUpdated: ((Int) -> Unit)? = null
    var onDistanceWarning: ((Boolean) -> Unit)? = null
    var onFaceDetected: ((Boolean) -> Unit)? = null
    var onYawnDetected: (() -> Unit)? = null

    val detector = remember {
        FatigueDetector(context).apply {
            onFatigueDetected = { resultado ->
                showFatigueMessage.value = resultado.estaFatigado
                eyeAspectRatio.value = resultado.ear
                if (resultado.estaFatigado) {
                    ServicioFatiga.iniciarAlarma(context)
                } else {
                    ServicioFatiga.detenerAlarma()
                }
            }

            onYawnDetected = {
                showYawnMessage.value = true
            }

            onEyeLandmarksUpdated = {
                eyeLandmarks.value = it
            }

            onFatigueLevelUpdated = {
                fatigueLevel.value = it
            }

            onBlinkRateUpdated = {
                blinkRate.value = it
            }

            onDistanceWarning = {
                distanceWarning.value = it
            }

            onFaceDetected = {
                faceDetected.value = it
            }
        }
    }

    val cameraController = remember {
        CameraController(
            context = context,
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            detector = detector
        )
    }

    val previewView = remember { mutableStateOf<android.view.View?>(null) }

    LaunchedEffect(Unit) {
        previewView.value = cameraController.startAndGetView(lifecycleOwner)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        previewView.value?.let { view ->
            FatigueUI(
                showFatigueMessage = showFatigueMessage,
                showYawnMessage = showYawnMessage,
                previewView = {
                    AndroidView(
                        factory = { view },
                        modifier = Modifier.fillMaxSize()
                    )
                },
                eyeLandmarks = null, // ❌ Ocultamos puntos
                fatigueLevel = fatigueLevel,
                distanceWarning = distanceWarning,
                blinkRate = blinkRate,
                eyeAspectRatio = eyeAspectRatio,
                faceDetected = faceDetected
            )
        }
    }
}
