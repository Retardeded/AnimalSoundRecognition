package com.example.animalsoundrecognition

import com.example.animalsoundrecognition.model.DataSound
import com.example.animalsoundrecognition.model.Quiz
import retrofit2.Call
import retrofit2.http.*


interface SoundService {

    @GET("api/sounds/soundInfo")
    fun getSounds(): Call<List<DataSound>>

    @GET("api/sounds/{id}")
    fun getSound( @Path("id") id:String): Call<DataSound>


    @Headers("Content-Type: application/json")
    @POST("api/sounds")
    fun postSound(@Body sound: DataSound): Call<DataSound>

    @Headers("Content-Type: application/json")
    @POST("api/sounds/check")
    fun checkSound(@Body sound: DataSound): Call<List<Pair<DataSound, Double>>>
}