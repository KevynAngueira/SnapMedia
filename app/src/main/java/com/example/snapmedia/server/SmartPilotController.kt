package com.example.snapmedia.server

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.snapmedia.SharedStoragePhoto
import com.example.snapmedia.externalstorage.loadPhotosFromDirectory
import com.example.snapmedia.externalstorage.loadVideosFromDirectory
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.newChunkedResponse
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * The Controller for the function definitions of each route in the SmartPilotAPI
 *
 * Public Methods
 * --------------
 * @fun serveTime()
 *      Serves the current time in yyyy-MM-dd HH:mm:ss format
 * @fun serveImagePage(contentResolver)
 *      Serves the "image page"
 * @fun serveImageFile(session)
 *      Serves the image file of the requested image
 *
 * Private Methods
 * ---------------
 * @fun getCurrentTime()
 *      Returns the current time in yyyy-MM-dd HH:mm:ss format
 * @fun getMimeType(filePath)
 *      Returns the mime type of the given image file
 */
interface SmartPilotController {

    // ---- Public Methods ----

    /**
     * Serves the current time in yyyy-MM-dd HH:mm:ss format
     * @return {Response}
     *      the response containing the current time
     */
    fun serveTime(): Response {
        val currentTime = getCurrentTime()
        val responseMessage = "{\"message\": \"$currentTime\"}"
        return newFixedLengthResponse(responseMessage)
    }

    /**
     * Serves the list of images in the given directory
     * @param {contentResolver : ContentResolver}
     *      the current application contentResolver
     * @param {subdirectory : String}
     *      the directory the image is stored inside
     * @return {Response}
     *      the response containing the list of images
     */
    fun serveImageList(contentResolver: ContentResolver, subdirectory: String): Response {
        val images = runBlocking {
            loadPhotosFromDirectory(contentResolver, subdirectory)
        }

        val jsonResponse = "{\"images\": ${images.joinToString(prefix = "[", postfix = "]") {
            """
            {
                "id": ${it.id},
                "name": "${it.name}",
                "width": ${it.width},
                "height": ${it.height},
                "contentUri": "${it.contentUri}"
            }
        """.trimIndent()
        }}}"

        return newFixedLengthResponse(jsonResponse)
    }

    /**
     * Serves the image file of the requested image from the given URI
     * @param {contentResolver : ContentResolver}
     *      the current content resolver of the application
     * @param {session : IHTTPSession}
     *      the current request session, contains the uri query parameters
     * @return {Response}
     *      the response containing the requested image file
     */
    fun serveImageFile(contentResolver: ContentResolver, session: IHTTPSession?): Response {
        // Extract image URI from the query parameters (make sure to decode it properly)
        val imageUriString = session?.queryParameterString?.let {
            Uri.decode(it) // Decode URI in case it's URL encoded
        }

        // Check if the image URI is not null or empty
        if (imageUriString.isNullOrEmpty()) {
            return newFixedLengthResponse("Image URI parameter missing or invalid.")
        }

        // Convert the URI string into an actual Uri object
        val imageUri: Uri = Uri.parse(imageUriString)

        try {
            // Check if the image exists
            val mimeType = contentResolver.getType(imageUri) ?: "application/octet-stream"
            val imageStream = contentResolver.openInputStream(imageUri)

            // If the image is found, return it as a chunked response
            return if (imageStream != null) {
                newChunkedResponse(Response.Status.OK, mimeType, imageStream)
            } else {
                // If the image couldn't be opened, return a 404 response
                newFixedLengthResponse("Image not found or cannot be opened.")
            }
        } catch (e: Exception) {
            // If any error occurs (e.g., invalid URI or IO issue), return a 500 error
            return newFixedLengthResponse("Error serving image: ${e.message}")
        }
    }

    /**
     * Serves the list of videos in the given directory
     * @param {contentResolver : ContentResolver}
     *      the current application contentResolver
     * @param {subdirectory : String}
     *      the directory the image is stored inside
     * @return {Response}
     *      the response containing the list of videos
     */
    fun serveVideoList(contentResolver: ContentResolver, subdirectory: String): Response {
        val videos = runBlocking {
            loadVideosFromDirectory(contentResolver, subdirectory)
        }

        val jsonResponse = "{\"videos\": ${videos.joinToString(prefix = "[", postfix = "]") {
            """
            {
                "id": ${it.id},
                "name": "${it.name}",
                "size": ${it.size},
                "contentUri": "${it.contentUri}"
            }
        """.trimIndent()
        }}}"

        return newFixedLengthResponse(jsonResponse)
    }

    /**
     * Serves the video file of the requested image from the given URI
     * @param {contentResolver : ContentResolver}
     *      the current content resolver of the application
     * @param {session : IHTTPSession}
     *      the current request session, contains the uri query parameters
     * @return {Response}
     *      the response containing the requested video file
     */
    fun serveVideoFile(contentResolver: ContentResolver, session: IHTTPSession?): Response {
        // Extract image URI from the query parameters (make sure to decode it properly)
        val videoUriString = session?.queryParameterString?.let {
            Uri.decode(it) // Decode URI in case it's URL encoded
        }

        // Check if the image URI is not null or empty
        if (videoUriString.isNullOrEmpty()) {
            return newFixedLengthResponse("Image URI parameter missing or invalid.")
        }

        // Convert the URI string into an actual Uri object
        val videoUri: Uri = Uri.parse(videoUriString)

        try {
            // Check if the image exists
            val mimeType = contentResolver.getType(videoUri) ?: "application/octet-stream"
            val videoStream = contentResolver.openInputStream(videoUri)

            // If the image is found, return it as a chunked response
            return if (videoStream != null) {
                newChunkedResponse(Response.Status.OK, mimeType, videoStream)
            } else {
                // If the image couldn't be opened, return a 404 response
                newFixedLengthResponse("Image not found or cannot be opened.")
            }
        } catch (e: Exception) {
            // If any error occurs (e.g., invalid URI or IO issue), return a 500 error
            return newFixedLengthResponse("Error serving image: ${e.message}")
        }
    }
    
    /**
     * Serves the requested html page
     * @param {context : Context}
     *      the current application context
     * @param {pageName : String}
     *      the name of the page to load
     * @return {Response}
     *      the response containing the page
     */
    fun servePage(context: Context, pageName: String): Response {

        // Read the HTML template from assets folder
        val htmlTemplate = loadHtmlTemplate(context, pageName)

        // Return the response with the updated HTML
        return newFixedLengthResponse(htmlTemplate)
    }

    // ---- Private Methods ----

    /**
     * Returns the current time in yyyy-MM-dd HH:mm:ss format
     * @return {String}
     *      the current time
     */
    private fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    /**
     * Load the HTML template from the assets folder
     * @param {context : Context}
     *      the application context
     * @param {pageName}
     *      the name of the html file to load
     * @return {String}
     *      the HTML template as a string
     */
    fun loadHtmlTemplate(context: Context, pageName: String): String {
        val assetManager = context.assets
        val inputStream = assetManager.open(pageName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.use { it.readText() }
    }
}