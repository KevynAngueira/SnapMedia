package com.example.snapmedia.primitives

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import java.io.File

private var recording: Recording? = null

/**
 * Records a video from now until this function is called again, and saves the resulting video
 * to external/scoped storage
 * @param {context: Context}
 *      the application context
 * @param {controller : LifecycleCameraController}
 *      the controller for the camera
 */
@SuppressLint("MissingPermission")
fun recordVideo(
    context: Context,
    controller: LifecycleCameraController
) {

    // If recording in process, stop recording
    if(recording != null) {
        recording?.stop()
        recording = null
        return
    }

    // Edge: Handle missing permissions
    if(!hasRequiredPermissions(context)) {
        return
    }

    // If no recording in process, start recording
    val outputFile = File(context.filesDir, "my-recording.mp4")
    recording = controller.startRecording(
        FileOutputOptions.Builder(outputFile).build(),
        AudioConfig.create(true),
        ContextCompat.getMainExecutor(context),
    ) { event ->
        when (event) {
            is VideoRecordEvent.Finalize -> {
                if(event.hasError()) {
                    recording?.close()
                    recording = null

                    Toast.makeText(
                        context,
                        "Video capture failed",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Video capture succeeded",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    }
}