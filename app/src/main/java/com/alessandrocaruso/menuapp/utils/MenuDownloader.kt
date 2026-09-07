package com.alessandrocaruso.menuapp.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.getSystemService

/**
 * Enqueues a scanned menu PDF with the system [DownloadManager].
 *
 * `DownloadManager` writes to the public Downloads collection on the app's behalf, so no storage
 * permission is required on the API levels this app supports (minSdk 28 uses the legacy path with
 * `WRITE_EXTERNAL_STORAGE` implied for DownloadManager destinations; 29+ uses scoped storage).
 * The previous manifest additionally requested `MANAGE_EXTERNAL_STORAGE` — an all-files
 * permission Google Play restricts — which was never requested at runtime nor needed.
 */
object MenuDownloader {

    private const val TAG = "MenuDownloader"

    /** Result of asking the system to download a menu. */
    sealed interface Result {
        data class Enqueued(val downloadId: Long) : Result
        data class Failed(val reason: String) : Result
    }

    /**
     * @param fileName name for the saved file, without extension.
     * @return whether the download was accepted by the system service. Failures are returned
     *   rather than swallowed, so the UI can tell the user instead of appearing to do nothing —
     *   the original helper caught every exception into an empty block.
     */
    fun enqueue(context: Context, url: String, fileName: String): Result {
        val downloadManager = context.getSystemService<DownloadManager>()
            ?: return Result.Failed("DownloadManager unavailable")

        return try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI
                )
                .setMimeType(PDF_MIME_TYPE)
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setTitle("$fileName.pdf")
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "$fileName.pdf",
                )

            Result.Enqueued(downloadManager.enqueue(request))
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Rejected download URL: $url", e)
            Result.Failed("Invalid download URL")
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to download to public Downloads", e)
            Result.Failed("Storage not available")
        }
    }

    private const val PDF_MIME_TYPE = "application/pdf"
}
