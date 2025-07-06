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
import kotlin.math.*

class HeadDownDetector(private val context: Context) {

    private var inicioCabezaAgachada: Long = 0
    private val umbralAnguloGrados = 35.0
    private val tiempoDeteccionMs = 500L
    var onHeadDownDetected: (() -> Unit)? = null
    var onHeadUpDetected: (() -> Unit)? = null

    private var detector: FaceLandmarker? = null

    init {
        val options = FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build()
            )
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .build()

        detector = FaceLandmarker.createFromOptions(context, options)
    }

    suspend fun detectAsync(bitmap: Bitmap) = withContext(Dispatchers.Default) {
        detector?.let {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = it.detect(mpImage)

            if (result.faceLandmarks().isNotEmpty()) {
                val landmarks = result.faceLandmarks()[0]

                val frente = landmarks[10]
                val menton = landmarks[152]

                val dy = menton.y() - frente.y()
                val dx = menton.x() - frente.x()

                val angulo = Math.toDegrees(atan2(dy, dx).toDouble())
                val anguloRelativo = abs(90 - angulo)

                val cabezaAgachada = anguloRelativo > umbralAnguloGrados

                if (cabezaAgachada) {
                    if (inicioCabezaAgachada == 0L) {
                        inicioCabezaAgachada = System.currentTimeMillis()
                    } else {
                        val duracion = System.currentTimeMillis() - inicioCabezaAgachada
                        if (duracion > tiempoDeteccionMs) {
                            onHeadDownDetected?.invoke()
                        }
                    }
                } else {
                    if (inicioCabezaAgachada != 0L) {
                        onHeadUpDetected?.invoke()
                    }
                    inicioCabezaAgachada = 0L
                }
            } else {
                inicioCabezaAgachada = 0L
            }
        }
    }

    fun close() {
        detector?.close()
    }
}

//entender y mejorar parte daniel
