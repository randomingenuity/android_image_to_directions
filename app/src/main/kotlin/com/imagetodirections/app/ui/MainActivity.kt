package com.imagetodirections.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.imagetodirections.app.R
import com.imagetodirections.app.databinding.ActivityMainBinding
import com.imagetodirections.app.exif.ExifMetadataReader
import com.imagetodirections.app.exif.ExifTimestampFormatter
import com.imagetodirections.app.exif.GpsCoordinateFormatter
import com.imagetodirections.app.exif.ImageMetadata
import com.imagetodirections.app.geocode.ReverseGeocoder
import com.imagetodirections.app.map.LocationMapThumbnailLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

/**
 * Main screen for selecting a JPEG image and displaying its EXIF metadata.
 *
 * The user can browse for an image, view timestamp and GPS coordinates when present,
 * see a reverse-geocoded address, preview a map thumbnail of the location, and open
 * the location in a maps application.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Latitude from the most recently selected image, used for the maps intent. */
    private var selectedLatitude: Double? = null

    /** Longitude from the most recently selected image, used for the maps intent. */
    private var selectedLongitude: Double? = null

    /** Cached map image file shown in the thumbnail and image viewer. */
    private var locationMapFile: File? = null

    /** In-flight map download job, cancelled when a new image is selected. */
    private var mapThumbnailJob: Job? = null

    /** In-flight reverse-geocoding job, cancelled when a new image is selected. */
    private var reverseGeocodeJob: Job? = null

    /** Resolved address from the most recent reverse-geocoding lookup. */
    private var resolvedAddress: String? = null

    /**
     * Receives the URI chosen by the system document picker.
     */
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { contentUri ->
        if (contentUri == null) {
            return@registerForActivityResult
        }
        processSelectedImage(contentUri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Launch the JPEG picker when the user taps Browse.
        binding.browseButton.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/jpeg"))
        }

        // Open the last parsed coordinates in a maps app.
        binding.openMapsButton.setOnClickListener {
            openSelectedLocationInMaps()
        }

        // Open the downloaded map image in an external image viewer.
        binding.locationMapThumbnail.setOnClickListener {
            openLocationMapInImageViewer()
        }

        // Copy the resolved address when the user taps it.
        binding.addressValue.setOnClickListener {
            copyResolvedAddressToClipboard()
        }
    }

    /**
     * Copies the selected image, parses EXIF metadata, and updates the screen.
     */
    private fun processSelectedImage(contentUri: Uri) {
        grantReadPermission(contentUri)

        // Copy the original file bytes into app cache so EXIF can be read reliably.
        val imageFile = copyImageToCache(contentUri)
        if (imageFile == null) {
            Toast.makeText(
                this,
                getString(R.string.error_reading_image),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        // Parse timestamp and GPS, retrying against the original URI when needed.
        val metadata = ExifMetadataReader.readWithUriFallback(
            imagePath = imageFile.absolutePath,
            contentResolver = contentResolver,
            contentUri = contentUri,
        )
        if (metadata == null) {
            Toast.makeText(
                this,
                getString(R.string.error_parsing_image),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        if (!metadata.hasGps) {
            Log.w(TAG, "No GPS found in EXIF for ${imageFile.absolutePath}")
        }

        displayMetadata(metadata)
    }

    /**
     * Keeps read access to the selected document across future image picks.
     */
    private fun grantReadPermission(contentUri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (exception: SecurityException) {
            Log.w(TAG, "Could not take persistable URI permission", exception)
        }
    }

    /**
     * Writes the selected image to a stable cache file path.
     *
     * @return The cached JPEG file, or null when the content URI cannot be copied.
     */
    private fun copyImageToCache(contentUri: Uri): File? {
        return try {
            val cacheFile = File(cacheDir, "selected_image.jpg")
            val parcelFileDescriptor = contentResolver.openFileDescriptor(contentUri, "r")
                ?: return null

            // Copy through a file descriptor to preserve the original JPEG bytes.
            parcelFileDescriptor.use { fileDescriptor ->
                FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                    cacheFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            cacheFile
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to copy selected image", exception)
            null
        }
    }

    /**
     * Shows parsed metadata and toggles GPS-specific UI elements.
     */
    private fun displayMetadata(metadata: ImageMetadata) {
        binding.resultsContainer.visibility = View.VISIBLE

        val formattedTimestamp = ExifTimestampFormatter.format(metadata.timestamp)
        binding.timestampValue.text = formattedTimestamp
            ?: getString(R.string.timestamp_not_available)

        if (metadata.hasGps && metadata.latitude != null && metadata.longitude != null) {
            // Show coordinates and enable the maps button.
            selectedLatitude = metadata.latitude
            selectedLongitude = metadata.longitude

            binding.latitudeLabel.visibility = View.VISIBLE
            binding.latitudeValue.visibility = View.VISIBLE
            binding.longitudeLabel.visibility = View.VISIBLE
            binding.longitudeValue.visibility = View.VISIBLE
            binding.openMapsButton.visibility = View.VISIBLE
            binding.noGpsMessage.visibility = View.GONE

            binding.latitudeValue.text = GpsCoordinateFormatter.formatCoordinate(metadata.latitude)
            binding.longitudeValue.text = GpsCoordinateFormatter.formatCoordinate(metadata.longitude)

            loadLocationAddress(metadata.latitude, metadata.longitude)
            loadLocationMapThumbnail(metadata.latitude, metadata.longitude)
        } else {
            // Hide coordinate fields and show the no-GPS message instead.
            selectedLatitude = null
            selectedLongitude = null
            locationMapFile = null
            resolvedAddress = null
            mapThumbnailJob?.cancel()
            reverseGeocodeJob?.cancel()

            binding.latitudeLabel.visibility = View.GONE
            binding.latitudeValue.visibility = View.GONE
            binding.longitudeLabel.visibility = View.GONE
            binding.longitudeValue.visibility = View.GONE
            binding.addressLabel.visibility = View.GONE
            binding.addressValue.visibility = View.GONE
            binding.openMapsButton.visibility = View.GONE
            binding.locationMapThumbnail.visibility = View.GONE
            binding.noGpsMessage.visibility = View.VISIBLE
        }
    }

    /**
     * Reverse-geocodes the selected coordinates and displays the resolved address.
     */
    private fun loadLocationAddress(latitude: Double, longitude: Double) {
        reverseGeocodeJob?.cancel()
        resolvedAddress = null
        binding.addressLabel.visibility = View.VISIBLE
        binding.addressValue.visibility = View.VISIBLE
        binding.addressValue.text = getString(R.string.address_lookup_in_progress)

        reverseGeocodeJob = lifecycleScope.launch {
            val address = ReverseGeocoder.lookupAddress(
                latitude = latitude,
                longitude = longitude,
            )

            resolvedAddress = address
            binding.addressValue.text = address
                ?: getString(R.string.timestamp_not_available)
        }
    }

    /**
     * Copies the resolved address to the system clipboard.
     */
    private fun copyResolvedAddressToClipboard() {
        val address = resolvedAddress ?: return

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("address", address))

        Toast.makeText(
            this,
            getString(R.string.address_copied_to_clipboard),
            Toast.LENGTH_SHORT,
        ).show()
    }

    /**
     * Downloads and displays a static map thumbnail for the selected coordinates.
     */
    private fun loadLocationMapThumbnail(latitude: Double, longitude: Double) {
        mapThumbnailJob?.cancel()
        binding.locationMapThumbnail.visibility = View.VISIBLE
        binding.locationMapThumbnail.setImageDrawable(null)

        mapThumbnailJob = lifecycleScope.launch {
            val downloadedMapFile = LocationMapThumbnailLoader.downloadMapImage(
                context = applicationContext,
                latitude = latitude,
                longitude = longitude,
            )

            if (downloadedMapFile == null) {
                binding.locationMapThumbnail.visibility = View.GONE
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.error_loading_map_thumbnail),
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }

            locationMapFile = downloadedMapFile
            val bitmap = BitmapFactory.decodeFile(downloadedMapFile.absolutePath)
            binding.locationMapThumbnail.setImageBitmap(bitmap)
        }
    }

    /**
     * Opens the cached map thumbnail in an external image viewer application.
     */
    private fun openLocationMapInImageViewer() {
        val mapFile = locationMapFile
        if (mapFile == null || !mapFile.exists()) {
            return
        }

        val contentUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            mapFile,
        )
        val viewerIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(viewerIntent)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(
                this,
                getString(R.string.error_no_image_viewer),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    /**
     * Launches a maps application for the currently selected coordinates.
     */
    private fun openSelectedLocationInMaps() {
        val latitude = selectedLatitude
        val longitude = selectedLongitude
        if (latitude == null || longitude == null) {
            return
        }

        val mapsUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        val mapsIntent = Intent(Intent.ACTION_VIEW, mapsUri)

        try {
            startActivity(mapsIntent)
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(
                this,
                getString(R.string.error_no_maps_app),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
