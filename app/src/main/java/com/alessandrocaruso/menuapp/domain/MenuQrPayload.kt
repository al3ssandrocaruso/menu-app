package com.alessandrocaruso.menuapp.domain

/**
 * The contract for the QR codes this app produces and consumes.
 *
 * Payload format (unchanged from the original app, so previously shared codes still scan):
 *
 *     <pdfBaseUrl><restaurantId>.pdf
 *
 * Encoding and decoding live together in one pure object so the scanner screen holds no parsing
 * logic and the format can be tested without a camera.
 */
class MenuQrPayload(private val pdfBaseUrl: String) {

    /** Result of interpreting scanned QR text. */
    sealed interface ScanResult {
        /** A QR code this app issued, for [restaurantId], whose PDF lives at [pdfUrl]. */
        data class MenuLink(val restaurantId: String, val pdfUrl: String) : ScanResult
        /** Decoded successfully but the content is not one of our menu links. */
        data object NotAMenuLink : ScanResult
        /** Nothing usable was decoded (null or blank). */
        data object Empty : ScanResult
    }

    /** Builds the payload to encode in a shareable QR code for [restaurantId]. */
    fun encode(restaurantId: String): String {
        require(restaurantId.isNotBlank()) { "restaurantId must not be blank" }
        require(RESTAURANT_ID.matches(restaurantId)) { "restaurantId must be alphanumeric: $restaurantId" }
        return "$pdfBaseUrl$restaurantId.pdf"
    }

    /**
     * Interprets scanned text. Never throws: any malformed, foreign or empty payload is reported
     * as [ScanResult.NotAMenuLink] / [ScanResult.Empty] so the UI can show a clear message
     * instead of the app acting on arbitrary scanned content.
     */
    fun decode(text: String?): ScanResult {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isEmpty()) return ScanResult.Empty
        if (!trimmed.startsWith(pdfBaseUrl)) return ScanResult.NotAMenuLink

        val remainder = trimmed.removePrefix(pdfBaseUrl)
        if (!remainder.endsWith(PDF_SUFFIX)) return ScanResult.NotAMenuLink

        val id = remainder.removeSuffix(PDF_SUFFIX)
        if (!RESTAURANT_ID.matches(id)) return ScanResult.NotAMenuLink

        return ScanResult.MenuLink(restaurantId = id, pdfUrl = trimmed)
    }

    private companion object {
        const val PDF_SUFFIX = ".pdf"

        /**
         * Restaurant ids in the feed are short numeric strings. Constraining the id keeps a
         * scanned code from injecting path segments or query strings into the download URL.
         */
        val RESTAURANT_ID = Regex("[A-Za-z0-9_-]{1,32}")
    }
}
