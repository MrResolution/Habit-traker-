package com.example.updater

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateInfo(
    @param:Json(name = "versionCode") val versionCode: Int,
    @param:Json(name = "versionName") val versionName: String,
    @param:Json(name = "releaseNotes") val releaseNotes: String,
    @param:Json(name = "apkUrl") val apkUrl: String,
    @param:Json(name = "isForceUpdate") val isForceUpdate: Boolean = false
)
