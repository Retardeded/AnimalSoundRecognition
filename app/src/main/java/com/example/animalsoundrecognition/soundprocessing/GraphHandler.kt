package com.example.animalsoundrecognition.soundprocessing

import android.media.AudioRecord
import ca.uol.aig.fftpack.RealDoubleFFT
import com.example.animalsoundrecognition.model.DataGraph
import com.example.animalsoundrecognition.model.DataGraphs
import com.example.animalsoundrecognition.soundprocessing.RecordHandler.Companion.isPlaying
import com.example.animalsoundrecognition.soundprocessing.RecordHandler.Companion.isRecording
import com.example.animalsoundrecognition.soundprocessing.RecordHandler.Companion.mMinBufferSize
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.BaseSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan

class GraphHandler(val graphAmplitude: GraphView, val graphPhase:GraphView, val graphTime: GraphView, val graphFreqFull: GraphView) {
    var dataGraphs: DataGraphs = DataGraphs()
    var mFullFreqSeries: BaseSeries<DataPoint>? = null
    var mAmplitudeSeries: BaseSeries<DataPoint>? = null
    var mPhaseSeries: BaseSeries<DataPoint>? = null
    var mTimeSeries: BaseSeries<DataPoint>? = null
    var pointsInGraphs: Long = 0
    var numOfGraphs: Long = 0
    var transformer: RealDoubleFFT? = null

    fun initGraphView() {
        graphTime.title = "Time Domain"
        graphTime.viewport.setMaxY(10000.0)
        graphTime.viewport.setMinY(-10000.0)
        graphTime.viewport.isYAxisBoundsManual = true
        mTimeSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())

        mAmplitudeSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())
        graphAmplitude.title = "Frequency Domain Amplitude"
        graphAmplitude.viewport.setMaxY(375000.0)
        graphAmplitude.viewport.setMinY(-375000.0)
        graphAmplitude.viewport.isYAxisBoundsManual = true

        mPhaseSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())
        graphPhase.title = "Frequency Domain Phase"
        graphPhase.viewport.setMaxY(500.0)
        graphPhase.viewport.setMinY(-500.0)
        graphPhase.viewport.isYAxisBoundsManual = true

        graphFreqFull.title = "Full Signal Frequency Domain"
        mFullFreqSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())
        graphFreqFull.viewport.setMaxY(250000.0)
        graphFreqFull.viewport.setMinY(-250.0)
        graphFreqFull.viewport.isYAxisBoundsManual = true

        if (graphTime.series.count() > 0) {
            graphTime.removeAllSeries()
        }
        graphTime.addSeries(mTimeSeries)

        if (graphAmplitude.series.count() > 0) {
            graphAmplitude.removeAllSeries()
        }
        graphAmplitude.addSeries(mAmplitudeSeries)

        if (graphPhase.series.count() > 0) {
            graphPhase.removeAllSeries()
        }
        graphPhase.addSeries(mPhaseSeries)

        if (graphFreqFull.series.count() > 0) {
            graphFreqFull.removeAllSeries()
        }
        graphFreqFull.addSeries(mFullFreqSeries)
    }

    fun updateGraphView(mAudioRecord:AudioRecord) {
        val audioData = ShortArray(mMinBufferSize)
        var index = 0
        val num = audioData.size
        val dataAmplitudeFullSignal = DoubleArray(num/2)
        while (isRecording) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                //os?.write(audioData, 0, mMinBufferSize);
                val dataAmplitude = arrayOfNulls<DataPoint>(num/2)
                val dataPhase = arrayOfNulls<DataPoint>(num/2)
                val dataTime = arrayOfNulls<DataPoint>(num)
                // apply Fast Fourier Transform here
                transformer = RealDoubleFFT(num)
                val toTransform = DoubleArray(num)
                for (i in 0 until num) {
                    toTransform[i] = audioData[i].toDouble() / num
                    //toTransform[i] = audioData[i].toDouble()
                }
                transformer!!.ft(toTransform)
               // the real part of k-th complex FFT coeffients is x[2*k-1];
               // <br>
               //the imaginary part of k-th complex FFT coeffients is x[2*k-2].
                for (i in 0 until num/2) {
                    //output_power[i] = (real_output[i] * real_output[i] + imaginary_output[i] * imaginary_output[i]) / real_output.length;
                    dataAmplitude[i] = DataPoint(i.toDouble(), toTransform[i*2+1] * toTransform[i*2+1] + toTransform[i*2] * toTransform[i*2])
                    dataAmplitudeFullSignal[i] += dataAmplitude[i]!!.y
                }

                for(i in 0 until num/2) {
                    dataPhase[i] = DataPoint(i.toDouble(),atan(toTransform[i*2]/toTransform[i*2+1])*180/PI)
                }

                for (i in 0 until num) {
                    dataTime[i] = DataPoint(i.toDouble(), audioData[i].toDouble())
                }


                val listTime: List<DataPoint> = dataTime.toList().filterNotNull()
                index++
                dataGraphs.currentRecordTimeDomain.add(DataGraph(listTime))
                GlobalScope.launch( Dispatchers.Main ){
                    mAmplitudeSeries!!.resetData(dataAmplitude)
                    mPhaseSeries!!.resetData(dataPhase)
                    mTimeSeries!!.resetData(dataTime)
                }
            }

        }
        pointsInGraphs = audioData.size.toLong()
        numOfGraphs = index.toLong()
        var dataFullList = mutableListOf<DataPoint>()
        dataGraphs.currentRecordFullFreqDomain = mutableListOf(DataGraph(dataFullList))
        for (i in 0 until num/2) {
            dataFullList.add(DataPoint(i.toDouble(), dataAmplitudeFullSignal[i]/numOfGraphs))
        }
    }

    fun replayGraphView(mAudioRecord:AudioRecord): Boolean {
        var index = 0
        val audioData = ShortArray(mMinBufferSize)

        while (isPlaying && index < dataGraphs.currentRecordTimeDomain.size) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            val numTime =  dataGraphs.currentRecordTimeDomain[index].dataPoints.size
            val dataTime = arrayOfNulls<DataPoint>(numTime)
            val dataAmplitude = arrayOfNulls<DataPoint>(numTime/2)
            val dataPhase = arrayOfNulls<DataPoint>(numTime/2)
            for (i in 0 until numTime) {
                dataTime[i] = dataGraphs.currentRecordTimeDomain[index].dataPoints[i]
            }
            transformer = RealDoubleFFT(numTime)
            val toTransform = DoubleArray(numTime)
            for (i in 0 until numTime) {
                toTransform[i] = dataTime[i]!!.y / numTime
            }
            transformer!!.ft(toTransform)
            for (i in 0 until numTime/2) {
                //output_power[i] = (real_output[i] * real_output[i] + imaginary_output[i] * imaginary_output[i]) / real_output.length;
                dataAmplitude[i] = DataPoint(i.toDouble(), toTransform[i*2+1] * toTransform[i*2+1] + toTransform[i*2] * toTransform[i*2])
            }
            for(i in 0 until numTime/2) {
                dataPhase[i] = DataPoint(i.toDouble(),atan(toTransform[i*2]/toTransform[i*2+1])*180/PI)
            }

            GlobalScope.launch( Dispatchers.Main ){
                mTimeSeries!!.resetData(dataTime)
                mAmplitudeSeries!!.resetData(dataAmplitude)
                mPhaseSeries!!.resetData(dataPhase)
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