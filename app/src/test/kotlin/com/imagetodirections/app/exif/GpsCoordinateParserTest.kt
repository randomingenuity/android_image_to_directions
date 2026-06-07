package com.imagetodirections.app.exif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [GpsCoordinateParser] rational-to-decimal conversion.
 */
class GpsCoordinateParserTest {

    /**
     * Verifies positive hemisphere references keep the computed coordinate sign.
     */
    @Test
    fun parseCoordinate_convertsNorthAndEastHemisphere() {
        val latitude = GpsCoordinateParser.parseCoordinate(
            rationalString = "26/1,34/1,55562160/1000000",
            reference = "N",
        )
        val longitude = GpsCoordinateParser.parseCoordinate(
            rationalString = "80/1,8/1,40443719/1000000",
            reference = "E",
        )

        assertEquals(26.5821006, latitude!!, 0.0001)
        assertEquals(80.14456769972223, longitude!!, 0.0001)
    }

    /**
     * Verifies south and west references negate the computed coordinate.
     */
    @Test
    fun parseCoordinate_appliesSouthAndWestHemisphereSigns() {
        val latitude = GpsCoordinateParser.parseCoordinate(
            rationalString = "26/1,34/1,55562160/1000000",
            reference = "S",
        )
        val longitude = GpsCoordinateParser.parseCoordinate(
            rationalString = "80/1,8/1,40443719/1000000",
            reference = "W",
        )

        assertEquals(-26.5821006, latitude!!, 0.0001)
        assertEquals(-80.14456769972223, longitude!!, 0.0001)
    }

    /**
     * Verifies invalid EXIF strings return null instead of throwing.
     */
    @Test
    fun parseCoordinate_returnsNullForInvalidInput() {
        assertNull(GpsCoordinateParser.parseCoordinate(null, "N"))
        assertNull(GpsCoordinateParser.parseCoordinate("26/1,34/1", "N"))
        assertNull(GpsCoordinateParser.parseRational("invalid"))
    }
}
