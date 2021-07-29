package com.example.animalsoundrecognition.model

import com.jjoe64.graphview.series.DataPoint

class DataSound(title:String, dataPoints:List<DataPoint>) {
    private val id: Int? = null
    private val title: String? = title
    private val dataPoints: List<DataPoint> = dataPoints
    override fun toString(): String {
        return "Quiz(id=$id, title=$title, dataPoints=$dataPoints)"
    }
}