package com.example.snapmedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.max
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.snapmedia.ui.theme.SnapMediaTheme
import kotlinx.coroutines.launch

import com.example.snapmedia.primitives.hasRequiredPermissions
import com.example.snapmedia.primitives.CAMERAX_PERMISSIONS
import com.example.snapmedia.primitives.isRecordingInProgress
import com.example.snapmedia.primitives.takePhoto
import com.example.snapmedia.primitives.recordVideo
import com.example.snapmedia.server.SmartPilotService
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {

    private var recording: Recording? = null
    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    private var cameraController : LifecycleCameraController? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Server Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        // Camera and Video Permissions
        // TODO: Change this for real permission handling
        if (!hasRequiredPermissions(applicationContext)) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }

        // Read and Write external storage permissions
        permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            readPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
            writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted
        }
        updateOrRequestPermissions()

        Log.d("MainActivity2", "Read: $readPermissionGranted")
        Log.d("MainActivity2", "Write: $writePermissionGranted")

        setContent {
            SnapMediaTheme {
                val scope = rememberCoroutineScope()
                val scaffoldState = rememberBottomSheetScaffoldState()
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE
                        )
                    }
                }

                cameraController = controller

                // Track recording state and elapsed time
                var isRecording by remember { mutableStateOf(false) }
                var elapsedTime by remember { mutableStateOf(0) }
                val maxTime = 15

                // Create a LaunchedEffect to update elapsed time while recording
                LaunchedEffect(isRecording) {
                    if (isRecording) {
                        while (isRecording && elapsedTime < maxTime) {
                            delay(1000) // Delay for 1 second
                            elapsedTime++
                        }

                        if (elapsedTime >= maxTime) {
                            isRecording = false
                            elapsedTime = 0
                        }
                    }
                }


                val viewModel = viewModel<GalleryViewModel>()
                val photos by viewModel.photos.collectAsState()

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 0.dp,
                    sheetContent = {
                        GalleryContent(
                            photos = photos,
                            onImageDelete = viewModel::deletePhoto,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        CameraPreview(
                            controller = controller,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Camera switch button
                        IconButton(
                            onClick = {
                                controller.cameraSelector =
                                    if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else CameraSelector.DEFAULT_BACK_CAMERA
                            },
                            modifier = Modifier.offset(16.dp, 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch camera"
                            )
                        }


                        // Recording indicator
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = elapsedTime / (maxTime.toFloat()),
                                    strokeWidth = 8.dp,
                                    modifier = Modifier.size(100.dp)
                                )
                                Text(
                                    text = "$elapsedTime s",
                                    style = MaterialTheme.typography.headlineSmall
                                        .copy(color = Color.White),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }


                        // Bottom button container
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // Open Gallery Button
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        scaffoldState.bottomSheetState.expand()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Photo,
                                    contentDescription = "Open gallery"
                                )
                            }

                            // Take Photo Button
                            IconButton(
                                onClick = {
                                    takePhoto(
                                        context = applicationContext,
                                        controller = controller,
                                        onPhotoTaken = viewModel::onTakePhoto
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Take Photo"
                                )
                            }

                            // Record Video Button
                            IconButton(
                                onClick = {
                                    recordVideo(
                                        context = applicationContext,
                                        controller = controller,
                                        subdirectory = "snapmedia",
                                        timedStop = (maxTime*1000).toLong()
                                    )

                                    isRecording = isRecordingInProgress()
                                    if (!isRecording) {
                                        elapsedTime = 0
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Record Video"
                                )
                            }

                            // Start Server Button
                            IconButton(
                                onClick = {
                                    Intent(applicationContext, SmartPilotService::class.java).also {
                                        it.action = SmartPilotService.Actions.START.toString()
                                        startService(it)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Server",
                                    tint = Color.Green
                                )
                            }

                            // Take Photo Button
                            IconButton(
                                onClick = {
                                    Intent(applicationContext, SmartPilotService::class.java).also {
                                        it.action = SmartPilotService.Actions.STOP.toString()
                                        startService(it)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Server",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    //TODO: Ensure external storage code is working
    /**
     * Updates or requests the necessary permissions to read and write images
     * from External/Scoped storage
     */
    private fun updateOrRequestPermissions() {
        //Check READ permission
        val hasReadPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        //Check WRITE permission
        val hasWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val minSdkWrite = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        // Permission Booleans
        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || minSdkWrite

        // Request missing permissions
        val permissionsToRequest = mutableListOf<String>()
        if(!writePermissionGranted) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(!readPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if(permissionsToRequest.isNotEmpty()){
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /// Release Camera when the app goes to background or is destroyed
    private fun releaseCamera() {
        // Unbind all use cases (this will stop camera preview and other use cases)
        cameraController?.unbind()
    }

    override fun onStop() {
        super.onStop()
        // Release the camera when the activity goes to the background
        releaseCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the camera when the activity is destroyed
        releaseCamera()
    }

    override fun onResume() {
        super.onResume()
        // Reinitialize and rebind the camera when the app comes to the foreground
        cameraController?.bindToLifecycle(
            this
        )
    }
}

