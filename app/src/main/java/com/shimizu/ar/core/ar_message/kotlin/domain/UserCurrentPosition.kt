package com.shimizu.ar.core.ar_message.kotlin.domain

import kotlin.math.abs

class UserCurrentPosition(longitude: Double, latitude: Double, altitude: Double) {
    val longitude: Double      = longitude
    val latitude: Double       = latitude
    val altitude: Double       = altitude

    fun comparePosition(userNowPosition: UserCurrentPosition):Boolean {
        // 結構誤差あるから経度、緯度10m、高度は3m移動したら
        val longitudeFixedDistance = 0.00011
        val latitudeFixedDistance = 0.00009
        val altitudeFixedDistance = 3.0
        val longitudeDiff = abs(this.longitude - userNowPosition.longitude)
        val latitudeDiff  = abs(this.latitude - userNowPosition.latitude)
        val altitudeDiff  = abs(this.altitude - userNowPosition.altitude)

        if (longitudeDiff > longitudeFixedDistance || latitudeDiff > latitudeFixedDistance || altitudeDiff > altitudeFixedDistance) {
            return true
        }
        return false
    }

}