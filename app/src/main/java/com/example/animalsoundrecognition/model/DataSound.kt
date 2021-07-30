package com.example.animalsoundrecognition.model

import com.jjoe64.graphview.series.DataPoint

class DataSound(title:String, durationMillis:Long, dataPoints:List<DataPoint>) {
    private val id: Int? = null
    private val title: String? = title
    private val durationMillis: Long? = durationMillis
    private val dataPoints: List<DataPoint> = dataPoints
    override fun toString(): String {
        return "Quiz(id=$id, title=$title, durationMilis=$durationMillis, dataPoints=${dataPoints.take(10)})"
    }
}