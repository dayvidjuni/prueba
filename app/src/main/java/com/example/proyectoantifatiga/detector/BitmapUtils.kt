package com.example.proyectoantifatiga.detector

import android.graphics.*
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.io.ByteArrayOutputStream

object BitmapUtils {

    var latestBitmap: Bitmap? = null

    /**
     * Convierte un ImageProxy (YUV) en un Bitmap correctamente rotado.
     */
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

        // Aplicar rotación según orientación de la cámara
        val rotated = rotateBitmap(bitmap, proxy.imageInfo.rotationDegrees)
        latestBitmap = rotated

        return rotated
    }

    /**
     * Rota un Bitmap los grados indicados.
     */
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Convierte un Bitmap en un objeto Mat de OpenCV.
     */
    fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }
}
