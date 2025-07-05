package com.example.proyectoantifatiga.detector

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot

class FatigueDetector(private val context: Context) {
    private var tiempoOjosCerrados: Long = 0
    private var inicioCierreOjos: Long = 0
    var onFatigueDetected: ((ResultadoFatiga) -> Unit)? = null
    var onFaceDetected: ((Boolean) -> Unit)? = null
    var ultimaLandmarks: List<NormalizedLandmark>? = null
        private set
    suspend fun detectAsync(bitmap: Bitmap) {
        val result = analizarFatiga(bitmap)
        onFatigueDetected?.invoke(result)
    }

    private suspend fun analizarFatiga(bitmap: Bitmap): ResultadoFatiga {
        return withContext(Dispatchers.Default) {
            val mpImage: MPImage = BitmapImageBuilder(bitmap).build()

            val options = FaceLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath("face_landmarker.task")
                        .build()
                )
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(1)
                .build()

            val detector = FaceLandmarker.createFromOptions(context, options)
            val result: FaceLandmarkerResult = detector.detect(mpImage)
            detector.close()

            if (result.faceLandmarks().isNotEmpty()) {
                val landmarks = result.faceLandmarks()[0]
                ultimaLandmarks = landmarks
                onFaceDetected?.invoke(true)
                val ear = calcularEAR(landmarks)
                val ojosCerrados = ear < 0.21f  // Usa tu mismo umbral EAR

                if (ojosCerrados) {
                    if (inicioCierreOjos == 0L) {
                        inicioCierreOjos = System.currentTimeMillis()
                    } else {
                        tiempoOjosCerrados = System.currentTimeMillis() - inicioCierreOjos
                    }
                } else {
                    inicioCierreOjos = 0
                    tiempoOjosCerrados = 0
                }

                val estaFatigado = tiempoOjosCerrados >= 2000L
                ResultadoFatiga(ear, estaFatigado, landmarks)
            } else {
                ultimaLandmarks = null
                onFaceDetected?.invoke(false)
                ResultadoFatiga(ear = 1.0f, estaFatigado = false, landmarks = null)
            }
        }
    }

    private fun calcularEAR(landmarks: List<NormalizedLandmark>): Float {
        val arriba = landmarks[159]
        val abajo = landmarks[145]
        val izquierdo = landmarks[33]
        val derecho = landmarks[133]

        val vertical = hypot(arriba.x() - abajo.x(), arriba.y() - abajo.y())
        val horizontal = hypot(izquierdo.x() - derecho.x(), izquierdo.y() - derecho.y())

        return if (horizontal != 0f) vertical / horizontal else 0f
    }

    data class ResultadoFatiga(
        val ear: Float,
        val estaFatigado: Boolean,
        val landmarks: List<NormalizedLandmark>? = null
    )
}
