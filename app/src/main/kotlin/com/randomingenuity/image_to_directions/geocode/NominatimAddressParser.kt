package com.randomingenuity.image_to_directions.geocode

import org.json.JSONObject

/**
 * Extracts a human-readable address from a Nominatim reverse-geocoding JSON response.
 */
object NominatimAddressParser {

    /**
     * Parses the address string from a Nominatim reverse-geocoding response body.
     *
     * @return The display address, or null when the response does not contain one.
     */
    fun parseAddressFromResponse(responseBody: String): String? {
        val jsonObject = JSONObject(responseBody)
        val displayName = jsonObject.optString("display_name")
        if (displayName.isNotEmpty()) {
            return displayName
        }

        val addressObject = jsonObject.optJSONObject("address") ?: return null

        return buildAddressFromComponents(addressObject)
    }

    /**
     * Builds an address from Nominatim address components when display_name is absent.
     */
    private fun buildAddressFromComponents(addressObject: JSONObject): String? {
        val addressParts = listOf(
            joinHouseNumberAndRoad(addressObject),
            addressObject.optString("city"),
            addressObject.optString("town"),
            addressObject.optString("village"),
            addressObject.optString("county"),
            addressObject.optString("state"),
            addressObject.optString("postcode"),
            addressObject.optString("country"),
        ).filter { addressPart -> addressPart.isNotEmpty() }

        if (addressParts.isEmpty()) {
            return null
        }

        return addressParts.joinToString(separator = ", ")
    }

    /**
     * Combines house number and road when both are present in the address object.
     */
    private fun joinHouseNumberAndRoad(addressObject: JSONObject): String {
        val houseNumber = addressObject.optString("house_number")
        val road = addressObject.optString("road")

        return when {
            houseNumber.isNotEmpty() && road.isNotEmpty() -> "$houseNumber $road"
            road.isNotEmpty() -> road
            else -> houseNumber
        }
    }
}
