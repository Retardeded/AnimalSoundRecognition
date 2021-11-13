package com.example.animalsoundrecognition.soundprocessing

import android.media.AudioRecord
import ca.uol.aig.fftpack.RealDoubleFFT
import com.example.animalsoundrecognition.MainActivity.Companion.isPlaying
import com.example.animalsoundrecognition.MainActivity.Companion.mAudioRecord
import com.example.animalsoundrecognition.MainActivity.Companion.mMinBufferSize
import com.example.animalsoundrecognition.R
import com.example.animalsoundrecognition.model.DataGraph
import com.example.animalsoundrecognition.model.DataGraphs
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.BaseSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class GraphHandler(graph: GraphView, graphTime: GraphView, graphFreqFull: GraphView) {
    var dataGraphs: DataGraphs = DataGraphs()
    val graph = graph
    val graphTime = graphTime
    val graphFreqFull = graphFreqFull
    var mFullFreqSeries: BaseSeries<DataPoint>? = null
    var mFreqSeries: BaseSeries<DataPoint>? = null
    var mTimeSeries: BaseSeries<DataPoint>? = null
    private var pointsInGraphs: Long = 0
    private var numOfGraphs: Long = 0
    var transformer: RealDoubleFFT? = null

    init {
    }

    fun initGraphView() {
        val graphSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())
        mFreqSeries = graphSeries
        graph.title = "Frequency Domain"

        graphTime.title = "Time Domain"
        mTimeSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())

        graphFreqFull.title = "Full Signal Frequency Domain"
        mFullFreqSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())

        if (graph.series.count() > 0) {
            graph.removeAllSeries()
        }
        graph.addSeries(mFreqSeries)

        if (graphTime.series.count() > 0) {
            graphTime.removeAllSeries()
        }
        graphTime.addSeries(mTimeSeries)

        if (graphFreqFull.series.count() > 0) {
            graphFreqFull.removeAllSeries()
        }
        graphFreqFull.addSeries(mFullFreqSeries)
    }

}