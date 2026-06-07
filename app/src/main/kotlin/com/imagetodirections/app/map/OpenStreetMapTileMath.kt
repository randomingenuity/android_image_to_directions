package com.imagetodirections.app.map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * Web Mercator tile math for OpenStreetMap raster tiles.
 */
object OpenStreetMapTileMath {

    const val TILE_SIZE_PIXELS = 256

    /**
     * Converts a longitude to a global pixel X coordinate at the given zoom level.
     */
    fun longitudeToPixelX(longitude: Double, zoomLevel: Int): Double {
        val tileCount = 1 shl zoomLevel

        return (longitude + 180.0) / 360.0 * tileCount * TILE_SIZE_PIXELS
    }

    /**
     * Converts a latitude to a global pixel Y coordinate at the given zoom level.
     */
    fun latitudeToPixelY(latitude: Double, zoomLevel: Int): Double {
        val tileCount = 1 shl zoomLevel
        val latitudeRadians = Math.toRadians(latitude)
        val mercatorY = (1.0 - ln(tan(latitudeRadians) + 1.0 / cos(latitudeRadians)) / PI) / 2.0

        return mercatorY * tileCount * TILE_SIZE_PIXELS
    }

    /**
     * Returns the tile index that contains the given global pixel coordinate.
     */
    fun pixelToTileIndex(pixelCoordinate: Double): Int {
        return floor(pixelCoordinate / TILE_SIZE_PIXELS).toInt()
    }

    /**
     * Builds an OpenStreetMap tile URL for the given tile indices.
     */
    fun buildTileUrl(zoomLevel: Int, tileX: Int, tileY: Int): String {
        return "https://tile.openstreetmap.org/$zoomLevel/$tileX/$tileY.png"
    }
}
