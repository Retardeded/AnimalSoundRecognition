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
import com.example.animalsoundrecognition.model.DataSound
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.BarGraphSeries
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
const val SAMPLE_RATE = 4410
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
    private lateinit var animalNameText:EditText
    var currentRecordFreqDomain:MutableList<DataGraph> = mutableListOf()
    var soundStartingTime:Long = 0
    var currentDuration:Long = 0

    private var fileName: String = ""
    var os: FileOutputStream? = null

    private var mAudioRecord: AudioRecord? = null
    private var recorder: MediaRecorder? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mRecordThread: Thread? = null
    private var mPostThread: Thread? = null
    private var mUploadThread: Thread? = null
    private var mGetThread: Thread? = null
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

        animalNameText = findViewById(R.id.textAnimalName)
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
    }

    suspend fun getSounds() {
        val response = service.getSounds()
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val quiz = response.body()!!
                val stringBuilder = quiz.toString();
                textTest.text = stringBuilder
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }

    suspend fun getSound() {
        val id = animalNameText.text.toString()
        val response = service.getSound(id)
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val soundData = response.body()!!
                val stringBuilder = soundData.toString();
                textTest.text = stringBuilder
                currentRecordFreqDomain = loadDataSound(soundData)
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }

    suspend fun postSound() {
        val sound = createDataSound()
        val response = service.postSound(sound)
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val dataSound = response.body()!!
                val stringBuilder = dataSound.toString();
                textTest.text = stringBuilder
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }

    suspend fun checkSound() {
        val sound = createDataSound()
        val response = service.checkSound(sound)
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val dataSound = response.body()!!
                val stringBuilder = dataSound.toString();
                textTest.text = stringBuilder
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
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
        GlobalScope.launch(Dispatchers.IO) {
            when (item?.itemId) {
                R.id.device_access_mic -> {
                    startRecording()
                    invalidateOptionsMenu()
                    return@launch
                }
                R.id.device_access_mic_muted -> {
                    stopRecording()
                    invalidateOptionsMenu()
                    return@launch
                }
                R.id.device_access_audio_play -> {
                    startPlaying()
                    invalidateOptionsMenu()
                    return@launch
                }
                R.id.device_access_audio_stop -> {
                    stopPlaying()
                    invalidateOptionsMenu()
                    return@launch
                }
                R.id.upload_sound -> {
                    postSound()
                    return@launch
                }
                R.id.check_sound -> {
                    checkSound()
                    return@launch
                }
                R.id.get_sound_types -> {
                    getSounds()
                    return@launch
                }
                R.id.get_sound -> {
                    getSound()
                    return@launch
                }
                R.id.action_settings -> {
                    initGraphView()
                    return@launch
                }
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

        val sound = createDataSound()
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
        isPlaying = true

        mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, mMinBufferSize)
        mAudioRecord!!.startRecording()

        mRecordThread = Thread(Runnable { replayGraphView() })
        mRecordThread!!.start()
    }

    private fun loadDataSound(sound:DataSound): MutableList<DataGraph> {
        val pointsInGraph = 3584
        val dataGraphs: MutableList<DataGraph> = mutableListOf()
        val numberOfGraphs = (sound.dataPoints.size / pointsInGraph)-1
        for (i in 0..numberOfGraphs) {
            val graph = DataGraph(sound.dataPoints.subList(i*pointsInGraph, (i+1)*pointsInGraph))
            dataGraphs.add(graph)
        }

        return dataGraphs
    }

    private fun createDataSound(): DataSound {
        val dataPoints: MutableList<DataPoint> = mutableListOf()
        for (graphs in currentRecordFreqDomain) {
            dataPoints.addAll(graphs.dataPoints)
        }
        val sound = DataSound(animalNameText.text.toString(), currentDuration, dataPoints)
        return sound
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

        currentRecordFreqDomain.clear()
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
        while (isPlaying && index < currentRecordFreqDomain.size) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                val num =  currentRecordFreqDomain[index].dataPoints.size
                //os?.write(audioData, 0, mMinBufferSize);
                val data = arrayOfNulls<DataPoint>(num)
                if (isBarGraph) {
                    for (i in 0 until num) {
                        data[i] = currentRecordFreqDomain[index].dataPoints[i]
                    }
                } else {
                    for (i in 0 until num) {
                        data[i] = currentRecordFreqDomain[index].dataPoints[i]
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
                currentRecordFreqDomain.add(DataGraph(list))

                this@MainActivity.runOnUiThread { mBaseSeries!!.resetData(data) }
            }

        }
    }

}