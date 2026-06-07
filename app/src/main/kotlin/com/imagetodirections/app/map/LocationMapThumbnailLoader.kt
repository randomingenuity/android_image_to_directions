package com.imagetodirections.app.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min

/**
 * Downloads OpenStreetMap tiles for GPS coordinates and composes a cached map thumbnail.
 */
object LocationMapThumbnailLoader {

    private const val TAG = "LocationMapThumbnailLoader"
    private const val MAP_CACHE_FILE_NAME = "location_map.png"
    private const val MAP_IMAGE_WIDTH = 400
    private const val MAP_IMAGE_HEIGHT = 300
    private const val MAP_ZOOM_LEVEL = 14
    private const val USER_AGENT = "ImageToDirections/1.2.0 (Android; com.imagetodirections.app)"

    /**
     * Downloads map tiles, composes a thumbnail, and writes it to the app cache directory.
     *
     * @return Cached PNG file for the map image, or null when tile download or composition fails.
     */
    suspend fun downloadMapImage(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): File? = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, MAP_CACHE_FILE_NAME)

        return@withContext try {
            val mapBitmap = composeMapBitmap(latitude, longitude)
            if (mapBitmap == null) {
                Log.w(TAG, "Map thumbnail composition returned no bitmap")
                return@withContext null
            }

            cacheFile.outputStream().use { outputStream ->
                mapBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            cacheFile
        } catch (exception: IOException) {
            Log.e(TAG, "Failed to download map thumbnail", exception)
            null
        }
    }

    /**
     * Stitches the OpenStreetMap tiles that cover the requested coordinates into one bitmap.
     */
    private fun composeMapBitmap(latitude: Double, longitude: Double): Bitmap? {
        val centerPixelX = OpenStreetMapTileMath.longitudeToPixelX(longitude, MAP_ZOOM_LEVEL)
        val centerPixelY = OpenStreetMapTileMath.latitudeToPixelY(latitude, MAP_ZOOM_LEVEL)
        val topLeftPixelX = centerPixelX - MAP_IMAGE_WIDTH / 2.0
        val topLeftPixelY = centerPixelY - MAP_IMAGE_HEIGHT / 2.0

        val firstTileX = OpenStreetMapTileMath.pixelToTileIndex(topLeftPixelX)
        val lastTileX = OpenStreetMapTileMath.pixelToTileIndex(topLeftPixelX + MAP_IMAGE_WIDTH - 1)
        val firstTileY = OpenStreetMapTileMath.pixelToTileIndex(topLeftPixelY)
        val lastTileY = OpenStreetMapTileMath.pixelToTileIndex(topLeftPixelY + MAP_IMAGE_HEIGHT - 1)

        val outputBitmap = Bitmap.createBitmap(
            MAP_IMAGE_WIDTH,
            MAP_IMAGE_HEIGHT,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(outputBitmap)

        for (tileY in firstTileY..lastTileY) {
            for (tileX in firstTileX..lastTileX) {
                val tileBitmap = downloadTile(MAP_ZOOM_LEVEL, tileX, tileY) ?: return null
                val tileLeftPixel = tileX * OpenStreetMapTileMath.TILE_SIZE_PIXELS
                val tileTopPixel = tileY * OpenStreetMapTileMath.TILE_SIZE_PIXELS
                val destinationLeft = (tileLeftPixel - topLeftPixelX).toFloat()
                val destinationTop = (tileTopPixel - topLeftPixelY).toFloat()

                val destinationRect = Rect(
                    max(0, destinationLeft.toInt()),
                    max(0, destinationTop.toInt()),
                    min(MAP_IMAGE_WIDTH, (destinationLeft + OpenStreetMapTileMath.TILE_SIZE_PIXELS).toInt()),
                    min(MAP_IMAGE_HEIGHT, (destinationTop + OpenStreetMapTileMath.TILE_SIZE_PIXELS).toInt()),
                )
                val sourceLeft = destinationRect.left - destinationLeft.toInt()
                val sourceTop = destinationRect.top - destinationTop.toInt()
                val sourceRect = Rect(
                    sourceLeft,
                    sourceTop,
                    sourceLeft + destinationRect.width(),
                    sourceTop + destinationRect.height(),
                )

                canvas.drawBitmap(tileBitmap, sourceRect, destinationRect, null)
                tileBitmap.recycle()
            }
        }

        drawLocationMarker(canvas, MAP_IMAGE_WIDTH / 2f, MAP_IMAGE_HEIGHT / 2f)

        return outputBitmap
    }

    /**
     * Downloads a single OpenStreetMap tile image.
     */
    private fun downloadTile(zoomLevel: Int, tileX: Int, tileY: Int): Bitmap? {
        val tileUrl = OpenStreetMapTileMath.buildTileUrl(zoomLevel, tileX, tileY)
        val connection = (URL(tileUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
        }

        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Tile download failed for $tileUrl with status ${connection.responseCode}")
                return null
            }

            connection.inputStream.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Failed to download tile $tileUrl", exception)
            null
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Draws a marker at the map center for the requested coordinates.
     */
    private fun drawLocationMarker(canvas: Canvas, centerX: Float, centerY: Float) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.FILL
        }
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        canvas.drawCircle(centerX, centerY, 8f, fillPaint)
        canvas.drawCircle(centerX, centerY, 8f, outlinePaint)
    }
}
