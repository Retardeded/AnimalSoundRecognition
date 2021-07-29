package com.example.animalsoundrecognition

import com.example.animalsoundrecognition.model.DataSound
import com.example.animalsoundrecognition.model.Quiz
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST


interface SoundService {
    @GET("api/quizzes")
    fun getQuizzes(): Call<List<Quiz>>

    //@POST("api/quizzes")
    //fun postQuiz(): Call<Quiz>

    @Headers("Content-Type: application/json")
    @POST("api/quizzes")
    fun postQuiz(@Body quiz: Quiz): Call<Quiz>

    @Headers("Content-Type: application/json")
    @POST("api/sounds")
    fun postSound(@Body sound: DataSound): Call<DataSound>

    //@POST("api/quizzes")
    //fun postQuiz(quiz: Quiz): Call<Quiz>
}