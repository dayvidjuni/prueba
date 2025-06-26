package com.example.proyectoantifatiga.detector

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

object EyeDrawer {
    fun drawEyes(bitmap: Bitmap, landmarks: List<NormalizedLandmark>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.GREEN
            strokeWidth = 6f
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val puntosOjoIzquierdo = listOf(33, 133, 160, 158, 159, 157, 173)
        val puntosOjoDerecho = listOf(362, 263, 387, 385, 386, 384, 398)

        val ancho = bitmap.width.toFloat()
        val alto = bitmap.height.toFloat()

        val puntos = puntosOjoIzquierdo + puntosOjoDerecho
        for (i in puntos) {
            if (i < landmarks.size) {
                val landmark = landmarks[i]
                val x = landmark.x() * ancho
                val y = landmark.y() * alto
                canvas.drawCircle(x, y, 6f, paint)
            }
        }

        return mutableBitmap
    }
}
