package com.example.snapmedia.externalstorage

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import com.example.snapmedia.SharedStorageVideo
import com.example.snapmedia.sdk29AndUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Loads all videos from external subdirectory
 * @param {contentResolver : ContentResolver}
 *      the application content resolver
 * @param {subdirectory : String}
 *      the subdirectory to load videos from
 * @return {List<SharedStoragePhoto>}
 *      the list of videos
 */
suspend fun loadVideosFromDirectory(
    contentResolver: ContentResolver,
    subdirectory: String
): List<SharedStorageVideo> {

    return withContext(Dispatchers.IO) {
        val collection = sdk29AndUp {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } ?: MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        // Filter to specified columns
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
        )

        // Filter to videos from specified subdirectory
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Movies/$subdirectory/%")

        val videos = mutableListOf<SharedStorageVideo>()
        val cursor: Cursor? = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while(cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                val size = cursor.getLong(sizeColumn)
                val duration = cursor.getLong(durationColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                videos.add(SharedStorageVideo(id, displayName, size, contentUri))
            }
            videos.toList()
        } ?: listOf()
    }
}
