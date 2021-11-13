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

        mRecordThread = Thread(Runnable { replayGraphView() })
        mRecordThread!!.start()
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

        mRecordThread = Thread(Runnable { updateGraphView() })
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

    private fun replayGraphView() {
        var index = 0
        val audioData = ShortArray(mMinBufferSize)

        while (isPlaying && index < graphHandler.dataGraphs.currentRecordTimeDomain.size) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                val numTime =  graphHandler.dataGraphs.currentRecordTimeDomain[index].dataPoints.size
                val dataTime = arrayOfNulls<DataPoint>(numTime)
                val data = arrayOfNulls<DataPoint>(numTime)
                for (i in 0 until numTime) {
                    dataTime[i] = graphHandler.dataGraphs.currentRecordTimeDomain[index].dataPoints[i]
                }
                graphHandler.transformer = RealDoubleFFT(numTime)
                val toTransform = DoubleArray(numTime)
                for (i in 0 until numTime) {
                    //toTransform[i] = audioData[i].toDouble() / Short.MAX_VALUE
                    toTransform[i] = dataTime[i]!!.y
                }
                graphHandler.transformer!!.ft(toTransform)
                for (i in 0 until numTime) {
                    data[i] = DataPoint(i.toDouble(), toTransform[i])
                }

                this@MainActivity.runOnUiThread {
                    graphHandler.mTimeSeries!!.resetData(dataTime)
                    graphHandler.mFreqSeries!!.resetData(data)
                }
            }
            index++
        }


        if(graphHandler.dataGraphs.currentRecordFullFreqDomain.size > 0)
        {
            val num =  graphHandler.dataGraphs.currentRecordFullFreqDomain[0].dataPoints.size
            val data = arrayOfNulls<DataPoint>(num)
            for (i in 0 until num) {
                data[i] = graphHandler.dataGraphs.currentRecordFullFreqDomain[0].dataPoints[i]
            }
            this@MainActivity.runOnUiThread {
                graphHandler.mFullFreqSeries!!.resetData(data)
            }
        }

        stopPlaying()
    }


    private fun updateGraphView() {
        val audioData = ShortArray(mMinBufferSize)
        var index = 0
        while (isRecording) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                val num = audioData.size
                //os?.write(audioData, 0, mMinBufferSize);
                val data = arrayOfNulls<DataPoint>(num)
                val dataTime = arrayOfNulls<DataPoint>(num)
                // apply Fast Fourier Transform here
                graphHandler.transformer = RealDoubleFFT(num)
                val toTransform = DoubleArray(num)
                for (i in 0 until num) {
                    //toTransform[i] = audioData[i].toDouble() / Short.MAX_VALUE
                    toTransform[i] = audioData[i].toDouble()
                }
                graphHandler.transformer!!.ft(toTransform)
                for (i in 0 until num) {
                    data[i] = DataPoint(i.toDouble(), toTransform[i])
                }

                for (i in 0 until num) {
                    dataTime[i] = DataPoint(i.toDouble(), audioData[i].toDouble())
                }

                val list: List<DataPoint> = data.toList().filterNotNull()
                val listTime: List<DataPoint> = dataTime.toList().filterNotNull()

                /*
                println("size:" + num)
                println("list:" + list )
                var highPeak = list.maxByOrNull { it.y }
                var lowPeak = list.minByOrNull { it.y }
                println("max:" + highPeak)
                println("min:" + lowPeak)
                println("TIME")
                println("size:" + num)
                println("list:" + listTime)
                var highPeakT = listTime.maxByOrNull { it.y }
                var lowPeakT = listTime.minByOrNull { it.y }
                println("max:" + highPeakT)
                println("min:" + lowPeakT)
                 */

                index++
                graphHandler.dataGraphs.currentRecordTimeDomain.add(DataGraph(listTime))

                this@MainActivity.runOnUiThread {
                    graphHandler.mFreqSeries!!.resetData(data)
                    graphHandler.mTimeSeries!!.resetData(dataTime)
                }
            }

        }
        pointsInGraphs = audioData.size.toLong()
        numOfGraphs = index.toLong()
        println("index::" + index)
    }

    companion object {
        var mAudioRecord: AudioRecord? = null
        var isPlaying = false
        var isRecording = false
        var mMinBufferSize = 0
    }

}