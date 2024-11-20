package com.example.snapmedia.server

import android.content.ContentResolver
import android.util.Log
import com.example.snapmedia.externalstorage.loadPhotosFromDirectory
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.newChunkedResponse
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse
import kotlinx.coroutines.runBlocking
import java.io.File
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
     * Serves the "image page" displaying all images stored in the "snapmedia" subdirectory
     * @param {contentResolver: ContentResolver}
     *      the contentResolver of the application
     * @return {Response}
     *      the response containing the "image page"
     */
    fun serveImagePage(contentResolver: ContentResolver): Response {
        val images = runBlocking {
            loadPhotosFromDirectory(contentResolver, "snapmedia")
        }

        Log.d("SmartPilotAPI", "Loaded images: $images")

        val htmlContent = StringBuilder()
        htmlContent.append("<html>")
        htmlContent.append("<head><title>Images</title></head>")
        htmlContent.append("<body>")
        htmlContent.append("<h1>Images in snapmedia Directory</h1>")

        if (images.isNotEmpty()) {
            htmlContent.append("<ul>")
            for (image in images) {
                // Use the image name or a unique identifier for the image in the URL
                val imageName = image.name // Use the filename or UUID as the image identifier
                htmlContent.append("<li>")
                // Link to the image using the /images route
                htmlContent.append("<img src=\"/images?$imageName\" alt=\"${image.name}\" width=\"150\" height=\"150\">")
                htmlContent.append("<br><strong>${image.name}</strong>")
                htmlContent.append("</li>")
            }
            htmlContent.append("</ul>")
        } else {
            htmlContent.append("<p>No images found.</p>")
        }

        htmlContent.append("</body>")
        htmlContent.append("</html>")

        return newFixedLengthResponse(htmlContent.toString())
    }

    /**
     * Serves the image file of the requested image
     * @param {session : IHTTPSession}
     *      the current request session, contains the image name query parameters
     * @return {Response}
     *      the response containing the requested image file
     */
    fun serveImageFile(session: IHTTPSession?): Response {
        // Extract image name from the query parameters
        val imageName = session?.queryParameterString

        // Build the full file path to the image
        val filePath = "/storage/emulated/0/Pictures/snapmedia/$imageName"

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


}