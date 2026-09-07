package com.alessandrocaruso.menuapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders QR bitmaps with ZXing — the library the app already uses to *decode* them.
 *
 * This replaces the `com.github.SumiMakito:AwesomeQRCode` dependency, which was unmaintained
 * since 2019 and resolved only through JitPack. That artifact is no longer resolvable from any
 * repository, so removing it is what makes the project build at all; it also removes CI's
 * dependency on a third-party artifact host.
 *
 * [render] must be called off the main thread: a 600px code allocates and fills a ~1.4 MB bitmap.
 */
object QrCodeGenerator {

    /**
     * Encodes [content] to a QR module matrix.
     *
     * Split out from [render] because it touches no Android types, which lets unit tests encode a
     * payload and decode it again to prove the generated codes are actually readable.
     *
     * @return the matrix, or null if [content] cannot be encoded at this size.
     */
    fun encodeMatrix(
        content: String,
        sizePx: Int = DEFAULT_SIZE_PX,
        marginModules: Int = DEFAULT_MARGIN,
    ): BitMatrix? {
        if (content.isEmpty() || sizePx <= 0) return null

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to marginModules,
            EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
        )
        return try {
            QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        } catch (e: WriterException) {
            // Content too long for the chosen size and error-correction level.
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** Renders [content] as a square bitmap ready to display, or null if it cannot be encoded. */
    fun render(
        content: String,
        sizePx: Int = DEFAULT_SIZE_PX,
        @ColorInt darkColor: Int = DEFAULT_DARK,
        @ColorInt lightColor: Int = Color.WHITE,
        marginModules: Int = DEFAULT_MARGIN,
    ): Bitmap? {
        val matrix = encodeMatrix(content, sizePx, marginModules) ?: return null

        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) darkColor else lightColor
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private const val DEFAULT_SIZE_PX = 600
    private const val DEFAULT_MARGIN = 2

    /** The app's brand green, matching the original generated codes. */
    private const val DEFAULT_DARK = 0xFF2E7855.toInt()
}
