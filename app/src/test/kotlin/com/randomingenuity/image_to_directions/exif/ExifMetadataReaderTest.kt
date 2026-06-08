package com.randomingenuity.image_to_directions.exif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Integration tests for [ExifMetadataReader] using JPEG fixtures from test resources.
 */
class ExifMetadataReaderTest {

    private lateinit var sampleWithGps: File
    private lateinit var sampleTimestampOnly: File
    private lateinit var sampleNoExif: File

    /**
     * Loads JPEG fixture files from the test classpath before each test case.
     */
    @Before
    fun setUp() {
        sampleWithGps = resourceFile("sample_with_gps.jpg")
        sampleTimestampOnly = resourceFile("sample_timestamp_only.jpg")
        sampleNoExif = resourceFile("sample_no_exif.jpg")
    }

    /**
     * Verifies that a real camera JPEG with GPS EXIF returns timestamp and coordinates.
     */
    @Test
    fun readFromFile_extractsTimestampAndGpsCoordinatesFromCameraJpeg() {
        val metadata = ExifMetadataReader.readFromFile(sampleWithGps.absolutePath)

        assertEquals("2026:06:01 20:02:06", metadata.timestamp)
        assertTrue(metadata.hasGps)
        assertEquals(26.5821006, metadata.latitude!!, 0.0000001)
        assertEquals(-80.14456769972223, metadata.longitude!!, 0.0000001)
    }

    /**
     * Verifies that timestamp EXIF is read even when GPS tags are absent.
     */
    @Test
    fun readFromFile_extractsTimestampWhenGpsIsMissing() {
        val metadata = ExifMetadataReader.readFromFile(sampleTimestampOnly.absolutePath)

        assertEquals("2026:06:01 20:02:06", metadata.timestamp)
        assertFalse(metadata.hasGps)
        assertNull(metadata.latitude)
        assertNull(metadata.longitude)
    }

    /**
     * Verifies that a JPEG without EXIF returns an empty metadata result.
     */
    @Test
    fun readFromFile_returnsNoMetadataWhenExifIsAbsent() {
        val metadata = ExifMetadataReader.readFromFile(sampleNoExif.absolutePath)

        assertNull(metadata.timestamp)
        assertFalse(metadata.hasGps)
        assertNull(metadata.latitude)
        assertNull(metadata.longitude)
    }

    /**
     * Resolves a fixture file from the test classpath.
     */
    private fun resourceFile(resourceName: String): File {
        val resourceUrl = checkNotNull(javaClass.classLoader?.getResource(resourceName)) {
            "Missing test resource: $resourceName"
        }

        return File(resourceUrl.toURI())
    }
}
