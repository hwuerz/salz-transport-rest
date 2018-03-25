package de.hwuerz.transport.rest.pteConnector

import de.schildbach.pte.NetworkProvider
import de.schildbach.pte.NvvProvider
import de.schildbach.pte.dto.*
import java.util.*
import java.util.stream.Collectors

/**
 * Only for manual quick and dirty tests.
 */
fun main(args: Array<String>) {
    val lookup = Lookup()
    println(lookup.suggestLocation("Frankfurt Hauptbahnhof"))
}

class Lookup(private val provider: NetworkProvider) {

    /**
     * Generates a new lookup with the NVV Provider
     */
    constructor() : this(NvvProvider())

    /**
     * Find near stations.
     * @param location The location object of which near locations should be fetched.
     * @return Near stations.
     */
    fun nearby(location: Location): NearbyLocationsResult {
        val result = provider.queryNearbyLocations(EnumSet.of(LocationType.STATION), location, 0, 5)
        return result
    }

    /**
     * Get the next departures of the passed station.
     * @param stationId The ID of the station of which the departures should be fetched. Example: "3000010"
     * @return The next five departures.
     */
    fun departures(stationId: String): QueryDeparturesResult {
        val date = Date()
        return provider.queryDepartures(stationId, date, 5, false)!!
    }

    /**
     * Get a trip from one of the `stationsFrom` to one of the `stationsTo`
     * @param stationsFrom All possible start stations.
     * @param stationsTo All possible end stations.
     * @return A trip from one start station to one end station or null if none was found.
     */
    fun trips(stationsFrom: List<KnownStation>, stationsTo: List<KnownStation>): Trip? {

        return stationsFrom
                // Build one list with all pairs of all possible combinations from start and end.
                .flatMap { stationFrom ->
                    stationsTo.map { stationTo -> Pair(stationFrom, stationTo) }
                }

                // Use Java8 parallel streams to increase query time.
                .parallelStream()

                // Request a trip for all pairs (Connector Lookup).
                .map { pair ->
                    val trip = trips(pair.first.id, pair.second.id, pair.first.getTimeWhenStationCanBeReached())
                    Triple(pair.first, pair.second, trip)
                }

                // Find the first reachable trip for each lookup.
                .map { triple ->
                    val from = triple.first
                    val queryTripsResult = triple.third
                    val firstReachableTrip = queryTripsResult.trips
                            // This trip has some public transport
                            .filter { trip -> trip.firstPublicLegDepartureTime != null }
                            // This trip can be reached
                            .filter { trip ->
                                trip.firstPublicLegDepartureTime!!.after(from.getTimeWhenStationCanBeReached())
                            }
                            // Take the earliest one
                            .minBy { trip ->
                                trip.firstPublicLegDepartureTime!!.time
                            }
                    Triple(triple.first, triple.second, firstReachableTrip)
                }

                // Filter all trips which have no route (trips did not contain any reachable trip).
                .filter { triple -> triple.third != null }
                .map { triple ->  Triple(triple.first, triple.second, triple.third!!) }

                // Map back to a normal list. This allows the usage of kotlin's minBy method .
                .collect(Collectors.toList())

                // Find the trip which reaches the destination first.
                .minBy { triple ->
                    val tripArrivalTime = triple.third.lastPublicLegArrivalTime?.time
                    val finalArrivalTime = tripArrivalTime?.plus(triple.second.getWalkingTimeInMillis())
                    finalArrivalTime ?: Long.MAX_VALUE
                }

                // return the trip inside the triple
                ?.third
    }

    /**
     * Queries a trip from `stationFrom` to `stationTo`
     * @param stationFrom The start station.
     * @param stationTo The end station.
     * @param date The departure date (optional. Default: Now)
     * @return The trips between the stations.
     */
    private fun trips(stationFrom: String, stationTo: String, date: Date = Date()): QueryTripsResult {
        val from = Location(LocationType.STATION, stationFrom)
        val to = Location(LocationType.STATION, stationTo)

        return provider.queryTrips(from, null, to, date, true, Product.ALL, null,
                NetworkProvider.WalkSpeed.FAST, NetworkProvider.Accessibility.NEUTRAL, null)!!
    }

    /**
     * Suggest a location based on its name.
     * @param location The name of the station.
     * @return The location.
     */
    fun suggestLocation(location: String): Location {
        return provider.suggestLocations(location).locations.first()
    }
}
