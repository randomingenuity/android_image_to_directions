package com.imagetodirections.app.exif

/**
 * Converts EXIF GPS rational strings into decimal degrees.
 *
 * EXIF stores coordinates as three comma-separated rationals (degrees, minutes, seconds)
 * plus a hemisphere reference letter (N/S/E/W).
 */
internal object GpsCoordinateParser {

    /**
     * Parses a GPS coordinate from EXIF rational and reference strings.
     *
     * @param rationalString DMS value such as "26/1,34/1,55562160/1000000".
     * @param reference Hemisphere reference such as "N", "S", "E", or "W".
     * @return Decimal degrees, or null when the input is missing or invalid.
     */
    fun parseCoordinate(rationalString: String?, reference: String?): Double? {
        // Reject missing EXIF values before attempting conversion.
        if (rationalString == null || reference == null) {
            return null
        }

        // Split degrees, minutes, and seconds rationals.
        val parts = rationalString.split(",")
        if (parts.size != 3) {
            return null
        }

        val degrees = parseRational(parts[0])
        val minutes = parseRational(parts[1])
        val seconds = parseRational(parts[2])
        if (degrees == null || minutes == null || seconds == null) {
            return null
        }

        // Convert DMS to decimal degrees and apply hemisphere sign.
        var coordinate = degrees + (minutes / 60.0) + (seconds / 3600.0)
        if (reference.equals("S", ignoreCase = true) || reference.equals("W", ignoreCase = true)) {
            coordinate = -coordinate
        }

        return coordinate
    }

    /**
     * Parses a single EXIF rational value such as "26/1".
     *
     * @param rational Numerator/denominator pair separated by a slash.
     * @return The floating-point quotient, or null when parsing fails.
     */
    fun parseRational(rational: String): Double? {
        val pair = rational.split("/")
        if (pair.size != 2) {
            return null
        }

        val numerator = pair[0].trim().toDoubleOrNull() ?: return null
        val denominator = pair[1].trim().toDoubleOrNull() ?: return null
        if (denominator == 0.0) {
            return null
        }

        return numerator / denominator
    }
}
