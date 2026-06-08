package com.randomingenuity.image_to_directions.exif

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
     * Reads EXIF metadata directly from a content URI.
     *
     * @param contentResolver Resolver used to open the selected image URI.
     * @param contentUri URI returned by the document picker or share intent.
     * @return Parsed metadata, or null when the stream cannot be read.
     */
    fun readFromUri(contentResolver: ContentResolver, contentUri: Uri): ImageMetadata? {
        return try {
            ExifContentUriOpener.openFileDescriptor(contentResolver, contentUri)
                ?.use { parcelFileDescriptor ->
                    readExifInterface(ExifInterface(parcelFileDescriptor.fileDescriptor))
                }
                ?: ExifContentUriOpener.openInputStream(contentResolver, contentUri)?.use { inputStream ->
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
     * file has no valid GPS, this method retries against the original URI.
     *
     * @param imagePath Absolute path to the cached copy of the selected image.
     * @param contentResolver Resolver used to open the selected image URI.
     * @param contentUri URI returned by the document picker or share intent.
     * @return Parsed metadata, or null when EXIF cannot be read.
     */
    fun readWithUriFallback(
        imagePath: String,
        contentResolver: ContentResolver,
        contentUri: Uri,
    ): ImageMetadata? {
        return try {
            val metadataFromFile = readFromFile(imagePath)
            val metadataFromUri = readFromUri(contentResolver, contentUri)
            val metadataWithGps = listOfNotNull(metadataFromUri, metadataFromFile)
                .firstOrNull { metadata -> metadata.hasValidGps() }

            if (metadataWithGps != null) {
                val timestamp = metadataFromFile.timestamp
                    ?: metadataFromUri?.timestamp
                    ?: metadataWithGps.timestamp

                return metadataWithGps.copy(timestamp = timestamp)
            }

            val metadataWithoutGps = metadataFromFile.withoutInvalidGps()
            if (metadataWithoutGps.timestamp == null && metadataFromUri?.timestamp != null) {
                return metadataWithoutGps.copy(timestamp = metadataFromUri.timestamp)
            }

            metadataWithoutGps
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
     * Clears bogus GPS values while keeping the rest of the metadata intact.
     */
    private fun ImageMetadata.withoutInvalidGps(): ImageMetadata {
        if (!hasGps || hasValidGps()) {
            return this
        }

        return copy(
            latitude = null,
            longitude = null,
            hasGps = false,
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
            if (GpsCoordinateParser.isPlausibleGpsCoordinates(latitude, longitude)) {
                return Pair(latitude, longitude)
            }

            return null
        }

        // Fall back to the library helper when raw GPS tags are unavailable.
        val latLong = FloatArray(2)
        if (exifInterface.getLatLong(latLong)) {
            val latitudeFromHelper = latLong[0].toDouble()
            val longitudeFromHelper = latLong[1].toDouble()
            if (GpsCoordinateParser.isPlausibleGpsCoordinates(latitudeFromHelper, longitudeFromHelper)) {
                return Pair(latitudeFromHelper, longitudeFromHelper)
            }
        }

        return null
    }
}
