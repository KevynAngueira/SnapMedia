package com.example.snapmedia.primitives

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import com.example.snapmedia.externalstorage.saveVideoToDirectory
import java.io.File
import java.io.IOException

private var recording: Recording? = null
private var outputFile: File? = null

/**
 * Records a video from now until this function is called again, and saves the resulting video
 * to external/scoped storage
 * @param {context: Context}
 *      the application context
 * @param {controller : LifecycleCameraController}
 *      the controller for the camera
 */
fun recordVideo(
    context: Context,
    controller: LifecycleCameraController,
    subdirectory: String
) {

    // Check if recording in progress, stop and save recording if so
    if(stopRecording()) {
        return
    }

    // Check if app has required permissions for video capture
    if (!hasRequiredPermissions(context)) {
        return
    }

    // If no recording in process, start recording
    startRecording(context, controller, subdirectory)
}

/**
 * Records a video from now until a set length, and saves the resulting video to external/scoped storage
 * @param {context: Context}
 *      the application context
 * @param {controller: LifecycleCameraController}
 *      the controller for the camera
 * @param {timedStop: Long}
 *      the length of the video in milliseconds
 */
fun recordVideo(
    context: Context,
    controller: LifecycleCameraController,
    subdirectory: String,
    timedStop: Long
) {

    // Check if recording in progress, stop and save recording if so
    if(stopRecording()) {
        return
    }

    // Check if app has required permissions for video capture
    if (!hasRequiredPermissions(context)) {
        return
    }

    // If no recording in process, start recording
    startRecording(context, controller, subdirectory)

    // Stop the recording after {timedStop} milliseconds
    Handler(Looper.getMainLooper()).postDelayed({
        stopRecording()
    }, timedStop)
}

fun isRecordingInProgress() : Boolean {
    return (recording != null)
}

/**
 * If recording in progress stops the recording and returns True, False otherwise
 * @return {Boolean}
 *      True if a recording was stopped, False otherwise
 */
private fun stopRecording() : Boolean {
    if (isRecordingInProgress()) {
        recording?.stop()
        recording = null
        return true
    }
    return false
}

/**
 * Starts a video recording
 * @param {context: Context}
 *      the application context
 * @param {controller: LifecycleCameraController}
 *      the camera controller
 */
@SuppressLint("MissingPermission")
private fun startRecording(
    context: Context,
    controller: LifecycleCameraController,
    subdirectory: String
) {
    // Determine where to save the video file based on the Android version
    val outputDir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.cacheDir // Use cache directory for Android 10 and above
    } else {
        File(context.getExternalFilesDir(null), "Movies") // Use external storage for older versions
    }

    // Ensure the output directory exists
    if (!outputDir.exists()) {
        outputDir.mkdirs() // Create the directory if it doesn't exist
    }

    // Create a unique file name using the current timestamp
    outputFile = File(outputDir, "temp-recording-${System.currentTimeMillis()}.mp4")

    // Set up the file output options
    val outputOptions = FileOutputOptions.Builder(outputFile!!)
        .build()


    recording = controller.startRecording(
        outputOptions,
        AudioConfig.create(true),
        ContextCompat.getMainExecutor(context)
    ) { event ->

        when (event) {
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    recording?.close()
                    recording = null

                    Toast.makeText(
                        context,
                        "Video capture failed",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    saveRecordedVideo(context, outputFile, subdirectory)

                }
            }
        }

    }
}

/**
 * Saves the recorded video to the specified subdirectory in external storage.
 * @param {context: Context} The application context
 * @param {outputFile: File} The temporary file where the video is stored
 * @param {subdirectory: String} The subdirectory where the video should be saved
 */
private fun saveRecordedVideo(
    context: Context,
    outputFile: File?,
    subdirectory: String
) {

    outputFile?.let {
        val contentResolver = context.contentResolver
        val fileName = "video-${System.currentTimeMillis()}" // Unique filename

        // Save the video file to the specified subdirectory in external storage
        val uri = saveVideoToDirectory(contentResolver, subdirectory, fileName, outputFile)

        if (uri != null) {
            Toast.makeText(context, "Video saved to $uri", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to save video", Toast.LENGTH_LONG).show()
        }
    } ?: throw IOException("Video capture failed")
}