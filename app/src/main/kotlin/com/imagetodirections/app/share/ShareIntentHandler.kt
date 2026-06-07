package com.imagetodirections.app.share

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/**
 * Extracts shared image URIs from [Intent.ACTION_SEND] intents.
 */
object ShareIntentHandler {

    private val JPEG_FILE_SIGNATURE = byteArrayOf(
        0xFF.toByte(),
        0xD8.toByte(),
        0xFF.toByte(),
    )

    /**
     * Returns whether the intent is a single-image share targeted at this app.
     */
    fun isShareImageIntent(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_SEND &&
            intent.type?.startsWith("image/") == true
    }

    /**
     * Reads the shared image URI from EXTRA_STREAM or clip data.
     */
    fun extractSharedImageUri(intent: Intent): Uri? {
        val streamUri = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        )
        if (streamUri != null) {
            return streamUri
        }

        val clipData = intent.clipData ?: return null
        if (clipData.itemCount == 0) {
            return null
        }

        return clipData.getItemAt(0).uri
    }

    /**
     * Resolves the MIME type for a shared image from the content URI, intent, or file bytes.
     */
    fun resolveImageMimeType(
        contentResolver: ContentResolver,
        intent: Intent,
        contentUri: Uri,
    ): String? {
        val resolvedMimeType = resolveMimeType(
            intentMimeType = intent.type,
            uriMimeType = contentResolver.getType(contentUri),
        )
        if (resolvedMimeType != null && !isWildcardMimeType(resolvedMimeType)) {
            return resolvedMimeType
        }

        if (hasJpegFileSignature(contentResolver, contentUri)) {
            return "image/jpeg"
        }

        return resolvedMimeType
    }

    /**
     * Returns whether the shared content is a JPEG image.
     */
    fun isJpegImage(
        contentResolver: ContentResolver,
        intent: Intent,
        contentUri: Uri,
    ): Boolean {
        val mimeType = resolveImageMimeType(contentResolver, intent, contentUri)

        return mimeType == "image/jpeg" || mimeType == "image/jpg"
    }

    /**
     * Picks the most specific image MIME type from the intent and content URI values.
     *
     * Non-image URI types are ignored so generic image share intents still work.
     */
    fun resolveMimeType(intentMimeType: String?, uriMimeType: String?): String? {
        val specificUriMimeType = uriMimeType?.takeIf { mimeType ->
            isImageMimeType(mimeType) && !isWildcardMimeType(mimeType)
        }
        val specificIntentMimeType = intentMimeType?.takeIf { mimeType ->
            isImageMimeType(mimeType) && !isWildcardMimeType(mimeType)
        }
        if (specificUriMimeType != null) {
            return specificUriMimeType
        }
        if (specificIntentMimeType != null) {
            return specificIntentMimeType
        }

        val wildcardUriMimeType = uriMimeType?.takeIf { mimeType ->
            isImageMimeType(mimeType) && isWildcardMimeType(mimeType)
        }
        val wildcardIntentMimeType = intentMimeType?.takeIf { mimeType ->
            isImageMimeType(mimeType) && isWildcardMimeType(mimeType)
        }

        return wildcardUriMimeType ?: wildcardIntentMimeType
    }

    /**
     * Returns whether the MIME type belongs to the image family.
     */
    fun isImageMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }

    /**
     * Returns whether the MIME type is a wildcard such as image with any subtype.
     */
    fun isWildcardMimeType(mimeType: String): Boolean {
        return mimeType.endsWith("/*")
    }

    /**
     * Detects JPEG files from their file signature when MIME metadata is generic.
     */
    private fun hasJpegFileSignature(contentResolver: ContentResolver, contentUri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(contentUri)?.use { inputStream ->
                val header = ByteArray(JPEG_FILE_SIGNATURE.size)
                val bytesRead = inputStream.read(header)

                bytesRead == JPEG_FILE_SIGNATURE.size &&
                    header.contentEquals(JPEG_FILE_SIGNATURE)
            } ?: false
        } catch (exception: Exception) {
            false
        }
    }
}
