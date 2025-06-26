package com.example.proyectoantifatiga.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Vibrator
import android.os.VibrationEffect
import android.util.Log
import androidx.compose.runtime.MutableState
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FatigueDetectors(
    private val context: Context,
    private val showFatigueMessage: MutableState<Boolean>,
    private val showYawnMessage: MutableState<Boolean>
) {
    private var earBelowThresholdFrames = 0
    private var yawnDetectedFrames = 0

    // Umbrales
    private val EAR_THRESHOLD = 0.23
    private val EAR_FRAMES_THRESHOLD = 15
    private val YAWN_FRAMES_THRESHOLD = 10
    private val YAWN_OPENING_THRESHOLD = 0.6

    fun checkFatigue(result: FaceLandmarkerResult, bitmap: android.graphics.Bitmap) {
        if (result.faceLandmarks().isEmpty()) {
            earBelowThresholdFrames = 0
            yawnDetectedFrames = 0
            showFatigueMessage.value = false
            showYawnMessage.value = false
            return
        }

        val landmarks = result.faceLandmarks()[0]

        // EAR: usar EyeDrawer para mantener consistencia visual
        val avgEAR = EyeDrawer.computeEARFromLandmarks(bitmap.width, bitmap.height, landmarks)

        // 🔻 Lógica de fatiga ocular (ojos cerrados prolongados)
        if (avgEAR < EAR_THRESHOLD) {
            earBelowThresholdFrames++
            if (earBelowThresholdFrames > EAR_FRAMES_THRESHOLD) {
                if (!showFatigueMessage.value) {
                    showFatigueMessage.value = true
                    vibrate()
                    playAlarm()
                    Log.d("Fatigue", "🔴 Fatiga ocular detectada")
                }
            }
        } else {
            earBelowThresholdFrames = 0
            showFatigueMessage.value = false
        }

        // 🔻 (Ejemplo simple) Detección de bostezo — puede reemplazarse por tu lógica basada en ROI
        val upperLipY = landmarks[13].y()
        val lowerLipY = landmarks[14].y()
        val mouthOpening = lowerLipY - upperLipY

        if (mouthOpening > YAWN_OPENING_THRESHOLD) {
            yawnDetectedFrames++
            if (yawnDetectedFrames > YAWN_FRAMES_THRESHOLD) {
                if (!showYawnMessage.value) {
                    showYawnMessage.value = true
                    vibrate()
                    Log.d("Yawn", "🟡 Bostezo detectado")
                }
            }
        } else {
            yawnDetectedFrames = 0
            showYawnMessage.value = false
        }
    }

    private fun vibrate() {
        try {
            val vibrator = context.getSystemService(Vibrator::class.java)
            val effect = VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator?.vibrate(effect)
        } catch (e: Exception) {
            Log.e("Vibration", "❌ Error vibrando: ${e.message}")
        }
    }

    private fun playAlarm() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        } catch (e: Exception) {
            Log.e("Alarm", "❌ Error al reproducir tono: ${e.message}")
        }
    }
}
