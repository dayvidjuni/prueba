package com.example.proyectoantifatiga.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.runtime.MutableState
import com.example.proyectoantifatiga.R
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.*
import org.opencv.android.Utils
import org.opencv.core.Mat
import kotlin.math.abs

class FatigueDetector(
    private val context: Context,
    private val showFatigueMessage: MutableState<Boolean>,
    private val showYawnMessage: MutableState<Boolean>,
    private val fatigueDurationMillis: Long = 2000L,
    private val eyeClosedThreshold: Float = 0.0045f,
    private val yawnThreshold: Float = 0.002f,
    private val yawnDurationMillis: Long = 1000L
) {
    private var eyeClosedStartTime: Long? = null
    private var mouthOpenStartTime: Long? = null
    private var mediaPlayer: MediaPlayer? = null
    private var fatigueHandled = false
    private var yawnHandled = false
    private var lastYawnTime: Long = 0L
    private val scope = CoroutineScope(Dispatchers.Main)

    fun checkFatigue(result: FaceLandmarkerResult) {
        val faces = result.faceLandmarks()
        if (faces.size != 1) {
            reset()
            return
        }

        val landmarks = faces.first()
        if (landmarks.size <= 386) {
            Log.w("FatigueDetector", "⚠️ Landmarks incompletos. Frame ignorado.")
            reset()
            return
        }

        // ✅ Procesar bitmap actual con OpenCV
        val inputBitmap: Bitmap = BitmapUtils.latestBitmap ?: return
        val mat = Mat()
        Utils.bitmapToMat(inputBitmap, mat)
        EyeDrawer.drawEyesAndEAR(mat, result)
        // Aquí puedes mostrar o guardar `bitmapWithEyes` si lo deseas

        // 👁 Cálculo de apertura de ojos (EAR simplificado)
        val leftEyeOpen = abs(landmarks[159].y() - landmarks[145].y())
        val rightEyeOpen = abs(landmarks[386].y() - landmarks[374].y())
        val leftClosed = leftEyeOpen < eyeClosedThreshold
        val rightClosed = rightEyeOpen < eyeClosedThreshold
        val bothEyesClosed = leftClosed && rightClosed

        Log.d("FatigueDetector", "👁️ Left: $leftEyeOpen | Right: $rightEyeOpen")

        // 👄 Cálculo apertura de boca
        val upperLip = landmarks[13].y()
        val lowerLip = landmarks[14].y()
        val mouthOpen = abs(lowerLip - upperLip)

        Log.d("FatigueDetector", "👄 Mouth Open: $mouthOpen")

        // 😮 Detección de bostezo
        if (mouthOpen > yawnThreshold) {
            if (mouthOpenStartTime == null) {
                mouthOpenStartTime = System.currentTimeMillis()
            }

            val elapsed = System.currentTimeMillis() - mouthOpenStartTime!!
            if (elapsed >= yawnDurationMillis && !yawnHandled) {
                showYawnMessage.value = true
                vibrate()
                yawnHandled = true
                lastYawnTime = System.currentTimeMillis()

                scope.launch {
                    delay(2000L)
                    showYawnMessage.value = false
                    yawnHandled = false
                }
            }
        } else {
            mouthOpenStartTime = null
        }

        // Ignorar fatiga ocular si hubo bostezo reciente
        if (System.currentTimeMillis() - lastYawnTime < 2000L) {
            Log.d("FatigueDetector", "⏳ Ignorando fatiga tras bostezo reciente")
            return
        }

        // 💤 Detección de fatiga ocular
        if (mouthOpen <= yawnThreshold && bothEyesClosed) {
            if (eyeClosedStartTime == null) {
                eyeClosedStartTime = System.currentTimeMillis()
            }

            val elapsed = System.currentTimeMillis() - eyeClosedStartTime!!
            if (elapsed >= fatigueDurationMillis && !fatigueHandled) {
                showFatigueMessage.value = true
                playAlarm()
                fatigueHandled = true

                scope.launch {
                    delay(2000L) // muestra mensaje al menos 2s
                }
            }
        } else {
            if (fatigueHandled) {
                stopAlarm()
                showFatigueMessage.value = false
                fatigueHandled = false
                Log.d("FatigueDetector", "✅ Ojos abiertos, fatiga cancelada")
            }
            eyeClosedStartTime = null
        }
    }

    private fun reset() {
        resetFatigue()
        showYawnMessage.value = false
        yawnHandled = false
        mouthOpenStartTime = null
    }

    private fun resetFatigue() {
        eyeClosedStartTime = null
        showFatigueMessage.value = false
        stopAlarm()
        fatigueHandled = false
    }

    private fun playAlarm() {
        stopAlarm()
        mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.alarma)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        Log.d("FatigueDetector", "🔔 Alarma iniciada")
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d("FatigueDetector", "🔕 Alarma detenida")
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
        Log.d("FatigueDetector", "📳 Vibración activada por bostezo")
    }
}
