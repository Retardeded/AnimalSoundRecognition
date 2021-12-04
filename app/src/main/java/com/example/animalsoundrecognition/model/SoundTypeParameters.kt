package com.example.animalsoundrecognition.model

class SoundTypeParameters(val typeName:String, val signalEnvelope:List<Integer>, val rootMeanSquareEnergy:List<Integer>, val zeroCrossingDensity:Integer) {
    override fun toString(): String {
        return "Params(zeroCrossingDensity=$zeroCrossingDensity \n" +
                "signalEnvelope=$signalEnvelope \n " +
                "rootMeanSquareEnergy=$rootMeanSquareEnergy)"
    }
}