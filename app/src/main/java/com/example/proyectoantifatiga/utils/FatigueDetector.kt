package com.example.proyectoantifatiga.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.MutableState
import com.example.proyectoantifatiga.R
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.*
import kotlin.math.abs

class FatigueDetector(
    private val context: Context,
    private val showFatigueMessage: MutableState<Boolean>,
    private val fatigueDurationMillis: Long = 3000L,
    private val eyeClosedThreshold: Float = 0.025f
) {
    private var eyeClosedStartTime: Long? = null
    private var mediaPlayer: MediaPlayer? = null
    private var fatigueHandled = false
    private val scope = CoroutineScope(Dispatchers.Main)

    fun checkFatigue(result: FaceLandmarkerResult) {
        if (fatigueHandled) return

        val faces = result.faceLandmarks()
        if (faces.size != 1) {
            reset()
            return
        }

        val landmarks = faces.first()
        if (landmarks.size <= 386) {
            reset()
            return
        }

        val leftEyeOpen = abs(landmarks[159].y() - landmarks[145].y())
        val rightEyeOpen = abs(landmarks[386].y() - landmarks[374].y())
        val eyeAvg = (leftEyeOpen + rightEyeOpen) / 2f

        Log.d("FatigueDetector", "Eye avg: $eyeAvg")

        val leftClosed = leftEyeOpen < eyeClosedThreshold
        val rightClosed = rightEyeOpen < eyeClosedThreshold

        if (leftClosed && rightClosed) {
            if (eyeClosedStartTime == null) {
                eyeClosedStartTime = System.currentTimeMillis()
            }

            val elapsed = System.currentTimeMillis() - eyeClosedStartTime!!
            if (elapsed >= fatigueDurationMillis && !fatigueHandled) {
                showFatigueMessage.value = true
                playAlarm()
                fatigueHandled = true

                // ⏳ Esperar 2 segundos y luego resetear todo
                scope.launch {
                    delay(2000L)
                    reset()
                }
            }
        } else {
            reset()
        }
    }

    private fun reset() {
        eyeClosedStartTime = null
        showFatigueMessage.value = false
        stopAlarm()
        fatigueHandled = false
    }

    private fun playAlarm() {
        stopAlarm()
        mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.alarma)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(1.0f, 1.0f)
        mediaPlayer?.start()
        Log.d("FatigueDetector", "🔔 Alarma iniciada")
    }

    private fun stopAlarm() {
        if (mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer?.stop()
                Log.d("FatigueDetector", "🔕 Alarma detenida")
            }
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}
