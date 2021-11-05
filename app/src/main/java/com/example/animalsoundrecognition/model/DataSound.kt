package com.example.animalsoundrecognition.model

import com.jjoe64.graphview.series.DataPoint

class DataSound(title:String, durationMillis:Long, pointsInGraph: Long, freqDomainPoints:List<DataPoint>, timeDomainPoints:List<DataPoint>) {
    private val id = null
    private val title: String = title
    private val durationMillis: Long = durationMillis
    private val pointsInGraph: Long = pointsInGraph
    val freqDomainPoints: List<DataPoint> = freqDomainPoints
    val timeDomainPoints: List<DataPoint> = timeDomainPoints
    override fun toString(): String {
        return "Sound(id=$id, title=$title, durationMilis=$durationMillis, pointsInGraph=$pointsInGraph" +
                ", freqPoints=${freqDomainPoints.take(3)}" + ", timePoints=${timeDomainPoints.take(3)}))"
    }
}