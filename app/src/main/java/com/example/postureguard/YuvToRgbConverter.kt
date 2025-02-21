package com.example.postureguard

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
class YuvToRgbConverter {

    @Synchronized
    fun yuvToRgb(image: Image, output: Bitmap) {
        if (image.format != ImageFormat.YUV_420_888) {
            throw IllegalArgumentException("Image format must be YUV_420_888")
        }

        val yuvPlanes = image.planes
        val yPlane = yuvPlanes[0]
        val uPlane = yuvPlanes[1]
        val vPlane = yuvPlanes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val width = image.width
        val height = image.height

        val yStride = yPlane.rowStride
        val uvStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val rgbArray = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val yIndex = y * yStride + x

                // Correctly calculate the position of the U and V components.
                val uvIndex = (y shr 1) * uvStride + (x shr 1) * uvPixelStride

                val yValue = yBuffer.get(yIndex).toInt() and 0xFF
                val uValue = uBuffer.get(uvIndex).toInt() and 0xFF
                val vValue = vBuffer.get(uvIndex).toInt() and 0xFF

                val c = yValue - 16
                val d = uValue - 128
                val e = vValue - 128

                val r = (1.164 * c + 1.596 * e).toInt().coerceIn(0, 255)
                val g = (1.164 * c - 0.813 * e - 0.391 * d).toInt().coerceIn(0, 255)
                val b = (1.164 * c + 2.018 * d).toInt().coerceIn(0, 255)

                rgbArray[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        output.setPixels(rgbArray, 0, width, 0, 0, width, height)
    }
}
