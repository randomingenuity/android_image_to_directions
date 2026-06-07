package com.imagetodirections.app.exif

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface

/**
 * Reads timestamp and GPS metadata from JPEG files using AndroidX ExifInterface.
 */
object ExifMetadataReader {

    private const val TAG = "ExifMetadataReader"

    /**
     * Reads EXIF metadata from an image file on disk.
     *
     * @param imagePath Absolute path to a JPEG file.
     * @return Parsed metadata from the file EXIF block.
     */
    fun readFromFile(imagePath: String): ImageMetadata {
        return readExifInterface(ExifInterface(imagePath))
    }

    /**
     * Reads EXIF metadata directly from a content URI stream.
     *
     * @param contentResolver Resolver used to open the selected image URI.
     * @param contentUri URI returned by the document picker.
     * @return Parsed metadata, or null when the stream cannot be read.
     */
    fun readFromUri(contentResolver: ContentResolver, contentUri: Uri): ImageMetadata? {
        return try {
            contentResolver.openInputStream(contentUri)?.use { inputStream ->
                readExifInterface(ExifInterface(inputStream))
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Could not read EXIF from content URI", exception)
            null
        }
    }

    /**
     * Reads EXIF from a cached file and falls back to the original content URI.
     *
     * Some content providers strip GPS data from copied streams. When the cached
     * file has no GPS, this method retries against the original URI.
     *
     * @param imagePath Absolute path to the cached copy of the selected image.
     * @param contentResolver Resolver used to open the selected image URI.
     * @param contentUri URI returned by the document picker.
     * @return Parsed metadata, or null when EXIF cannot be read.
     */
    fun readWithUriFallback(
        imagePath: String,
        contentResolver: ContentResolver,
        contentUri: Uri,
    ): ImageMetadata? {
        return try {
            val metadataFromFile = readFromFile(imagePath)

            // Return immediately when the cached file already contains GPS data.
            if (metadataFromFile.hasGps) {
                return metadataFromFile
            }

            // Retry against the original URI when GPS was stripped from the cache copy.
            val metadataFromUri = readFromUri(contentResolver, contentUri)
            if (metadataFromUri != null && metadataFromUri.hasGps) {
                return metadataFromUri.copy(
                    timestamp = metadataFromFile.timestamp ?: metadataFromUri.timestamp,
                )
            }

            metadataFromFile
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to read EXIF metadata", exception)
            null
        }
    }

    /**
     * Builds [ImageMetadata] from an already-opened [ExifInterface] instance.
     */
    private fun readExifInterface(exifInterface: ExifInterface): ImageMetadata {
        val timestamp = readTimestamp(exifInterface)
        val coordinates = readGpsCoordinates(exifInterface)

        return ImageMetadata(
            timestamp = timestamp,
            latitude = coordinates?.first,
            longitude = coordinates?.second,
            hasGps = coordinates != null,
        )
    }

    /**
     * Reads the best available capture timestamp from EXIF tags.
     */
    private fun readTimestamp(exifInterface: ExifInterface): String? {
        return exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
    }

    /**
     * Reads GPS latitude and longitude from EXIF tags.
     *
     * Parses the raw GPS rationals first because [ExifInterface.getLatLong] converts
     * through [Float] and loses precision present in the original EXIF values.
     */
    @Suppress("DEPRECATION")
    private fun readGpsCoordinates(exifInterface: ExifInterface): Pair<Double, Double>? {
        // Parse full-precision EXIF rationals before using the float-based helper.
        val latitude = GpsCoordinateParser.parseCoordinate(
            rationalString = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
            reference = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF),
        )
        val longitude = GpsCoordinateParser.parseCoordinate(
            rationalString = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
            reference = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF),
        )
        if (latitude != null && longitude != null) {
            return Pair(latitude, longitude)
        }

        // Fall back to the library helper when raw GPS tags are unavailable.
        val latLong = FloatArray(2)
        if (exifInterface.getLatLong(latLong)) {
            return Pair(latLong[0].toDouble(), latLong[1].toDouble())
        }

        return null
    }
}
