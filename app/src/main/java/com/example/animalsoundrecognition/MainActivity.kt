package com.example.animalsoundrecognition

import android.media.*
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ca.uol.aig.fftpack.RealDoubleFFT
import com.example.animalsoundrecognition.model.DataGraph
import com.example.animalsoundrecognition.model.DataSound
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.BaseSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


const val SAMPLE_RATE = 44100
const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

class MainActivity : AppCompatActivity() {

    lateinit var service:SoundService
    //var okHttpClient: OkHttpClient? = null

    // tutaj ustaw swoje lokalne ip
    val ipString = "http://192.168.1.3:8080"
    //.baseUrl("http://10.0.0.5:8080/")
    //.baseUrl("http://192.168.1.3:8080/")
    private lateinit var textTest:TextView
    var currentRecord:MutableList<DataGraph> = mutableListOf()
    var soundStartingTime:Long = 0
    var currentDuration:Long = 0

    private var fileName: String = ""
    var os: FileOutputStream? = null

    private var mAudioRecord: AudioRecord? = null
    private var recorder: MediaRecorder? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mRecordThread: Thread? = null
    private var mBaseSeries: BaseSeries<DataPoint>? = null

    private var mMinBufferSize = 0
    private var isRecording = false
    private var isPlaying = false
    private var isBarGraph = false

    private var transformer: RealDoubleFFT? = null

    private lateinit var graph: GraphView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textTest = findViewById(R.id.textTest)
        graph = findViewById(R.id.graph)
        mMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        textTest.text = "DEFAULT"

        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest.3gp"

        createClient()
    }

    fun createClient() {
        val retrofit = Retrofit.Builder()
            .baseUrl(ipString)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(SoundService::class.java)
        //getQuizzes()
    }
    /*

    fun getQuizzes() {
        val call = service.getQuizzes()
        call.enqueue(object : Callback<List<Quiz>> {
            override fun onResponse(call: Call<List<Quiz>>, response: Response<List<Quiz>>) {
                if (response.code() == 200) {
                    //
                    textTest.text = response.toString()

                    val quiz = response.body()!!

                    val stringBuilder = quiz[1].toString();

                    textTest.text = stringBuilder

                } else
                textTest.text = "cOS zle"
            }
            override fun onFailure(call: Call<List<Quiz>>, t: Throwable) {
                //
                val text = "MSG:" + t.message + "CAUSE: " + t.cause
                //textTest.text = "FAIL"
                textTest.text = text
            }
        })
    }

    fun postQuiz() {

        val quiz:Quiz = Quiz("My Question","What to do?", listOf("Try","Catch","Cook", "Sleep"), listOf(1,2))
        Log.d("ss", quiz.toString())

        val call = service.postQuiz(quiz)
        call.enqueue(object : Callback<Quiz> {
            override fun onResponse(call: Call<Quiz>, response: Response<Quiz>) {
                if (response.code() == 200) {
                    //
                    textTest.text = response.toString()

                    val quiz = response.body()!!
                    val stringBuilder = quiz.toString();
                    textTest.text = stringBuilder

                } else
                textTest.text = "cOS zle"
            }

            override fun onFailure(call: Call<Quiz>, t: Throwable) {
                val text = "MSG:" + t.message + "CAUSE: " + t.cause
                //textTest.text = "FAIL"
                textTest.text = text
            }
        })
    }

     */

    fun postSound() {
        val sound = DataSound("My sound", currentDuration, currentRecord)
        //postSound(dataSound)
        val call = service.postSound(sound)
        call.enqueue(object : Callback<DataSound> {
            override fun onResponse(call: Call<DataSound>, response: Response<DataSound>) {
                if (response.code() == 200) {
                    //
                    textTest.text = response.toString()
                    val dataSound = response.body()!!
                    val stringBuilder = dataSound.toString();
                    textTest.text = stringBuilder

                } else
                    textTest.text = "something wrong"
            }

            override fun onFailure(call: Call<DataSound>, t: Throwable) {
                val text = "MSG:" + t.message + "CAUSE: " + t.cause
                //textTest.text = "FAIL"
                textTest.text = text
            }
        })
    }

    override fun onStart() {
        super.onStart()
        initGraphView()
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
        when (item?.itemId) {
            R.id.device_access_mic -> {
                startRecording()
                invalidateOptionsMenu()
                return true
            }
            R.id.device_access_mic_muted -> {
                stopRecording()
                invalidateOptionsMenu()
                return true
            }
            R.id.device_access_audio_play -> {
                startPlaying()
                invalidateOptionsMenu()
                return true
            }
            R.id.device_access_audio_stop -> {
                stopPlaying()
                invalidateOptionsMenu()
                return true
            }
            R.id.check_sound -> {
                postSound()
                return true
            }
            R.id.action_settings -> {
                initGraphView()
                return true
            }
        }
        return item?.let { super.onOptionsItemSelected(it) }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val item1: MenuItem?
        val item2: MenuItem?
        val item3: MenuItem?
        val item4: MenuItem?

        if (!isRecording) {
            item1 = menu?.findItem(R.id.device_access_mic_muted)
            item2 = menu?.findItem(R.id.device_access_mic)
        } else {
            item1 = menu?.findItem(R.id.device_access_mic)
            item2 = menu?.findItem(R.id.device_access_mic_muted)
        }

        if(!isPlaying) {
            item3 = menu?.findItem(R.id.device_access_audio_stop)
            item4 = menu?.findItem(R.id.device_access_audio_play)
        } else {
            item3 = menu?.findItem(R.id.device_access_audio_play)
            item4 = menu?.findItem(R.id.device_access_audio_stop)
        }



        if (item1 != null) {
            item1.isEnabled = false
            item1.isVisible = false
            if (item2 != null) {
                item2.isEnabled = true
                item2.isVisible = true
            }
        }

        if (item3 != null) {
            item3.isEnabled = false
            item3.isVisible = false
            if (item4 != null) {
                item4.isEnabled = true
                item4.isVisible = true
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    private fun startPlaying() {

        val sound = DataSound("My sound", currentDuration, currentRecord)
        val stringBuilder = sound.toString();
        textTest.text = stringBuilder


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
    }

    private fun stopPlaying() {
        mMediaPlayer?.release()
        mMediaPlayer = null
        isPlaying = false

        mRecordThread = null
        mAudioRecord?.stop()
        mRecordThread = null
        mAudioRecord?.release()
        mAudioRecord = null
        mBaseSeries?.resetData(arrayOf<DataPoint>())
    }


    private fun startRecording() {

        currentRecord.clear()
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
            mBaseSeries?.resetData(arrayOf<DataPoint>())
        }

        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    private fun initGraphView() {
        if (isBarGraph) {
            mBaseSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())
            graph.title = "Time Domain"
        } else {
            mBaseSeries = BarGraphSeries<DataPoint>(arrayOf<DataPoint>())
            graph.title = "Frequency Domain"
        }

        isBarGraph = !isBarGraph

        if (graph.series.count() > 0) {
            graph.removeAllSeries()
        }
        graph.addSeries(mBaseSeries)
    }

    private fun replayGraphView() {
        var index = 0
        val audioData = ShortArray(mMinBufferSize)
        while (isPlaying && index < currentRecord.size) {
                val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
                if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                val num =  currentRecord[index].dataPoints.size
                //os?.write(audioData, 0, mMinBufferSize);
                val data = arrayOfNulls<DataPoint>(num)
                if (isBarGraph) {
                    for (i in 0 until num) {
                        data[i] = currentRecord[index].dataPoints[i]
                    }
                } else {
                    for (i in 0 until num) {
                        data[i] = currentRecord[index].dataPoints[i]
                    }
                }
                this@MainActivity.runOnUiThread { mBaseSeries!!.resetData(data) }
                }
            index++
        }
    }

    private fun updateGraphView() {
        val audioData = ShortArray(mMinBufferSize)
        while (isRecording) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                val num = audioData.size
                //os?.write(audioData, 0, mMinBufferSize);
                val data = arrayOfNulls<DataPoint>(num)
                if (isBarGraph) {
                    // apply Fast Fourier Transform here
                    transformer = RealDoubleFFT(num)
                    val toTransform = DoubleArray(num)
                    for (i in 0 until num) {
                        toTransform[i] = audioData[i].toDouble() / Short.MAX_VALUE
                    }
                    transformer!!.ft(toTransform)
                    for (i in 0 until num) {
                        data[i] = DataPoint(i.toDouble(), toTransform[i])
                    }
                } else {
                    for (i in 0 until num) {
                        data[i] = DataPoint(i.toDouble(), audioData[i].toDouble())
                    }
                }

                val list: List<DataPoint> = data.toList().filterNotNull()
                currentRecord.add(DataGraph(list))

                this@MainActivity.runOnUiThread { mBaseSeries!!.resetData(data) }
            }

        }
    }

}