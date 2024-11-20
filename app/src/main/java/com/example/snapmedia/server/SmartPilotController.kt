package com.example.snapmedia.server

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import com.example.snapmedia.SharedStoragePhoto
import com.example.snapmedia.externalstorage.loadPhotosFromDirectory
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
     * Serves the list of image names in the given directory
     * @param {contentResolver : ContentResolver}
     *      the current application contentResolver
     * @param {subdirectory : String}
     *      the directory the image is stored inside
     * @return {Response}
     *      the response containing the list of images
     */
    fun serveImageList(contentResolver: ContentResolver, subdirectory: String): Response {
        // Load images from the "snapmedia" directory using MediaStore
        val images = runBlocking {
            loadPhotosFromDirectory(contentResolver, subdirectory)
        }

        // Extract the display names (or any other identifier) from the list of images
        val imageNames = images.map { it.name }

        // Return the image names as a JSON response
        val jsonResponse = "{\"images\": ${imageNames.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}}"
        return NanoHTTPD.newFixedLengthResponse(jsonResponse)
    }

    /**
     * Serves the image file of the requested image from the given directory
     * @param {session : IHTTPSession}
     *      the current request session, contains the image name query parameters
     * @param {subdirectory : String}
     *      the directory the image is stored inside
     * @return {Response}
     *      the response containing the requested image file
     */
    fun serveImageFile(session: IHTTPSession?, subdirectory: String): Response {
        // Extract image name from the query parameters
        val imageName = session?.queryParameterString

        // Build the full file path to the image
        val filePath = "/storage/emulated/0/Pictures/$subdirectory/$imageName"

        // Create a File object from the path
        val imageFile = File(filePath)

        // Check if the file exists and return the appropriate response
        return if (imageFile.exists()) {
            // Get the MIME type based on the file extension
            val mimeType = getMimeType(filePath)
            newChunkedResponse(Response.Status.OK, mimeType, imageFile.inputStream())
        } else {
            // If the image doesn't exist, return a 404 message
            newFixedLengthResponse("Image not found.")
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
     * Returns the mime type of the given image file
     * @param {filePath : String}
     *      the file path of the given image
     * @return {String}
     *      the mime type of the given image
     */
    private fun getMimeType(filePath: String): String {
        return when {
            filePath.endsWith(".jpg", true) || filePath.endsWith(".jpeg", true) -> "image/jpeg"
            filePath.endsWith(".png", true) -> "image/png"
            filePath.endsWith(".gif", true) -> "image/gif"
            else -> "application/octet-stream"
        }
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