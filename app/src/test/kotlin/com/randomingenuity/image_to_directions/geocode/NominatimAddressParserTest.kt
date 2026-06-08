package com.randomingenuity.image_to_directions.geocode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [NominatimAddressParser].
 */
class NominatimAddressParserTest {

    /**
     * Verifies display_name is returned when present in the Nominatim response.
     */
    @Test
    fun parseAddressFromResponse_returnsDisplayName() {
        val responseBody = """
            {
              "display_name": "6463, Bridgeport Lane, Palm Beach County, Florida, 33463, United States",
              "address": {
                "house_number": "6463",
                "road": "Bridgeport Lane"
              }
            }
        """.trimIndent()

        val address = NominatimAddressParser.parseAddressFromResponse(responseBody)

        assertEquals(
            "6463, Bridgeport Lane, Palm Beach County, Florida, 33463, United States",
            address,
        )
    }

    /**
     * Verifies address components are joined when display_name is missing.
     */
    @Test
    fun parseAddressFromResponse_buildsAddressFromComponents() {
        val responseBody = """
            {
              "address": {
                "house_number": "6463",
                "road": "Bridgeport Lane",
                "county": "Palm Beach County",
                "state": "Florida",
                "postcode": "33463",
                "country": "United States"
              }
            }
        """.trimIndent()

        val address = NominatimAddressParser.parseAddressFromResponse(responseBody)

        assertEquals(
            "6463 Bridgeport Lane, Palm Beach County, Florida, 33463, United States",
            address,
        )
    }

    /**
     * Verifies null is returned when the response contains no usable address fields.
     */
    @Test
    fun parseAddressFromResponse_returnsNullForEmptyResponse() {
        val address = NominatimAddressParser.parseAddressFromResponse("{}")

        assertNull(address)
    }
}
