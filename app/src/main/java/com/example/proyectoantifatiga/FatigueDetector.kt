package com.tuapp.fatigadetector

import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FatigueDetector {

    private val LEFT_EYE = listOf(33, 160, 158, 133, 153, 144)
    private val RIGHT_EYE = listOf(362, 385, 387, 263, 373, 380)

    private var eyeClosedFrames = 0
    private val EAR_THRESHOLD = 0.25
    private val MAX_CLOSED_FRAMES = 15

    fun process(result: FaceLandmarkerResult): Boolean {
        if (result.faceLandmarks().isEmpty()) return false

        val landmarks = result.faceLandmarks()[0]

        val leftEAR = computeEAR(landmarks, LEFT_EYE)
        val rightEAR = computeEAR(landmarks, RIGHT_EYE)
        val avgEAR = (leftEAR + rightEAR) / 2.0

        Log.d("EAR", "Left: %.3f, Right: %.3f, Avg: %.3f".format(leftEAR, rightEAR, avgEAR))

        if (avgEAR < EAR_THRESHOLD) {
            eyeClosedFrames++
        } else {
            eyeClosedFrames = 0
        }

        return eyeClosedFrames >= MAX_CLOSED_FRAMES
    }

    private fun computeEAR(landmarks: List<NormalizedLandmark>, indices: List<Int>): Double {
        fun dist(p1: NormalizedLandmark, p2: NormalizedLandmark): Double {
            val dx = p1.x() - p2.x()
            val dy = p1.y() - p2.y()
            return Math.sqrt((dx * dx + dy * dy).toDouble())
        }

        val A = dist(landmarks[indices[1]], landmarks[indices[5]])
        val B = dist(landmarks[indices[2]], landmarks[indices[4]])
        val C = dist(landmarks[indices[0]], landmarks[indices[3]])

        return (A + B) / (2.0 * C)
    }
}
