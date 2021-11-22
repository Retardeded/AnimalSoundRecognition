package com.example.animalsoundrecognition.model

class SoundsFreqCoefficients(val powerSpectrumCoefficient:Double, val mergedCoefficient:Double) {
    override fun toString(): String {
        return "Coefficients:(\nmergedCoefficient=$mergedCoefficient\n" +
                "powerSpectrumCoefficient=$powerSpectrumCoefficient\n"
    }
}