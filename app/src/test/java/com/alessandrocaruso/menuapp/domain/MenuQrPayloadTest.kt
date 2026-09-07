package com.alessandrocaruso.menuapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QR payload contract. Covers the round trip and, more importantly, the inputs a camera will
 * genuinely encounter: other people's QR codes, blank decodes and lookalike URLs.
 */
class MenuQrPayloadTest {

    private val baseUrl = "https://github.com/al3ssandrocaruso/restaurantsappdata/raw/main/menus/PDFsMenu/"
    private val payload = MenuQrPayload(baseUrl)

    @Test
    fun `encodes the documented URL shape`() {
        assertEquals("${baseUrl}3.pdf", payload.encode("3"))
    }

    @Test
    fun `encode then decode round-trips the restaurant id`() {
        val result = payload.decode(payload.encode("12"))

        assertTrue(result is MenuQrPayload.ScanResult.MenuLink)
        assertEquals("12", (result as MenuQrPayload.ScanResult.MenuLink).restaurantId)
        assertEquals("${baseUrl}12.pdf", result.pdfUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `encoding a blank id is rejected`() {
        payload.encode("  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `encoding an id with path separators is rejected`() {
        payload.encode("../../secrets")
    }

    @Test
    fun `surrounding whitespace is tolerated`() {
        val result = payload.decode("  ${baseUrl}5.pdf \n")

        assertTrue(result is MenuQrPayload.ScanResult.MenuLink)
        assertEquals("5", (result as MenuQrPayload.ScanResult.MenuLink).restaurantId)
    }

    @Test
    fun `null and blank scans report Empty`() {
        assertEquals(MenuQrPayload.ScanResult.Empty, payload.decode(null))
        assertEquals(MenuQrPayload.ScanResult.Empty, payload.decode(""))
        assertEquals(MenuQrPayload.ScanResult.Empty, payload.decode("   "))
    }

    @Test
    fun `an unrelated QR code is reported, not acted on`() {
        assertEquals(
            MenuQrPayload.ScanResult.NotAMenuLink,
            payload.decode("https://example.com/whatever"),
        )
        assertEquals(
            MenuQrPayload.ScanResult.NotAMenuLink,
            payload.decode("WIFI:S=cafe;T=WPA;P=hunter2;;"),
        )
    }

    @Test
    fun `a menu URL without the pdf extension is not accepted`() {
        assertEquals(MenuQrPayload.ScanResult.NotAMenuLink, payload.decode("${baseUrl}3"))
    }

    @Test
    fun `an id containing path traversal is rejected on decode`() {
        // The scanned id is interpolated into a download URL, so it must not carry path segments.
        assertEquals(
            MenuQrPayload.ScanResult.NotAMenuLink,
            payload.decode("$baseUrl../../../etc/passwd.pdf"),
        )
    }

    @Test
    fun `an id carrying a query string is rejected`() {
        assertEquals(
            MenuQrPayload.ScanResult.NotAMenuLink,
            payload.decode("${baseUrl}3?redirect=evil.com.pdf"),
        )
    }

    @Test
    fun `an empty id is rejected`() {
        assertEquals(MenuQrPayload.ScanResult.NotAMenuLink, payload.decode("$baseUrl.pdf"))
    }

    @Test
    fun `a host that merely resembles ours is rejected`() {
        assertEquals(
            MenuQrPayload.ScanResult.NotAMenuLink,
            payload.decode("https://evil.example/github.com/al3ssandrocaruso/…/PDFsMenu/1.pdf"),
        )
    }
}
