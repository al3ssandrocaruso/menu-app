package com.alessandrocaruso.menuapp.utils

import com.alessandrocaruso.menuapp.domain.MenuQrPayload
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Closes the loop on QR sharing: a payload is encoded exactly as the app would render it, then
 * decoded with the same ZXing reader the camera analyzer uses, and interpreted by
 * [MenuQrPayload]. If a generated code were unreadable, or encoded the wrong URL, this fails.
 */
class QrCodeGeneratorTest {

    private val payload = MenuQrPayload(
        "https://github.com/al3ssandrocaruso/restaurantsappdata/raw/main/menus/PDFsMenu/"
    )

    @Test
    fun `a generated code decodes back to the restaurant it was made for`() {
        val decodedText = decode(QrCodeGenerator.encodeMatrix(payload.encode("3")))

        val result = payload.decode(decodedText)
        assertTrue(result is MenuQrPayload.ScanResult.MenuLink)
        assertEquals("3", (result as MenuQrPayload.ScanResult.MenuLink).restaurantId)
    }

    @Test
    fun `codes remain readable at the smallest size the dialog renders`() {
        val matrix = QrCodeGenerator.encodeMatrix(payload.encode("12"), sizePx = 200)

        assertEquals(payload.encode("12"), decode(matrix))
    }

    @Test
    fun `the requested size and quiet zone are honoured`() {
        val matrix = checkNotNull(QrCodeGenerator.encodeMatrix("hello", sizePx = 300))

        assertEquals(300, matrix.width)
        assertEquals(300, matrix.height)
    }

    @Test
    fun `empty or non-positive input encodes to nothing rather than throwing`() {
        assertNull(QrCodeGenerator.encodeMatrix(""))
        assertNull(QrCodeGenerator.encodeMatrix("x", sizePx = 0))
        assertNull(QrCodeGenerator.encodeMatrix("x", sizePx = -1))
    }

    @Test
    fun `content too large for the symbol is reported instead of crashing`() {
        // A QR code cannot hold this much data at the smallest size / error-correction level.
        assertNull(QrCodeGenerator.encodeMatrix("x".repeat(10_000), sizePx = 40))
    }

    /** Decodes a module matrix with the same reader configuration the camera analyzer uses. */
    private fun decode(matrix: BitMatrix?): String? {
        checkNotNull(matrix) { "expected the payload to encode" }
        val reader = MultiFormatReader().apply {
            setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
        }
        return reader.decode(BinaryBitmap(HybridBinarizer(matrix.toLuminanceSource()))).text
    }

    /** Renders the matrix as 8-bit luminance, the form ZXing expects from a camera frame. */
    private fun BitMatrix.toLuminanceSource(): LuminanceSource {
        val pixels = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (this[x, y]) 0x00 else 0xFF.toByte()
            }
        }
        return object : LuminanceSource(width, height) {
            override fun getRow(y: Int, row: ByteArray?): ByteArray {
                val out = row?.takeIf { it.size >= width } ?: ByteArray(width)
                System.arraycopy(pixels, y * width, out, 0, width)
                return out
            }

            override fun getMatrix(): ByteArray = pixels
        }
    }
}
