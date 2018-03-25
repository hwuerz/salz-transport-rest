package de.hwuerz.transport.rest

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.hwuerz.transport.rest.pteConnector.Lookup
import de.schildbach.pte.dto.Product


class Start : RequestHandler<Any, Start.ResponseClass> {

    private val lookup = Lookup()

    data class ResponseClass(val meta: ResponseMeta, val data: ResponseData)

    data class ResponseMeta(val message: String = "Success", val code: Int = 200, val detail: String? = null)

    interface ResponseData
    data class ResponseDataEmpty(val data: String? = null) : ResponseData
    data class ResponseDataDepartures(val departures: List<ResponseDataDeparture>) : ResponseData

    data class ResponseDataDeparture(val startStation: String?,
                                     val plannedTime: Long?,
                                     val predictedTime: Long?,
                                     val product: Product?,
                                     val label: String?,
                                     val destinationPlace: String?,
                                     val destinationName: String?) : ResponseData

    override fun handleRequest(input: Any, context: Context): ResponseClass  {

        return try {
            val jsonInput = JsonParser().parse(input.toString()).asJsonObject
            invokeMethod(jsonInput)
        } catch (e: Exception) {
            ResponseClass(ResponseMeta("Unknown error", 500, e.toString()), ResponseDataEmpty())
        }

    }

    private fun invokeMethod(jsonObject: JsonObject): ResponseClass {
        if (jsonObject.has("station")) {
            val stationId = jsonObject.getAsJsonPrimitive("station").asString
            val responseData = ResponseDataDepartures(lookup
                    .departures(stationId)
                    .stationDepartures
                    .flatMap { stationDeparture ->
                        stationDeparture.departures.map { departure -> Pair(stationDeparture, departure) }
                    }
                    .map { publicTransport -> ResponseDataDeparture(
                            publicTransport.first.location.id, // 3000010 ID of the start station
                            publicTransport.second.plannedTime?.time, // Real departure time
                            publicTransport.second.predictedTime?.time, // Real departure time
                            publicTransport.second.line.product, // BUS
                            publicTransport.second.line.label, // 0
                            publicTransport.second.destination?.place, // Frankfurt
                            publicTransport.second.destination?.name) // Hauptbahnhof
                    })

            return ResponseClass(ResponseMeta(), responseData)
        } else return ResponseClass(
                ResponseMeta("Error", 422, "The parameter 'station' is missing"),
                ResponseDataEmpty())

    }

}