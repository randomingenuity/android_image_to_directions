package com.randomingenuity.image_to_directions.exif

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import java.io.InputStream

/**
 * Opens shared and picked image URIs with unredacted EXIF when possible.
 */
object ExifContentUriOpener {

    private const val TAG = "ExifContentUriOpener"

    /**
     * Opens a readable file descriptor for the original image bytes behind a content URI.
     */
    fun openFileDescriptor(contentResolver: ContentResolver, contentUri: Uri): ParcelFileDescriptor? {
        val originalContentUri = requireOriginalUri(contentUri)

        return try {
            contentResolver.openFileDescriptor(originalContentUri, "r")
        } catch (securityException: SecurityException) {
            Log.w(
                TAG,
                "Could not open original file descriptor; retrying without requireOriginal",
                securityException,
            )

            try {
                contentResolver.openFileDescriptor(contentUri, "r")
            } catch (retryException: SecurityException) {
                Log.w(TAG, "Could not open file descriptor", retryException)
                null
            }
        }
    }

    /**
     * Opens a readable input stream for the original image bytes behind a content URI.
     */
    fun openInputStream(contentResolver: ContentResolver, contentUri: Uri): InputStream? {
        val originalContentUri = requireOriginalUri(contentUri)

        return try {
            contentResolver.openInputStream(originalContentUri)
        } catch (securityException: SecurityException) {
            Log.w(
                TAG,
                "Could not open original input stream; retrying without requireOriginal",
                securityException,
            )
            contentResolver.openInputStream(contentUri)
        }
    }

    /**
     * Requests the original file contents from MediaStore when running on Android 10+.
     */
    fun requireOriginalUri(contentUri: Uri): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return contentUri
        }

        return MediaStore.setRequireOriginal(contentUri)
    }
}
