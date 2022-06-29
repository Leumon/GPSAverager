package me.muehl.gpsaverager

import kotlin.math.abs

class Coordinates(lat: Double, lon: Double) {

    var lat: Double = 0.0
    var lon: Double = 0.0

    init {
        this.lat = lat
        this.lon = lon
    }

    fun toDDMString() : String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return "$latDir ${toDDM(lat)} $lonDir ${toDDM(lon)}"
    }

    private fun toDDM(degreePart: Double) : String {
        var degrees : Int = abs(degreePart.toInt())
        var minutes : Double = (abs(degreePart) - degrees) * 60
        return "$degrees° ${"%.3f".format(minutes)}"
    }

}