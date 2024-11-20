package com.example.snapmedia.server

import android.content.ContentResolver
import android.util.Log
import com.example.snapmedia.externalstorage.loadPhotosFromDirectory
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.File


/**
 * The API that controls the SmartPilot Webpage
 *
 *  Attributes
 *  ----------
 *  @param {contentResolver : ContentResolver}
 *      the contentResolver for the application
 *
 *  Public Methods
 *  --------------
 *  @fun serve(session)
 *      Handles requests to the server
 *  @fun startServer()
 *      Starts the server in the background
 *  @fun stopServer()
 *      Stops the server
 */
class SmartPilotAPI (private val contentResolver: ContentResolver): NanoHTTPD("0.0.0.0",8080), SmartPilotController {

    /**
     * Handles request to the server
     * @param {session: IHTTPSession?}
     *      the session of the request
     * @return {Response}
     *      the server's response to the request
     */
    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri

        Log.d("SmartPilotAPI", "Request $uri")

        return when (uri) {
            "/time" -> serveTime()

            "/page" -> serveImagePage(contentResolver)

            "/images" -> serveImageFile(session)

            else -> newFixedLengthResponse("404 Not Found")
        }
    }

    /**
     * Starts the server in the background, displays a notification to show that server is active
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
     * Stops the server
     */
    fun stopServer() {
        stop()
        println("Server stopped.")
    }
}