package com.example.animalsoundrecognition.soundprocessing

import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import android.widget.TextView
import com.example.animalsoundrecognition.AUDIO_FORMAT
import com.example.animalsoundrecognition.CHANNEL_CONFIG
import com.example.animalsoundrecognition.MainActivity
import com.example.animalsoundrecognition.SAMPLE_RATE
import com.example.animalsoundrecognition.model.DataSound
import com.jjoe64.graphview.series.DataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

class RecordHandler(val graphHandler: GraphHandler, val fileName:String) {

    private var recorder: MediaRecorder? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mRecordThread: Thread? = null
    var soundStartingTime:Long = 0
    var currentDuration:Long = 0



    fun startPlaying(textTest: TextView, animalNameText: TextView) {
        graphHandler.mFreqSeries?.resetData(arrayOf<DataPoint>())
        graphHandler.mTimeSeries?.resetData(arrayOf<DataPoint>())
        graphHandler.mFullFreqSeries?.resetData(arrayOf<DataPoint>())

        val sound = createDataSound(true, animalNameText)
        val stringBuilder = sound.toString()
        GlobalScope.launch( Dispatchers.Main ){
            textTest.text = stringBuilder
        }

        mMediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("", "prepare() failed")
            }
        }
        MainActivity.isPlaying = true

        MainActivity.mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, MainActivity.mMinBufferSize)
        MainActivity.mAudioRecord!!.startRecording()

        val playedOut = graphHandler.replayGraphView()
        if(playedOut) {
            stopPlaying()
        }
    }

    fun stopPlaying() {
        mMediaPlayer?.release()
        mMediaPlayer = null
        MainActivity.isPlaying = false

        MainActivity.mAudioRecord?.stop()
        mRecordThread = null
        MainActivity.mAudioRecord?.release()
        MainActivity.mAudioRecord = null
    }

    fun startRecording() {
        graphHandler.dataGraphs.currentRecordTimeDomain.clear()
        MainActivity.mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
            MainActivity.mMinBufferSize
        )
        MainActivity.mAudioRecord!!.startRecording()
        MainActivity.isRecording = true

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("tag", "prepare() failed")
            }

            start()
        }

        mRecordThread = Thread(Runnable { graphHandler.updateGraphView() })
        mRecordThread!!.start()
        soundStartingTime = System.currentTimeMillis()
    }

    fun stopRecording() {
        currentDuration = System.currentTimeMillis() - soundStartingTime
        if (MainActivity.mAudioRecord != null) {
            if (MainActivity.isRecording) {
                MainActivity.mAudioRecord?.stop()
                MainActivity.isRecording = false
                mRecordThread = null
            }
            MainActivity.mAudioRecord?.release()
            MainActivity.mAudioRecord = null
            graphHandler.mFreqSeries?.resetData(arrayOf<DataPoint>())
            graphHandler.mTimeSeries?.resetData(arrayOf<DataPoint>())
        }

        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    fun createDataSound(includeGraph:Boolean, animalNameText: TextView): DataSound {
        val dataPoints: MutableList<DataPoint> = mutableListOf()
        val timePoints: MutableList<DataPoint> = mutableListOf()
        if(includeGraph) {
            for (graphs in graphHandler.dataGraphs.currentRecordTimeDomain) {
                timePoints.addAll(graphs.dataPoints)
            }
        }
        val sound = DataSound(animalNameText.text.toString(), currentDuration, graphHandler.pointsInGraphs, graphHandler.numOfGraphs, dataPoints, timePoints)
        return sound
    }
}