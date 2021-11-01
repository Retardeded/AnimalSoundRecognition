package com.example.animalsoundrecognition.model

import com.jjoe64.graphview.series.DataPoint

class DataSound(title:String, durationMillis:Long, dataPoints:List<DataPoint>) {
    private val id: Int? = null
    private val title: String? = title
    private val durationMillis: Long? = durationMillis
    val dataPoints: List<DataPoint> = dataPoints
    override fun toString(): String {
        return "Sound(id=$id, title=$title, durationMilis=$durationMillis), data=${dataPoints.take(2)}\n"
    }
}