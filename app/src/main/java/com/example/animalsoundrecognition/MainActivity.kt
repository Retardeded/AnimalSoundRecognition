package com.example.animalsoundrecognition

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.animalsoundrecognition.databinding.ActivityMainBinding
import com.example.animalsoundrecognition.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    lateinit var binding: ActivityMainBinding
    val viewModel: MainViewModel by viewModels()

    /*

    lateinit var recordHandler:RecordHandler
    lateinit var serviceHandler:SoundServiceHandler
    lateinit var graphHandler:GraphHandler

    lateinit var textTest:TextView
    lateinit var animalNameText:EditText

     */


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launchWhenStarted {
            viewModel.conversion.collect { event ->
                when(event) {
                    is MainViewModel.SoundEvent.Success -> {
                        binding.progressBar.isVisible = false
                        binding.tvResult.setTextColor(Color.BLACK)
                        binding.tvResult.text = event.resultText
                    }
                    is MainViewModel.SoundEvent.Failure -> {
                        binding.progressBar.isVisible = false
                        binding.tvResult.setTextColor(Color.RED)
                        binding.tvResult.text = event.errorText
                    }
                    is MainViewModel.SoundEvent.Loading -> {
                        binding.progressBar.isVisible = true
                    }
                    else -> Unit
                }
            }
        }


        /*
        animalNameText = findViewById(R.id.textAnimalName)
        textTest = findViewById(R.id.tvResult)
        textTest.text = "DEFAULT"
        serviceHandler = SoundServiceHandler()
        graphHandler = GraphHandler(findViewById(R.id.graphAplitude), findViewById(R.id.graphTime), findViewById(R.id.graphFreqFull))
        recordHandler = RecordHandler(graphHandler, "${externalCacheDir?.absolutePath}/audiorecordtest.3gp")

         */
    }

    /*
    override fun onStart() {
        super.onStart()
        graphHandler.initGraphView()
    }

    override fun onPause() {
        super.onPause()
        recordHandler.stopRecording()
    }

     */


    /*
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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
                R.id.check_sound_time -> {
                    val sound = recordHandler.createDataSound(true, animalNameText)
                    serviceHandler.checkSoundTimeDomain(textTest, sound)
                }
                R.id.check_sound_freq -> {
                    val sound = recordHandler.createDataSound(true, animalNameText)
                    serviceHandler.checkSoundFrequencyDomain(textTest, sound)
                }
                R.id.check_sound_power -> {
                    val sound = recordHandler.createDataSound(true, animalNameText)
                    serviceHandler.checkSoundPowerSpectrum(textTest, sound)
                }
                R.id.get_sounds -> {
                    viewModel.convert(textTest.text.toString())
                    //serviceHandler.getSounds(textTest)
                }
                R.id.get_sound_types-> {
                    serviceHandler.getTypes(textTest)
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

     */

}