package com.example.animalsoundrecognition.model

class DataSoundParameters(val typeName:String, val signalEnvelope:List<Double>, val rootMeanSquareEnergy:List<Double>, val zeroCrossingDensity:Double) {
    override fun toString(): String {
        return "Params(zeroCrossingDensity=$zeroCrossingDensity \n" +
                "signalEnvelope=$signalEnvelope \n " +
                "rootMeanSquareEnergy=$rootMeanSquareEnergy)"
    }
}