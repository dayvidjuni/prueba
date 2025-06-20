package com.example.proyectoantifatiga

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.proyectoantifatiga.utils.FatigueUI
import com.example.proyectoantifatiga.detection.FaceLandmarkerManager
import com.example.proyectoantifatiga.utils.BitmapUtils
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var faceLandmarkerManager: FaceLandmarkerManager
    private val executor = Executors.newSingleThreadExecutor()
    private var showFatigueMessageRef: MutableState<Boolean>? = null
    private var mediaPlayer: MediaPlayer? = null
    private var eyeClosedStartTime: Long? = null
    private val fatigueDurationMillis = 3000L
    private val EYE_CLOSED_THRESHOLD = 0.01f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
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
            val showFatigueMessageState = remember { mutableStateOf(false) }
            showFatigueMessageRef = showFatigueMessageState

            FatigueUI(
                showFatigueMessage = showFatigueMessageState.value,
                onCameraReady = { previewView -> startCamera(previewView) }
            )
        }
    }

    private fun setupFaceLandmarker() {
        faceLandmarkerManager = FaceLandmarkerManager(this) { result ->
            checkFatigue(result)
        }
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
                    val mpImg = BitmapImageBuilder(bitmap).build()
                    faceLandmarkerManager.faceLandmarker.detectAsync(
                        mpImg,
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

    private fun checkFatigue(result: FaceLandmarkerResult) {
        val faces = result.faceLandmarks()
        if (faces.size != 1) {
            eyeClosedStartTime = null
            showFatigueMessageRef?.value = false
            stopAlarm()
            return
        }

        val landmarks = faces.first()
        if (landmarks.size <= 386) {
            eyeClosedStartTime = null
            showFatigueMessageRef?.value = false
            stopAlarm()
            return
        }

        val leftEyeOpen = kotlin.math.abs(landmarks[159].y() - landmarks[145].y())
        val rightEyeOpen = kotlin.math.abs(landmarks[386].y() - landmarks[374].y())
        val eyeAvg = (leftEyeOpen + rightEyeOpen) / 2f

        runOnUiThread {
            if (eyeAvg < EYE_CLOSED_THRESHOLD) {
                if (eyeClosedStartTime == null) {
                    eyeClosedStartTime = System.currentTimeMillis()
                }
                val elapsed = System.currentTimeMillis() - eyeClosedStartTime!!
                if (elapsed >= fatigueDurationMillis) {
                    if (showFatigueMessageRef?.value != true) {
                        showFatigueMessageRef?.value = true
                        playAlarm()
                    }
                }
            } else {
                eyeClosedStartTime = null
                showFatigueMessageRef?.value = false
                stopAlarm()
            }
        }
    }

    private fun playAlarm() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarma)
            mediaPlayer?.isLooping = true
        }
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    private fun stopAlarm() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            mediaPlayer?.seekTo(0)
        }
    }
}

