package com.alessandrocaruso.menuapp.utils

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * Decodes QR codes from CameraX frames.
 *
 * The analyzer only reports raw decoded text; deciding what that text *means* is
 * [com.alessandrocaruso.menuapp.domain.MenuQrPayload]'s job. Keeping the split means the scanning
 * pipeline has no knowledge of restaurants or URLs, and the payload rules are testable without a
 * camera.
 */
class QRCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    /**
     * Reused across frames: [MultiFormatReader] allocates its hint table and decoder state on
     * construction, and the analyzer is called ~30 times a second.
     */
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    override fun analyze(image: ImageProxy) {
        // `use` guarantees the frame is closed on every path. The previous version only closed
        // it inside the supported-format branch, so a single unsupported frame stalled the
        // camera permanently: CameraX will not deliver another image until this one is released.
        image.use { frame ->
            if (frame.format !in SUPPORTED_IMAGE_FORMATS) return@use

            val text = try {
                val plane = frame.planes.first()
                val source = PlanarYUVLuminanceSource(
                    plane.buffer.toByteArray(),
                    // Rows are padded to `rowStride`; using `width` here would shear the image
                    // on devices whose stride exceeds the frame width and decode nothing.
                    plane.rowStride,
                    frame.height,
                    0,
                    0,
                    frame.width,
                    frame.height,
                    false,
                )
                reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))?.text
            } catch (e: NotFoundException) {
                // No code in this frame: the overwhelmingly common case, not an error.
                null
            } catch (e: RuntimeException) {
                // Malformed/partial symbols make ZXing throw IllegalArgument or index errors.
                null
            } finally {
                reader.reset()
            }

            if (!text.isNullOrBlank()) onQrCodeScanned(text)
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        return ByteArray(remaining()).also { get(it) }
    }

    private companion object {
        val SUPPORTED_IMAGE_FORMATS = listOf(
            ImageFormat.YUV_420_888,
            ImageFormat.YUV_422_888,
            ImageFormat.YUV_444_888,
        )
    }
}
