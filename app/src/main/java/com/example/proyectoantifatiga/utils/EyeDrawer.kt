package com.example.proyectoantifatiga.utils

import android.graphics.Bitmap
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot

object EyeDrawer {

    private val leftEyeIndices = listOf(33, 160, 158, 133, 153, 144)
    private val rightEyeIndices = listOf(362, 385, 387, 263, 373, 380)

    /**
     * Dibuja los ojos y muestra el EAR en pantalla (usado en modo debug visual)
     */
    fun drawEyesAndEAR(mat: Mat, result: FaceLandmarkerResult): Bitmap {
        if (result.faceLandmarks().isEmpty()) return Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        val landmarks = result.faceLandmarks()[0]

        val leftEAR = drawEyeAndCalculateEAR(mat, landmarks, leftEyeIndices)
        val rightEAR = drawEyeAndCalculateEAR(mat, landmarks, rightEyeIndices)
        val avgEAR = (leftEAR + rightEAR) / 2.0

        Imgproc.putText(
            mat,
            "EAR: %.2f".format(avgEAR),
            Point(10.0, 30.0),
            Imgproc.FONT_HERSHEY_SIMPLEX,
            1.0,
            Scalar(0.0, 255.0, 0.0),
            2
        )

        val outputBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, outputBitmap)
        return outputBitmap
    }

    /**
     * Calcula el EAR promedio usando solo los landmarks (sin dibujar)
     */
    fun computeEARFromLandmarks(
        imageWidth: Int,
        imageHeight: Int,
        landmarks: List<NormalizedLandmark>
    ): Double {
        val leftEAR = computeEAR(landmarks, leftEyeIndices, imageWidth, imageHeight)
        val rightEAR = computeEAR(landmarks, rightEyeIndices, imageWidth, imageHeight)
        return (leftEAR + rightEAR) / 2.0
    }

    private fun drawEyeAndCalculateEAR(
        mat: Mat,
        landmarks: List<NormalizedLandmark>,
        indices: List<Int>
    ): Double {
        val points = indices.map { index ->
            Point(
                (landmarks[index].x() * mat.width()).toDouble(),
                (landmarks[index].y() * mat.height()).toDouble()
            )
        }

        for (i in points.indices) {
            val next = (i + 1) % points.size
            Imgproc.line(mat, points[i], points[next], Scalar(255.0, 0.0, 0.0), 2)
        }

        val A = distance(points[1], points[5])
        val B = distance(points[2], points[4])
        val C = distance(points[0], points[3])
        return (A + B) / (2.0 * C)
    }

    private fun computeEAR(
        landmarks: List<NormalizedLandmark>,
        indices: List<Int>,
        imageWidth: Int,
        imageHeight: Int
    ): Double {
        val points = indices.map { index ->
            Point(
                (landmarks[index].x() * imageWidth).toDouble(),
                (landmarks[index].y() * imageHeight).toDouble()
            )
        }
        val A = distance(points[1], points[5])
        val B = distance(points[2], points[4])
        val C = distance(points[0], points[3])
        return (A + B) / (2.0 * C)
    }

    private fun distance(p1: Point, p2: Point): Double {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }
}
