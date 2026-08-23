package com.example.melhome.network

import com.example.melhome.data.MelHomeContext
import retrofit2.http.GET
import retrofit2.http.Header

interface MelCloudService {
    @GET("context")
    suspend fun getContext(@Header("Cookie") cookie: String): MelHomeContext
}
