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
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

class FatigueDetector(
    private val context: Context,
    private val showFatigueMessage: MutableState<Boolean>,
    private val showYawnMessage: MutableState<Boolean>,
    private val fatigueFrameThreshold: Int = 15,
    private val earSampleFrames: Int = 30
) {
    private var eyeClosedFrameCount = 0
    private var mediaPlayer: MediaPlayer? = null
    private var fatigueHandled = false
    private var yawnHandled = false
    private var lastYawnTime: Long = 0L
    private val scope = CoroutineScope(Dispatchers.Main)

    private val earSamples = mutableListOf<Float>()
    private var initialEarAverage: Float = -1f
    private val earClosedRatio = 0.6f

    // Yawn config
    private val yawnVerticalThreshold = 0.04f
    private val yawnContourAreaThreshold = 500.0
    private val yawnFrameThreshold = 10
    private var yawnFrameCount = 0

    fun checkFatigue(result: FaceLandmarkerResult, bitmap: Bitmap) {
        val faces = result.faceLandmarks()
        if (faces.size != 1) {
            reset()
            return
        }

        val landmarks = faces.first()
        if (landmarks.size <= 386) {
            Log.w("FatigueDetector", "⚠️ Landmarks incompletos")
            reset()
            return
        }

        // EAR (ojos)
        val leftEyeOpen = abs(landmarks[159].y() - landmarks[145].y())
        val rightEyeOpen = abs(landmarks[386].y() - landmarks[374].y())
        val avgEAR = ((leftEyeOpen + rightEyeOpen) / 2f)

        if (earSamples.size < earSampleFrames) {
            earSamples.add(avgEAR)
            if (earSamples.size == earSampleFrames) {
                initialEarAverage = earSamples.average().toFloat()
                Log.d("FatigueDetector", "🎯 EAR baseline: $initialEarAverage")
            }
            return
        }

        if (initialEarAverage <= 0f) return

        val dynamicThreshold = initialEarAverage * earClosedRatio
        val eyesClosed = avgEAR < dynamicThreshold
        Log.d("FatigueDetector", "👁️ EAR: $avgEAR | Umbral: $dynamicThreshold")

        // Bostezo (combinado)
        val mouthOpen = abs(landmarks[14].y() - landmarks[13].y())
        val validVertical = mouthOpen > yawnVerticalThreshold

        // ROI de la boca usando landmarks
        val mouthTop = landmarks[13]
        val mouthBottom = landmarks[14]
        val mouthLeft = landmarks[78]
        val mouthRight = landmarks[308]

        val rectX = (mouthLeft.x() * bitmap.width).toInt()
        val rectY = (mouthTop.y() * bitmap.height).toInt()
        val rectWidth = ((mouthRight.x() - mouthLeft.x()) * bitmap.width).toInt()
        val rectHeight = ((mouthBottom.y() - mouthTop.y()) * bitmap.height).toInt()

        if (rectWidth <= 0 || rectHeight <= 0) return

        val mouthROI = Rect(rectX, rectY, rectWidth, rectHeight)

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
        val roi = Mat(mat, mouthROI)
        Imgproc.GaussianBlur(roi, roi, Size(3.0, 3.0), 0.0)
        Imgproc.threshold(roi, roi, 50.0, 255.0, Imgproc.THRESH_BINARY)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(roi.clone(), contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        var foundLargeContour = false
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            Log.d("YawnDetection", "🔍 Contour area: $area")
            if (area > yawnContourAreaThreshold) {
                foundLargeContour = true
                break
            }
        }

        if (validVertical && foundLargeContour) {
            yawnFrameCount++
            if (yawnFrameCount >= yawnFrameThreshold && !yawnHandled) {
                showYawnMessage.value = true
                vibrate()
                yawnHandled = true
                lastYawnTime = System.currentTimeMillis()
                scope.launch {
                    delay(2000)
                    showYawnMessage.value = false
                    yawnHandled = false
                }
            }
        } else {
            yawnFrameCount = 0
        }

        // Ignorar fatiga tras bostezo reciente
        if (System.currentTimeMillis() - lastYawnTime < 2000L) {
            eyeClosedFrameCount = 0
            return
        }

        if (eyesClosed) {
            eyeClosedFrameCount++
            if (eyeClosedFrameCount >= fatigueFrameThreshold && !fatigueHandled) {
                showFatigueMessage.value = true
                playAlarm()
                fatigueHandled = true
            }
        } else {
            if (fatigueHandled) {
                showFatigueMessage.value = false
                stopAlarm()
                fatigueHandled = false
            }
            eyeClosedFrameCount = 0
        }
    }

    private fun reset() {
        showFatigueMessage.value = false
        showYawnMessage.value = false
        eyeClosedFrameCount = 0
        yawnFrameCount = 0
        stopAlarm()
    }

    private fun playAlarm() {
        stopAlarm()
        mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.alarma)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }
}
