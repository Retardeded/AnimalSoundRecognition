package com.example.animalsoundrecognition.model

data class SoundType(val name:String, val dataSounds:List<DataSound>, val soundTypeParameters: SoundTypeParameters) {
    override fun toString(): String {
        return "SoundType(name=$name, dataSounds=${dataSounds}, soundTypeParameters=$soundTypeParameters)"
    }
}