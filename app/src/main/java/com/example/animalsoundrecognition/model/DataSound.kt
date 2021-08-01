package com.example.animalsoundrecognition.model

class DataSound(title:String, durationMillis:Long, dataGraphs:List<DataGraph>) {
    private val id: Int? = null
    private val title: String? = title
    private val durationMillis: Long? = durationMillis
    private val dataGraphs: List<DataGraph> = dataGraphs
    override fun toString(): String {
        return "Sound(id=$id, title=$title, durationMilis=$durationMillis, dataGraphs=${dataGraphs.take(2)})"
    }
}