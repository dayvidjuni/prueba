package com.example.proyectoantifatiga.utils

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot

object EyeDrawer {

    private val leftEyeIndices = listOf(33, 160, 158, 133, 153, 144)
    private val rightEyeIndices = listOf(362, 385, 387, 263, 373, 380)

    fun drawEyesAndEAR(mat: Mat, result: FaceLandmarkerResult) {
        if (result.faceLandmarks().isEmpty()) return

        val landmarks = result.faceLandmarks()[0]

        val leftEAR = drawEyeAndCalculateEAR(mat, landmarks, leftEyeIndices)
        val rightEAR = drawEyeAndCalculateEAR(mat, landmarks, rightEyeIndices)

        val avgEAR = (leftEAR + rightEAR) / 2.0
        val earText = "EAR: %.2f".format(avgEAR)

        Imgproc.putText(
            mat,
            earText,
            Point(10.0, 30.0),
            Imgproc.FONT_HERSHEY_SIMPLEX,
            1.0,
            Scalar(0.0, 255.0, 0.0),
            2
        )
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

        // Dibuja contorno del ojo
        for (i in points.indices) {
            val next = (i + 1) % points.size
            Imgproc.line(mat, points[i], points[next], Scalar(255.0, 0.0, 0.0), 2)
        }

        // EAR: Eye Aspect Ratio
        val A = distance(points[1], points[5])
        val B = distance(points[2], points[4])
        val C = distance(points[0], points[3])
        return (A + B) / (2.0 * C)
    }

    private fun distance(p1: Point, p2: Point): Double {
        return hypot(p2.x - p1.x, p2.y - p1.y)
    }
}
