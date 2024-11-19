package com.example.snapmedia.server

import android.content.ContentResolver
import android.os.Environment
import android.util.Log
import com.example.snapmedia.externalstorage.loadPhotosFromDirectory
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmartPilotAPI (private val contentResolver: ContentResolver): NanoHTTPD("0.0.0.0",8080) {

    /**
     * Handles request to the webserver
     * @param {session: IHTTPSession?}
     *      the session of the request
     * @return {Response}
     *      the webserver's response to the request
     */
    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri
        var responseMessage = "Hello from the Android REST API!"

        Log.d("SmartPilotAPI", "Request $uri")

        return when (uri) {
            "/api/time" -> {
                val time = getCurrentTime()
                responseMessage = "{\"message\": \"$time\"}"
                newFixedLengthResponse(responseMessage)
            }

            "/api/data" -> {
                val images = runBlocking {
                    loadPhotosFromDirectory(contentResolver, "snapmedia")
                }
                Log.d("Loaded Images", images.toString())
                responseMessage = "{\"message\": \"$images\"}"
                Log.d("Loaded Images", responseMessage.toString())
                newFixedLengthResponse(responseMessage)
            }

            else -> newFixedLengthResponse("404 Not Found")
        }
    }

    /**
     * Starts the webserver in the background, displays a notification to show that server is active
     */
    fun startServer() {
        try {
            start(SOCKET_READ_TIMEOUT, false) // starts the server
            println("Server started on port 8080")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stops the webserver
     */
    fun stopServer() {
        stop()
        println("Server stopped.")
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
}