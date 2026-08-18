package com.example.updater

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progressPercentage: Int) : DownloadState()
    data class Downloaded(val file: File) : DownloadState()
    data class Failed(val reason: String) : DownloadState()
}

class ApkDownloader(private val context: Context) {

    fun downloadApk(apkUrl: String, fileName: String = "update.apk"): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null && !downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val apkFile = File(downloadsDir, fileName)
        if (apkFile.exists()) {
            apkFile.delete()
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("Downloading App Update")
            setDescription("Fetching latest APK version...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(apkFile))
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadId = downloadManager.enqueue(request)
        var downloading = true

        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                val bytesDownloaded = if (bytesDownloadedIndex != -1) cursor.getInt(bytesDownloadedIndex) else 0
                val bytesTotal = if (bytesTotalIndex != -1) cursor.getInt(bytesTotalIndex) else 0
                val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1
                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val reasonCode = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1

                cursor.close()

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        downloading = false
                        emit(DownloadState.Downloaded(apkFile))
                    }
                    DownloadManager.STATUS_FAILED -> {
                        downloading = false
                        val errorMessage = when (reasonCode) {
                            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "HTTP error returned by server."
                            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download."
                            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No external storage device found."
                            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists."
                            DownloadManager.ERROR_FILE_ERROR -> "Storage file error."
                            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space."
                            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many HTTP redirects."
                            DownloadManager.ERROR_UNKNOWN -> "Unknown download error."
                            else -> if (reasonCode in 100..599) "HTTP Error $reasonCode (e.g. 404 Not Found)" else "Error code: $reasonCode"
                        }
                        emit(DownloadState.Failed("Download failed: $errorMessage"))
                    }
                    DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                        if (bytesTotal > 0) {
                            val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                            emit(DownloadState.Downloading(progress))
                        }
                    }
                }
            } else {
                cursor?.close()
            }
            delay(500)
        }
    }
}
