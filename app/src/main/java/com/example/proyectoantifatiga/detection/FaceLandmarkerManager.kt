package com.example.proyectoantifatiga.detection

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions

class FaceLandmarkerManager(
    context: Context,
    private val onResult: (FaceLandmarkerResult) -> Unit
) {
    val faceLandmarker: FaceLandmarker

    init {
        val options = FaceLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ -> onResult(result) }
            .setErrorListener { e -> Log.e("MediaPipe", "Error: ${e.message}") }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }
}
