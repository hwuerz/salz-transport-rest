package de.hwuerz.transport.rest.pteConnector

import java.util.*

/**
 * A special station with additional walk time to it.
 * @param id The ID of the station.
 * @param walkingTime The time in minutes you need to reach this station.
 */
data class KnownStation(val id: String, val walkingTime: Int = 0) {
    fun getWalkingTimeInMillis() = walkingTime * 60 * 1000
    fun getTimeWhenStationCanBeReached(): Date {
        return Date(System.currentTimeMillis() + getWalkingTimeInMillis())
    }
}

object Station {

    val FRANKFURT_HAUPTBAHNHOF = KnownStation("3000010", 3)

}