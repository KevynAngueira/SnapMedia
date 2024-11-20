package com.example.snapmedia.externalstorage

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import com.example.snapmedia.SharedStoragePhoto
import com.example.snapmedia.sdk29AndUp
import java.io.IOException

/**
 * Saves a given image in bitmap form to external subdirectory
 * @param {contentResolver : ContentResolver}
 *      the application content resolver
 * @param {displayName : String}
 *      the name to save the photo under
 * @param {bmp : Bitmap}
 *      the bitmap of the image to be saved
 * @return {SharedStoragePhoto}
 *      the saved image
 */
fun savePhotoToDirectory(
    contentResolver: ContentResolver,
    subdirectory: String,
    displayName: String,
    bmp: Bitmap
): SharedStoragePhoto? {
    // Get MediaStore URI dependent on build version
    val imageCollection = sdk29AndUp {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    // Create image Metadata
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        put(MediaStore.Images.Media.WIDTH, bmp.width)
        put(MediaStore.Images.Media.HEIGHT, bmp.height)

        // Specify the subdirectory under Pictures (e.g., /Pictures/MyApp)
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$subdirectory")  // Subdirectory
    }

    return try {
        // Store image Metadata at URI
        val uri = contentResolver.insert(imageCollection, contentValues)
        uri?.let{
            // Store image Bitmap through an output stream
            contentResolver.openOutputStream(uri).use { outputStream ->
                outputStream?.let {
                    if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        //if(!outputStream?.let { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }!!) {
                        throw IOException("Couldn't save bitmap")
                    }
                } ?: throw IOException("Null output stream")
            }

            val id = uri.lastPathSegment?.toLongOrNull() ?: -1L
            SharedStoragePhoto(
                id = id,
                name = displayName,
                width = bmp.width,
                height = bmp.height,
                contentUri = uri
            )

        } ?: throw IOException("Couldn't create MediaStore entry")

    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}