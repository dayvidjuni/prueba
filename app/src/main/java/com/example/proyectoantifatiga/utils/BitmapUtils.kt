package com.example.proyectoantifatiga.utils

import android.graphics.*
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object BitmapUtils {
    var latestBitmap: Bitmap? = null

    fun imageProxyToBitmap(proxy: ImageProxy): Bitmap {
        val y = proxy.planes[0].buffer
        val u = proxy.planes[1].buffer
        val v = proxy.planes[2].buffer
        val ySize = y.remaining()
        val uSize = u.remaining()
        val vSize = v.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        y.get(nv21, 0, ySize)
        v.get(nv21, ySize, vSize)
        u.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, proxy.width, proxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, proxy.width, proxy.height), 100, out)
        val jpegBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        // Rotar automáticamente según orientación
        val rotatedBitmap = rotateBitmap(bitmap, proxy.imageInfo.rotationDegrees)

        // Guardar el bitmap más reciente (para dibujar en FatigueDetector)
        latestBitmap = rotatedBitmap

        return rotatedBitmap
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
