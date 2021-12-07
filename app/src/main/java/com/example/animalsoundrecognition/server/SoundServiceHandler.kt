package com.example.animalsoundrecognition.server

import android.widget.TextView
import com.example.animalsoundrecognition.model.DataGraph
import com.example.animalsoundrecognition.model.DataGraphs
import com.example.animalsoundrecognition.model.DataSound
import com.jjoe64.graphview.series.DataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SoundServiceHandler {
    lateinit var service: SoundService
    //var okHttpClient: OkHttpClient? = null
    // tutaj ustaw swoje lokalne ip
    val ipString = "http://192.168.1.3:8080"
    //.baseUrl("http://10.0.0.5:8080/")
    //.baseUrl("http://192.168.1.3:8080/")
    init {
        createClient()
    }
    fun createClient() {
        val retrofit = Retrofit.Builder()
            .baseUrl(ipString)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(SoundService::class.java)
    }

    suspend fun getSounds(textTest: TextView) {
        val response = service.getSounds()
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val data = response.body()!!
                val stringBuilder = data.toString();
                textTest.text = stringBuilder
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }

    suspend fun getTypes(textTest: TextView) {
        val response = service.getTypes()
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val data = response.body()!!
                val stringBuilder = data.toString();
                textTest.text = stringBuilder
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }

    suspend fun getSound(textTest: TextView, animalNameText: TextView, dataGraphs: DataGraphs) {
        val id = animalNameText.text.toString()
        val response = service.getSound(id)
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val soundData = response.body()!!
                val stringBuilder = soundData.toString();
                textTest.text = stringBuilder
                dataGraphs.currentRecordTimeDomain = loadDataSound(soundData.pointsInGraphs, soundData.timeDomainPoints, false)
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }

        }

    }

    private fun loadDataSound(pointsInGraphs:Long,soundData:List<DataPoint>, isFreqDomain:Boolean): MutableList<DataGraph> {
        val dataGraphs: MutableList<DataGraph> = mutableListOf()
        var pointsInGraphs = pointsInGraphs
        var numberOfGraphs = (soundData.size / pointsInGraphs)
        if(isFreqDomain) {
            numberOfGraphs = 1
            pointsInGraphs = soundData.size.toLong()-1
        }

        println(numberOfGraphs)
        println(pointsInGraphs)
        println(soundData.size)

        for (i in 0..numberOfGraphs-1) {
            val graph = DataGraph(
                soundData.subList(
                    ((i * pointsInGraphs).toInt()),
                    ((i + 1) * pointsInGraphs).toInt()
                )
            )
            dataGraphs.add(graph)
        }
        return dataGraphs
    }

    suspend fun deleteSound(textTest: TextView, animalNameText: TextView) {
        val id = animalNameText.text.toString()
        val response = service.deleteSound(id)
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }

    suspend fun postSound(textTest: TextView, dataSound:DataSound) {
        val sound = dataSound
        val response = service.postSound(sound)
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val dataSound = response.body()!!
                val stringBuilder = dataSound.toString();
                textTest.text = stringBuilder
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }

    suspend fun checkSound(textTest: TextView, dataSound:DataSound) {
        val sound = dataSound
        val response = service.checkSound(sound)
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val mostSimilarSoundTypesList = response.body()!!
                var text:String = "d\n"
                for (soundType in mostSimilarSoundTypesList) {
                    text += "Sound Type info:\n" + soundType.first + "\n"
                    text += "Correlation info:\n" + soundType.second + "\n"
                }
                textTest.text = text
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }

    suspend fun checkSoundFreqDomain(textTest: TextView, dataSound:DataSound) {
        val sound = dataSound
        val response = service.checkSoundFreqDomain(sound)
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val mostSimilarSoundsList = response.body()!!
                var text:String = "d\n"
                for (sound in mostSimilarSoundsList) {
                    text += "Sound info:\n" + sound.first + "\n"
                    text += "Correlation info:\n" + sound.second + "\n"
                }
                textTest.text = text
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }
}