package com.example.updater

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloader = remember { ApkDownloader(context) }

    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var needsPermission by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isForceUpdate && downloadState !is DownloadState.Downloading) {
                onDismiss()
            }
        },
        title = {
            Text(text = "New Update Available (${updateInfo.versionName})")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = updateInfo.releaseNotes)
                Spacer(modifier = Modifier.height(16.dp))

                when (val state = downloadState) {
                    is DownloadState.Downloading -> {
                        Text(text = "Downloading update: ${state.progressPercentage}%")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.progressPercentage / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    is DownloadState.Failed -> {
                        Text(
                            text = "Error: ${state.reason}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is DownloadState.Downloaded -> {
                        Text(text = "Download complete! Tap Install to update.")
                    }
                    DownloadState.Idle -> {
                        if (needsPermission) {
                            Text(
                                text = "Permission required to install apps from outside Play Store. Tap 'Grant Permission' to proceed.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is DownloadState.Idle -> {
                    Button(
                        onClick = {
                            if (!ApkInstaller.canInstallApk(context)) {
                                needsPermission = true
                                ApkInstaller.openUnknownSourcesSettings(context)
                            } else {
                                needsPermission = false
                                coroutineScope.launch {
                                    downloader.downloadApk(updateInfo.apkUrl).collect { state ->
                                        downloadState = state
                                        if (state is DownloadState.Downloaded) {
                                            downloadedFile = state.file
                                            ApkInstaller.installApk(context, state.file)
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text(text = if (needsPermission) "Grant Permission" else "Update Now")
                    }
                }
                is DownloadState.Downloaded -> {
                    Button(
                        onClick = {
                            downloadedFile?.let { file ->
                                ApkInstaller.installApk(context, file)
                            }
                        }
                    ) {
                        Text(text = "Install Now")
                    }
                }
                is DownloadState.Failed -> {
                    Button(
                        onClick = {
                            downloadState = DownloadState.Idle
                        }
                    ) {
                        Text(text = "Retry")
                    }
                }
                is DownloadState.Downloading -> {
                    // Disable actions while downloading
                }
            }
        },
        dismissButton = {
            if (!updateInfo.isForceUpdate && downloadState !is DownloadState.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Later")
                }
            }
        }
    )
}
