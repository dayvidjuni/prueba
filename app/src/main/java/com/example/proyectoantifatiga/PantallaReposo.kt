package com.example.proyectoantifatiga

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.viewinterop.AndroidView
import com.example.proyectoantifatiga.camera.CameraController
import com.example.proyectoantifatiga.detector.FatigueDetector
import com.example.proyectoantifatiga.detector.HeadDownDetector
import com.example.proyectoantifatiga.ui.FatigueUI
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PantallaReposo : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PantallaReposo", "Entrando a PantallaReposo")

        val detector = FatigueDetector(this)
        val headDownDetector = HeadDownDetector(this)


        val cameraController = CameraController(
            this, // context
            CameraSelector.LENS_FACING_FRONT, // lente
            detector, // FatigueDetector
            headDownDetector // HeadDownDetector
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
        var onYawnDetected: (() -> Unit)? = null
        val cabezaAgachada = mutableStateOf(false)



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
                faceDetected = rostroDetectado,
                headDownDetected = cabezaAgachada
            )
        }
    }
}

//pantalla reposo se combina con la pantalla black
