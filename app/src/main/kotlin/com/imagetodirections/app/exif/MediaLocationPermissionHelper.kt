package com.imagetodirections.app.exif

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Checks whether the app can read photo location metadata from MediaStore URIs.
 */
object MediaLocationPermissionHelper {

    /**
     * Returns whether Android 10+ requires ACCESS_MEDIA_LOCATION for photo GPS EXIF.
     */
    fun isRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * Returns whether the app currently holds ACCESS_MEDIA_LOCATION.
     */
    fun hasPermission(context: Context): Boolean {
        if (!isRequired()) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
