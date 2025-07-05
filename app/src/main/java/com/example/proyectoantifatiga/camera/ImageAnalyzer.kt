package com.example.proyectoantifatiga.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.proyectoantifatiga.detector.BitmapUtils
import com.example.proyectoantifatiga.detector.FatigueDetector
import com.example.proyectoantifatiga.detector.HeadDownDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImageAnalyzer(
    private val fatigueDetector: FatigueDetector,
    private val headDownDetector: HeadDownDetector
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val bitmap = BitmapUtils.imageProxyToBitmap(image)
                fatigueDetector.detectAsync(bitmap)
                headDownDetector.detectAsync(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image.close()
            }
        }
    }
}
