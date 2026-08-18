package com.example.updater

import retrofit2.http.GET
import retrofit2.http.Url

interface UpdaterApi {
    @GET
    suspend fun getUpdateInfo(@Url url: String): UpdateInfo
}
