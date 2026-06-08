package com.randomingenuity.image_to_directions.exif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Unit tests for [ExifTimestampFormatter].
 */
class ExifTimestampFormatterTest {

    /**
     * Verifies EXIF timestamps are formatted with the day of week and readable date.
     */
    @Test
    fun format_includesDayOfWeekAndReadableDate() {
        val expectedFormatter =
            SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        val parsedDate = parser.parse("2026:06:01 20:02:06")
        val expected = expectedFormatter.format(parsedDate!!)

        val actual = ExifTimestampFormatter.format("2026:06:01 20:02:06")

        assertEquals(expected, actual)
    }

    /**
     * Verifies null timestamps stay null.
     */
    @Test
    fun format_returnsNullForNullInput() {
        assertNull(ExifTimestampFormatter.format(null))
    }

    /**
     * Verifies invalid EXIF strings are returned unchanged.
     */
    @Test
    fun format_returnsOriginalStringWhenParsingFails() {
        val invalidTimestamp = "not-a-timestamp"

        assertEquals(invalidTimestamp, ExifTimestampFormatter.format(invalidTimestamp))
    }
}
