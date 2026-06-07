package com.imagetodirections.app.geocode

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looks up a street address for GPS coordinates using the Nominatim reverse-geocoding API.
 */
object ReverseGeocoder {

    private const val TAG = "ReverseGeocoder"
    private const val USER_AGENT = "ImageToDirections/1.2.0 (Android; com.imagetodirections.app)"
    private const val NOMINATIM_REVERSE_URL =
        "https://nominatim.openstreetmap.org/reverse?format=json&addressdetails=1"

    /**
     * Resolves GPS coordinates to a human-readable address.
     *
     * @return The resolved address, or null when lookup fails.
     */
    suspend fun lookupAddress(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            val reverseGeocodeUrl =
                "$NOMINATIM_REVERSE_URL&lat=$latitude&lon=$longitude"
            val connection = (URL(reverseGeocodeUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            return@withContext try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(
                        TAG,
                        "Reverse geocoding failed with status ${connection.responseCode}",
                    )
                    return@withContext null
                }

                val responseBody = connection.inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }

                NominatimAddressParser.parseAddressFromResponse(responseBody)
            } catch (exception: IOException) {
                Log.e(TAG, "Failed to reverse-geocode coordinates", exception)
                null
            } finally {
                connection.disconnect()
            }
        }
}
