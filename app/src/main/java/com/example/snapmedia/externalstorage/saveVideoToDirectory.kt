package com.example.snapmedia.externalstorage

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import com.example.snapmedia.SharedStoragePhoto
import com.example.snapmedia.SharedStorageVideo
import com.example.snapmedia.sdk29AndUp
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Saves a given video to external subdirectory
 * @param {contentResolver : ContentResolver}
 *      the application content resolver
 * @param {subdirectory : String}
 *      the subdirectory to save the video under
 * @param {fileName : String}
 *      the name to save the video under
 * @param {videoFile : File}
 *      the video file to be saved
 * @return {Uri?}
 *      the URI of the saved video or null if failed
 */
fun saveVideoToDirectory(
    contentResolver: ContentResolver,
    subdirectory: String,
    displayName: String,
    videoFile: File
): SharedStorageVideo? {
    // Get MediaStore URI dependent on build version
    val videoCollection = sdk29AndUp {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } ?: MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    // Create video metadata for insertion
    val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "$displayName.mp4") // Video filename (without extension)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4") // MIME type for MP4 video
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/$subdirectory") // Save in the Movies folder under specified subdirectory
        put(MediaStore.Video.Media.SIZE, videoFile.length()) // Set video file size
    }

    return try {
        // Insert the video entry into MediaStore
        val uri = contentResolver.insert(videoCollection, contentValues)

        uri?.let { destinationUri ->
            // Copy the video file to the MediaStore location using an OutputStream
            contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                videoFile.inputStream().use { inputStream ->
                    copyStream(inputStream, outputStream) // Efficiently copy the video data
                }
            } ?: throw IOException("Failed to open output stream")

            // Return a SharedStorageVideo object with the URI and metadata
            val id = uri.lastPathSegment?.toLongOrNull() ?: -1L
            SharedStorageVideo(
                id = id,
                name = displayName,
                size = videoFile.length(),
                contentUri = uri
            )

        } ?: throw IOException("Failed to insert video into MediaStore")

    } catch (e: Exception) {
        e.printStackTrace() // Log the error for debugging
        null
    }
}

/**
 * Copies data from the InputStream to the OutputStream
 * @param inputStream The source InputStream (video file)
 * @param outputStream The destination OutputStream (MediaStore location)
 */
private fun copyStream(inputStream: InputStream, outputStream: OutputStream) {
    val buffer = ByteArray(1024)
    var bytesRead: Int
    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        outputStream.write(buffer, 0, bytesRead)
    }
    outputStream.flush()
}
