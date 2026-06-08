package com.randomingenuity.image_to_directions.map

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [OpenStreetMapTileMath] Web Mercator conversions.
 */
class OpenStreetMapTileMathTest {

    /**
     * Verifies known GPS coordinates map to the expected tile indices at zoom 14.
     */
    @Test
    fun pixelToTileIndex_mapsKnownCoordinatesAtZoom14() {
        val latitude = 26.5821006
        val longitude = -80.14456769972223
        val zoomLevel = 14

        val pixelX = OpenStreetMapTileMath.longitudeToPixelX(longitude, zoomLevel)
        val pixelY = OpenStreetMapTileMath.latitudeToPixelY(latitude, zoomLevel)

        assertEquals(4544, OpenStreetMapTileMath.pixelToTileIndex(pixelX))
        assertEquals(6936, OpenStreetMapTileMath.pixelToTileIndex(pixelY))
    }

    /**
     * Verifies tile URLs use the standard OpenStreetMap tile server path format.
     */
    @Test
    fun buildTileUrl_usesOpenStreetMapTileServer() {
        val tileUrl = OpenStreetMapTileMath.buildTileUrl(
            zoomLevel = 14,
            tileX = 4544,
            tileY = 6936,
        )

        assertEquals(
            "https://tile.openstreetmap.org/14/4544/6936.png",
            tileUrl,
        )
    }
}
