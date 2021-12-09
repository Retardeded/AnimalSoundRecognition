package com.example.animalsoundrecognition.main

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animalsoundrecognition.util.DispatcherProvider
import com.example.animalsoundrecognition.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel @ViewModelInject constructor(
    val repository: MainRepository,
    val dispatchers: DispatcherProvider
): ViewModel() {

    sealed class SoundEvent {
        class Success(val resultText: String): SoundEvent()
        class Failure(val errorText: String): SoundEvent()
        object Loading : SoundEvent()
        object Empty : SoundEvent()
    }


    private val _conversion = MutableStateFlow<SoundEvent>(SoundEvent.Empty)
    val conversion: StateFlow<SoundEvent> = _conversion

    fun convert(
        amountStr: String,
    ) {
        val fromAmount = amountStr
        if(fromAmount == null) {
            _conversion.value = SoundEvent.Failure("Not a valid amount")
            return
        }

        viewModelScope.launch(dispatchers.io) {
            _conversion.value = SoundEvent.Loading
            when(val ratesResponse = repository.getSounds()) {
                is Resource.Error -> _conversion.value = SoundEvent.Failure(ratesResponse.message!!)
                is Resource.Success -> {
                    val list = ratesResponse.data!!
                    if(list == null) {
                        _conversion.value = SoundEvent.Failure("Unexpected error")
                    } else {
                        _conversion.value = SoundEvent.Success(
                            "$list"
                        )
                    }
                }
            }
        }
    }
}