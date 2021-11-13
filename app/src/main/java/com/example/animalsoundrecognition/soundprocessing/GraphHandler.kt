package com.example.animalsoundrecognition.soundprocessing

import android.media.AudioRecord
import ca.uol.aig.fftpack.RealDoubleFFT
import com.example.animalsoundrecognition.MainActivity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GraphHandler(graph: GraphView, graphTime: GraphView, graphFreqFull: GraphView) {
    var dataGraphs: DataGraphs = DataGraphs()
    val graph = graph
    val graphTime = graphTime
    val graphFreqFull = graphFreqFull
    var mFullFreqSeries: BaseSeries<DataPoint>? = null
    var mFreqSeries: BaseSeries<DataPoint>? = null
    var mTimeSeries: BaseSeries<DataPoint>? = null
    var pointsInGraphs: Long = 0
    var numOfGraphs: Long = 0
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

    fun updateGraphView() {
        val audioData = ShortArray(mMinBufferSize)
        var index = 0
        while (MainActivity.isRecording) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                val num = audioData.size
                //os?.write(audioData, 0, mMinBufferSize);
                val data = arrayOfNulls<DataPoint>(num)
                val dataTime = arrayOfNulls<DataPoint>(num)
                // apply Fast Fourier Transform here
                transformer = RealDoubleFFT(num)
                val toTransform = DoubleArray(num)
                for (i in 0 until num) {
                    //toTransform[i] = audioData[i].toDouble() / Short.MAX_VALUE
                    toTransform[i] = audioData[i].toDouble()
                }
                transformer!!.ft(toTransform)
                for (i in 0 until num) {
                    data[i] = DataPoint(i.toDouble(), toTransform[i])
                }

                for (i in 0 until num) {
                    dataTime[i] = DataPoint(i.toDouble(), audioData[i].toDouble())
                }

                val listTime: List<DataPoint> = dataTime.toList().filterNotNull()
                index++
                dataGraphs.currentRecordTimeDomain.add(DataGraph(listTime))
                GlobalScope.launch( Dispatchers.Main ){
                    mFreqSeries!!.resetData(data)
                    mTimeSeries!!.resetData(dataTime)
                }
            }

        }
        pointsInGraphs = audioData.size.toLong()
        numOfGraphs = index.toLong()
        println("index::" + index)
    }

    fun replayGraphView(): Boolean {
        var index = 0
        val audioData = ShortArray(mMinBufferSize)

        while (isPlaying && index < dataGraphs.currentRecordTimeDomain.size) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            val numTime =  dataGraphs.currentRecordTimeDomain[index].dataPoints.size
            val dataTime = arrayOfNulls<DataPoint>(numTime)
            val data = arrayOfNulls<DataPoint>(numTime)
            for (i in 0 until numTime) {
                dataTime[i] = dataGraphs.currentRecordTimeDomain[index].dataPoints[i]
            }
            transformer = RealDoubleFFT(numTime)
            val toTransform = DoubleArray(numTime)
            for (i in 0 until numTime) {
                //toTransform[i] = audioData[i].toDouble() / Short.MAX_VALUE
                toTransform[i] = dataTime[i]!!.y
            }
            transformer!!.ft(toTransform)
            for (i in 0 until numTime) {
                data[i] = DataPoint(i.toDouble(), toTransform[i])
            }

            GlobalScope.launch( Dispatchers.Main ){
                mTimeSeries!!.resetData(dataTime)
                mFreqSeries!!.resetData(data)
            }

            index++
        }

        if(dataGraphs.currentRecordFullFreqDomain.size > 0)
        {
            val num = dataGraphs.currentRecordFullFreqDomain[0].dataPoints.size
            val data = arrayOfNulls<DataPoint>(num)
            for (i in 0 until num) {
                data[i] = dataGraphs.currentRecordFullFreqDomain[0].dataPoints[i]
            }
            GlobalScope.launch( Dispatchers.Main ){
                mFullFreqSeries!!.resetData(data)
            }
        }
        return true
    }

}