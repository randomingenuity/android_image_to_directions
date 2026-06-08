package com.randomingenuity.image_to_directions.exif

import java.util.Locale

/**
 * Formats decimal GPS coordinates for display.
 */
object GpsCoordinateFormatter {

    /**
     * Formats a coordinate in decimal degrees with enough precision for map use.
     *
     * @param coordinate Latitude or longitude in decimal degrees.
     * @return Fixed-precision decimal string suitable for on-screen display.
     */
    fun formatCoordinate(coordinate: Double): String {
        return String.format(Locale.US, "%.8f", coordinate)
    }
}
