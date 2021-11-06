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

    lateinit var service:SoundService
    //var okHttpClient: OkHttpClient? = null
    // tutaj ustaw swoje lokalne ip
    val ipString = "http://192.168.1.3:8080"
    //.baseUrl("http://10.0.0.5:8080/")
    //.baseUrl("http://192.168.1.3:8080/")
    private lateinit var textTest:TextView
    private lateinit var animalNameText:EditText
    var currentRecordFreqDomain:MutableList<DataGraph> = mutableListOf()
    var currentRecordTimeDomain:MutableList<DataGraph> = mutableListOf()
    var soundStartingTime:Long = 0
    var currentDuration:Long = 0

    private var fileName: String = ""
    var os: FileOutputStream? = null

    private var mAudioRecord: AudioRecord? = null
    private var recorder: MediaRecorder? = null
    private var mMediaPlayer: MediaPlayer? = null
    private var mRecordThread: Thread? = null
    private var mBaseSeries: BaseSeries<DataPoint>? = null
    private var mTimeSeries: BaseSeries<DataPoint>? = null

    private var mMinBufferSize = 0
    private var isRecording = false
    private var isPlaying = false
    private var isBarGraph = false

    private var transformer: RealDoubleFFT? = null

    private lateinit var graph: GraphView
    private lateinit var graphTime: GraphView
    private var pointsInGraphs: Long = 0
    private var numOfGraphs: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        animalNameText = findViewById(R.id.textAnimalName)
        textTest = findViewById(R.id.textTest)
        graph = findViewById(R.id.graph)
        graphTime = findViewById(R.id.graphTime)
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
                currentRecordFreqDomain = loadDataSound(soundData.freqDomainPoints)
                currentRecordTimeDomain = loadDataSound(soundData.timeDomainPoints)
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }
    }

    suspend fun postSound() {
        val sound = createDataSound(false)
        val response = service.postSound(sound)
        GlobalScope.launch(Dispatchers.Main) {
            if (response.isSuccessful) {
                textTest.text = response.toString()
                val dataSound = response.body()!!
                val stringBuilder = dataSound.toString();
                textTest.text = stringBuilder

                println(sound.freqDomainPoints.size)
                println(dataSound.freqDomainPoints.size)
            }
            else {
                val text = "MSG:" + response.message() + "CAUSE: " + response.errorBody()
                textTest.text = text
            }
        }

    }

    suspend fun checkSound() {
        val sound = createDataSound(false)
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
                    postSound()
                }
                R.id.check_sound -> {
                    checkSound()
                }
                R.id.get_sound_types -> {
                    getSounds()
                }
                R.id.get_sound -> {
                    getSound()
                }
                R.id.action_settings -> {
                    initGraphView()
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
        val sound = createDataSound(true)
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
        invalidateOptionsMenu()
    }

    private fun loadDataSound(soundData:List<DataPoint>): MutableList<DataGraph> {
        val dataGraphs: MutableList<DataGraph> = mutableListOf()
        val numberOfGraphs = (soundData.size / pointsInGraphs)-1
        for (i in 0..numberOfGraphs) {
            val graph = DataGraph(
                soundData.subList(
                    ((i * pointsInGraphs).toInt()),
                    ((i + 1) * pointsInGraphs).toInt()
                )
            )
            dataGraphs.add(graph)
        }
        return dataGraphs
    }

    private fun createDataSound(includeFreqDomain:Boolean): DataSound {
        val dataPoints: MutableList<DataPoint> = mutableListOf()
        val timePoints: MutableList<DataPoint> = mutableListOf()
        if(includeFreqDomain) {
            for (graphs in currentRecordFreqDomain) {
                dataPoints.addAll(graphs.dataPoints)
            }
        }
        for (graphs in currentRecordTimeDomain) {
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
        mBaseSeries?.resetData(arrayOf<DataPoint>())
        mTimeSeries?.resetData(arrayOf<DataPoint>())
        invalidateOptionsMenu()
    }


    private fun startRecording() {

        currentRecordFreqDomain.clear()
        currentRecordTimeDomain.clear()
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

        //updateGraphView()
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
            mBaseSeries?.resetData(arrayOf<DataPoint>())
            mTimeSeries?.resetData(arrayOf<DataPoint>())
        }

        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        invalidateOptionsMenu()
    }

    private fun initGraphView() {
        if (isBarGraph) {
            mBaseSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())
            graph.title = "Time Domain"
        } else {
            mBaseSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())
            graph.title = "Frequency Domain"
        }


        graphTime.title = "Time Domain"
        mTimeSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())

        isBarGraph = !isBarGraph

        if (graph.series.count() > 0) {
            graph.removeAllSeries()
        }
        graph.addSeries(mBaseSeries)

        if (graphTime.series.count() > 0) {
            graphTime.removeAllSeries()
        }
        graphTime.addSeries(mTimeSeries)
    }


    private fun replayGraphView() {
        var index = 0
        val audioData = ShortArray(mMinBufferSize)
        while (isPlaying && index < currentRecordFreqDomain.size || index < currentRecordTimeDomain.size) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                val num =  currentRecordFreqDomain[index].dataPoints.size
                val numTime =  currentRecordTimeDomain[index].dataPoints.size
                //os?.write(audioData, 0, mMinBufferSize);
                val data = arrayOfNulls<DataPoint>(num)
                val dataTime = arrayOfNulls<DataPoint>(num)
                for (i in 0 until num) {
                    data[i] = currentRecordFreqDomain[index].dataPoints[i]
                }

                for (i in 0 until numTime) {
                    dataTime[i] = currentRecordTimeDomain[index].dataPoints[i]
                }

                this@MainActivity.runOnUiThread {
                    mBaseSeries!!.resetData(data)
                    mTimeSeries!!.resetData(dataTime)
                }
            }
            index++
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
                if (isBarGraph) {
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
                } else {
                    for (i in 0 until num) {
                        data[i] = DataPoint(i.toDouble(), audioData[i].toDouble())
                    }
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


                currentRecordFreqDomain.add(DataGraph(list))
                currentRecordTimeDomain.add(DataGraph(listTime))

                this@MainActivity.runOnUiThread {
                    mBaseSeries!!.resetData(data)
                    mTimeSeries!!.resetData(dataTime)
                }
            }

        }
        pointsInGraphs = audioData.size.toLong()
        numOfGraphs = index.toLong()
        println("index::" + index)

    }

}