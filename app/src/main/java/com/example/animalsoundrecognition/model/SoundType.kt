package com.example.animalsoundrecognition.model

class SoundType(val name:String, val dataSounds:List<DataSound>, val dataSoundParameters: DataSoundParameters) {
    override fun toString(): String {
        return "SoundType(name=$name, dataSounds=$dataSounds, dataSoundParameters=$dataSoundParameters)"
    }
}