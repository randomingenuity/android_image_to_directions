package com.imagetodirections.app.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ShareIntentHandler] MIME type resolution.
 */
class ShareIntentHandlerTest {

    /**
     * Verifies a specific content URI MIME type wins over a wildcard intent MIME type.
     */
    @Test
    fun resolveMimeType_prefersSpecificUriMimeTypeOverWildcardIntent() {
        val mimeType = ShareIntentHandler.resolveMimeType(
            intentMimeType = "image/*",
            uriMimeType = "image/jpeg",
        )

        assertEquals("image/jpeg", mimeType)
    }

    /**
     * Verifies a specific intent MIME type is used when the URI MIME type is wildcard.
     */
    @Test
    fun resolveMimeType_usesSpecificIntentMimeTypeWhenUriIsWildcard() {
        val mimeType = ShareIntentHandler.resolveMimeType(
            intentMimeType = "image/jpeg",
            uriMimeType = "image/*",
        )

        assertEquals("image/jpeg", mimeType)
    }

    /**
     * Verifies wildcard MIME types fall back to whichever value is available.
     */
    @Test
    fun resolveMimeType_returnsWildcardWhenNoSpecificTypeExists() {
        val mimeType = ShareIntentHandler.resolveMimeType(
            intentMimeType = "image/*",
            uriMimeType = null,
        )

        assertEquals("image/*", mimeType)
    }

    /**
     * Verifies image wildcard MIME types are detected.
     */
    @Test
    fun isWildcardMimeType_detectsImageWildcard() {
        assertTrue(ShareIntentHandler.isWildcardMimeType("image/*"))
        assertFalse(ShareIntentHandler.isWildcardMimeType("image/jpeg"))
    }

    /**
     * Verifies non-image URI MIME types are ignored when the share intent is image.
     */
    @Test
    fun resolveMimeType_ignoresNonImageUriMimeType() {
        val mimeType = ShareIntentHandler.resolveMimeType(
            intentMimeType = "image/*",
            uriMimeType = "application/octet-stream",
        )

        assertEquals("image/*", mimeType)
    }

    /**
     * Verifies only image MIME types are treated as image content.
     */
    @Test
    fun isImageMimeType_acceptsOnlyImageFamily() {
        assertTrue(ShareIntentHandler.isImageMimeType("image/jpeg"))
        assertTrue(ShareIntentHandler.isImageMimeType("image/*"))
        assertFalse(ShareIntentHandler.isImageMimeType("application/pdf"))
    }
}
