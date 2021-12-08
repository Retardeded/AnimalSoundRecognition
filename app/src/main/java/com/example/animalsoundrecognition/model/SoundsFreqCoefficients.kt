package com.example.animalsoundrecognition.model

class SoundsFreqCoefficients(val centroidsCoefficient:Double, val fluxesCoefficient:Double,
                             val rollOffPointsCoeficient:Double, val mergedCoefficient:Double) {
    override fun toString(): String {
        return "Coefficients:(\nmergedCoefficient=$mergedCoefficient\n" +
                "centroidsCoefficient=$centroidsCoefficient\n" +
                "fluxesCoefficient=$fluxesCoefficient\n" +
                "rollOffPointsCoeficient=$rollOffPointsCoeficient\n"
    }
}