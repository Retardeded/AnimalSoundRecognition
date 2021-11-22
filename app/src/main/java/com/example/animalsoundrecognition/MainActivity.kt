package com.example.animalsoundrecognition

import android.media.*
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.animalsoundrecognition.server.SoundServiceHandler
import com.example.animalsoundrecognition.soundprocessing.GraphHandler
import com.example.animalsoundrecognition.soundprocessing.RecordHandler
import com.example.animalsoundrecognition.soundprocessing.RecordHandler.Companion.isPlaying
import com.example.animalsoundrecognition.soundprocessing.RecordHandler.Companion.isRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var recordHandler:RecordHandler
    lateinit var serviceHandler:SoundServiceHandler
    lateinit var graphHandler:GraphHandler

    lateinit var textTest:TextView
    lateinit var animalNameText:EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        animalNameText = findViewById(R.id.textAnimalName)
        textTest = findViewById(R.id.textTest)
        textTest.text = "DEFAULT"
        serviceHandler = SoundServiceHandler()
        graphHandler = GraphHandler(findViewById(R.id.graph), findViewById(R.id.graphTime), findViewById(R.id.graphFreqFull))
        recordHandler = RecordHandler(graphHandler, "${externalCacheDir?.absolutePath}/audiorecordtest.3gp")
    }

    override fun onStart() {
        super.onStart()
        graphHandler.initGraphView()
    }

    override fun onPause() {
        super.onPause()
        recordHandler.stopRecording()
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
                    recordHandler.startRecording()
                    invalidateOptionsMenu()
                }
                R.id.device_access_mic_muted -> {
                    recordHandler.stopRecording()
                    invalidateOptionsMenu()
                }
                R.id.device_access_audio_play -> {
                    recordHandler.startPlaying(textTest, animalNameText)
                    invalidateOptionsMenu()
                }
                R.id.device_access_audio_stop -> {
                    recordHandler.stopPlaying()
                    invalidateOptionsMenu()
                }
                R.id.upload_sound -> {
                    val sound = recordHandler.createDataSound(true, animalNameText)
                    serviceHandler.postSound(textTest, sound)
                }
                R.id.check_sound -> {
                    val sound = recordHandler.createDataSound(true, animalNameText)
                    serviceHandler.checkSound(textTest, sound)
                }
                R.id.check_sound_freq -> {
                    val sound = recordHandler.createDataSound(true, animalNameText)
                    serviceHandler.checkSoundFreqDomain(textTest, sound)
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

}