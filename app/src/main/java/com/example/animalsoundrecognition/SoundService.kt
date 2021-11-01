package com.example.animalsoundrecognition

import com.example.animalsoundrecognition.model.DataSound
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.*


interface SoundService {

    @GET("api/sounds/soundInfo")
    suspend fun getSounds(): Response<List<DataSound>>

    @GET("api/sounds/{id}")
    suspend fun getSound( @Path("id") id:String): Response<DataSound>

    @Headers("Content-Type: application/json")
    @POST("api/sounds")
    suspend fun postSound(@Body sound: DataSound): Response<DataSound>

    @Headers("Content-Type: application/json")
    @POST("api/sounds/check")
    suspend fun checkSound(@Body sound: DataSound): Response<List<Pair<DataSound, Double>>>
}