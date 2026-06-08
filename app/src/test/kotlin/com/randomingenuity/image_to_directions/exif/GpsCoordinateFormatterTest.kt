package com.randomingenuity.image_to_directions.exif

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [GpsCoordinateFormatter].
 */
class GpsCoordinateFormatterTest {

    /**
     * Verifies that coordinates are rendered with stable decimal precision.
     */
    @Test
    fun formatCoordinate_rendersEightDecimalPlaces() {
        assertEquals("26.58210060", GpsCoordinateFormatter.formatCoordinate(26.5821006))
        assertEquals("-80.14456770", GpsCoordinateFormatter.formatCoordinate(-80.14456769972223))
    }
}
