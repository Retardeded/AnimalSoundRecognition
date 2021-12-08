package com.example.animalsoundrecognition.server

import com.example.animalsoundrecognition.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.*


interface SoundService {

    @GET("api/sounds/soundsInfo")
    suspend fun getSounds(): Response<List<DataSound>>

    @GET("api/sounds/soundTypes")
    suspend fun getTypes(): Response<List<SoundType>>

    @GET("api/sounds/{id}")
    suspend fun getSound( @Path("id") id:String): Response<DataSound>

    @DELETE("api/sounds/{id}")
    suspend fun deleteSound( @Path("id") id:String): Response<Object>

    @Headers("Content-Type: application/json")
    @POST("api/sounds")
    suspend fun postSound(@Body sound: DataSound): Response<DataSound>

    @Headers("Content-Type: application/json")
    @POST("api/sounds/checkTime")
    suspend fun checkSoundTimeDomain(@Body sound: DataSound): Response<List<Pair<SoundType, SoundsTimeCoefficients>>>

    @Headers("Content-Type: application/json")
    @POST("api/sounds/checkPowerSpectrum")
    suspend fun checkSoundPowerSpectrum(@Body sound: DataSound): Response<List<Pair<SoundType, PowerSpectrumCoefficient>>>

    @Headers("Content-Type: application/json")
    @POST("api/sounds/checkFrequency")
    suspend fun checkSoundFrequencyDomain(@Body sound: DataSound): Response<List<Pair<SoundType, SoundsFreqCoefficients>>>


}