package com.example.animalsoundrecognition

import android.media.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ca.uol.aig.fftpack.RealDoubleFFT
import com.example.animalsoundrecognition.model.DataGraph
import com.example.animalsoundrecognition.model.DataGraphs
import com.example.animalsoundrecognition.model.DataSound
import com.example.animalsoundrecognition.server.SoundService
import com.example.animalsoundrecognition.server.SoundServiceHandler
import com.example.animalsoundrecognition.soundprocessing.GraphHandler
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.BaseSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.FileOutputStream
import java.io.IOException


//const val SAMPLE_RATE = 44100
const val SAMPLE_RATE = 10100
const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

class MainActivity : AppCompatActivity() {

    lateinit var serviceHandler:SoundServiceHandler
    lateinit var graphHandler:GraphHandler
    private lateinit var textTest:TextView
    private lateinit var animalNameText:EditText
    var soundStartingTime:Long = 0
    var currentDuration:Long = 0

    private var fileName: String = ""
    var os: FileOutputStream? = null

    private var recorder: MediaRecorder? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mRecordThread: Thread? = null

    private var pointsInGraphs: Long = 0
    private var numOfGraphs: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        animalNameText = findViewById(R.id.textAnimalName)
        textTest = findViewById(R.id.textTest)
        mMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        textTest.text = "DEFAULT"
        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest.3gp"
        createServiceHandler()
        graphHandler = GraphHandler(findViewById(R.id.graph), findViewById(R.id.graphTime), findViewById(R.id.graphFreqFull))
    }


    fun createServiceHandler() {
        serviceHandler = SoundServiceHandler()
    }

    override fun onStart() {
        super.onStart()
        graphHandler.initGraphView()
    }

    override fun onPause() {
        super.onPause()
        stopRecording()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        GlobalScope.launch(Dispatchers.IO) {
            when (item?.itemId) {
                R.id.device_access_mic -> {
                    startRecording()
                }
                R.id.device_access_mic_muted -> {
                    stopRecording()
                }
                R.id.device_access_audio_play -> {
                    startPlaying()
                }
                R.id.device_access_audio_stop -> {
                    stopPlaying()
                }
                R.id.upload_sound -> {
                    val sound = createDataSound(false)
                    serviceHandler.postSound(textTest, sound)
                }
                R.id.check_sound -> {
                    val sound = createDataSound(false)
                    serviceHandler.checkSound(textTest, sound)
                }
                R.id.get_sound_types -> {
                    serviceHandler.getSounds(textTest)
                }
                R.id.get_sound -> {
                    serviceHandler.getSound(textTest, animalNameText, graphHandler.dataGraphs)
                }
                R.id.delete_sound -> {
                    serviceHandler.deleteSound(textTest, animalNameText)
                }
            }
            return@launch
        }

        return item?.let { super.onOptionsItemSelected(it) }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val recordAudioOption = menu!!.findItem(R.id.device_access_mic)
        val stopRecordAudioOption = menu.findItem(R.id.device_access_mic_muted)
        val playSoundOption = menu.findItem(R.id.device_access_audio_play)
        val stopPlaySoundOption = menu.findItem(R.id.device_access_audio_stop)

        if (isRecording) {
            changeMenuOptionVisibility(recordAudioOption, false)
            changeMenuOptionVisibility(stopRecordAudioOption, true)
        } else {
            changeMenuOptionVisibility(recordAudioOption, true)
            changeMenuOptionVisibility(stopRecordAudioOption, false)
        }

        if(isPlaying) {
            changeMenuOptionVisibility(playSoundOption, false)
            changeMenuOptionVisibility(stopPlaySoundOption, true)
        } else {
            changeMenuOptionVisibility(playSoundOption, true)
            changeMenuOptionVisibility(stopPlaySoundOption, false)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    private fun changeMenuOptionVisibility(option: MenuItem, status:Boolean) {
        option.isEnabled = status
        option.isVisible = status
    }

    private fun startPlaying() {
        graphHandler.mFreqSeries?.resetData(arrayOf<DataPoint>())
        graphHandler.mTimeSeries?.resetData(arrayOf<DataPoint>())
        graphHandler.mFullFreqSeries?.resetData(arrayOf<DataPoint>())
        /*
        val sound = createDataSound(true)
        val stringBuilder = sound.toString()
        GlobalScope.launch( Dispatchers.Main ){
            textTest.text = stringBuilder
        }
         */

        mMediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("", "prepare() failed")
            }
        }
        isPlaying = true

        mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, mMinBufferSize)
        mAudioRecord!!.startRecording()

        val playedOut = graphHandler.replayGraphView()
        if(playedOut) {
            stopPlaying()
        }
        invalidateOptionsMenu()
    }

    private fun createDataSound(includeFreqDomain:Boolean): DataSound {
        val dataPoints: MutableList<DataPoint> = mutableListOf()
        val timePoints: MutableList<DataPoint> = mutableListOf()
        for (graphs in graphHandler.dataGraphs.currentRecordTimeDomain) {
            timePoints.addAll(graphs.dataPoints)
        }
        val sound = DataSound(animalNameText.text.toString(), currentDuration, pointsInGraphs, numOfGraphs, dataPoints, timePoints)
        return sound
    }

    private fun stopPlaying() {
        mMediaPlayer?.release()
        mMediaPlayer = null
        isPlaying = false

        mAudioRecord?.stop()
        mRecordThread = null
        mAudioRecord?.release()
        mAudioRecord = null
        //mFreqSeries?.resetData(arrayOf<DataPoint>())
        //mTimeSeries?.resetData(arrayOf<DataPoint>())
        invalidateOptionsMenu()
    }


    private fun startRecording() {
        graphHandler.dataGraphs.currentRecordTimeDomain.clear()
        mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, mMinBufferSize)
        mAudioRecord!!.startRecording()
        isRecording = true

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
        invalidateOptionsMenu()
    }

    private fun stopRecording() {
        currentDuration = System.currentTimeMillis() - soundStartingTime
        if (mAudioRecord != null) {
            if (isRecording) {
                mAudioRecord?.stop()
                isRecording = false
                mRecordThread = null
            }
            mAudioRecord?.release()
            mAudioRecord = null
            graphHandler.mFreqSeries?.resetData(arrayOf<DataPoint>())
            graphHandler.mTimeSeries?.resetData(arrayOf<DataPoint>())
        }

        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        invalidateOptionsMenu()
    }

    companion object {
        var mAudioRecord: AudioRecord? = null
        var isPlaying = false
        var isRecording = false
        var mMinBufferSize = 0
    }

}