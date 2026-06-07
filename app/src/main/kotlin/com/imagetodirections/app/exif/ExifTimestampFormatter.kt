package com.imagetodirections.app.exif

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Converts EXIF timestamp strings into human-readable text.
 */
object ExifTimestampFormatter {

    private val exifTimestampParser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    private val displayTimestampFormatter =
        SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())

    /**
     * Formats an EXIF timestamp such as "2026:06:01 20:02:06".
     *
     * @param exifTimestamp Raw EXIF date/time string, or null when unavailable.
     * @return A readable string including the day of week, or null when input is null.
     */
    fun format(exifTimestamp: String?): String? {
        if (exifTimestamp == null) {
            return null
        }

        return try {
            val parsedDate: Date = exifTimestampParser.parse(exifTimestamp)
                ?: return exifTimestamp
            displayTimestampFormatter.format(parsedDate)
        } catch (exception: ParseException) {
            exifTimestamp
        }
    }
}
