package com.imagetodirections.app.exif

/**
 * EXIF metadata extracted from a JPEG image.
 *
 * @property timestamp Photo capture time from EXIF, or null when unavailable.
 * @property latitude Decimal latitude in degrees, or null when GPS is missing.
 * @property longitude Decimal longitude in degrees, or null when GPS is missing.
 * @property hasGps True when both latitude and longitude were parsed successfully.
 */
data class ImageMetadata(
    val timestamp: String?,
    val latitude: Double?,
    val longitude: Double?,
    val hasGps: Boolean,
) {

    /**
     * Returns whether the metadata contains usable GPS coordinates.
     */
    fun hasValidGps(): Boolean {
        return hasGps &&
            latitude != null &&
            longitude != null &&
            GpsCoordinateParser.isPlausibleGpsCoordinates(latitude, longitude)
    }
}
