package com.example.animalsoundrecognition.main

import com.example.animalsoundrecognition.model.DataSound
import com.example.animalsoundrecognition.server.SoundService
import com.example.animalsoundrecognition.util.Resource
import javax.inject.Inject

class DefaultMainRepository @Inject constructor(val api:SoundService) :MainRepository {
    override suspend fun getSounds(): Resource<List<DataSound>> {
        return try {
            val response = api.getSounds()
            val result = response.body()
            if(response.isSuccessful && result != null) {
                Resource.Success(result)
            } else {
                Resource.Error(response.message())
            }
        } catch(e: Exception) {
            Resource.Error(e.message ?: "An error occured")
        }
    }

    //override suspend fun postSound(sound: DataSound): Resource<DataSound> {
    //    TODO("Not yet implemented")
    //}

}