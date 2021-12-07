package com.example.animalsoundrecognition.model

import com.jjoe64.graphview.series.DataPoint

class DataSound(private val title: String, private val type:String, private val durationMillis: Long,
                val pointsInGraphs: Long, private val numOfGraphs: Long, val timeDomainPoints:List<DataPoint>) {
    private val id: Int? = null
    val dataSoundParameters: DataSoundParameters? = null

    override fun toString(): String {
        return "Sound(id=$id, title=$title, type=$type, durationMilis=$durationMillis, pointsInGraphs=$pointsInGraphs, numsOfGraphs=$numOfGraphs" +
                ", parameteres=$dataSoundParameters" +
                ", timePoints=${timeDomainPoints.takeLast(10)})"
    }
}