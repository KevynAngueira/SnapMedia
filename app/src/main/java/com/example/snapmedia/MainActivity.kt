package com.example.snapmedia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.snapmedia.ui.theme.SnapMediaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import com.example.snapmedia.primitives.hasRequiredPermissions
import com.example.snapmedia.primitives.CAMERAX_PERMISSIONS
import com.example.snapmedia.primitives.takePhoto

class MainActivity : ComponentActivity() {
    //TODO
    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var recyclerView: RecyclerView
    private lateinit var photosAdapter: SharedPhotoAdapter
    private lateinit var photosList: List<SharedStoragePhoto>

    private var recording: Recording? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get permissions
        // TODO: Change this for real permission handling
        if (!hasRequiredPermissions(applicationContext)) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }

        //externalStoragePhotoAdapter = SharedPhotoAdapter { }
        permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            readPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
            writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted
        }
        updateOrRequestPermissions()

        /*
        setContentView(R.layout.activity_photos)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        photosAdapter = SharedPhotoAdapter(emptyList()) // Initialize with empty list
        recyclerView.adapter = photosAdapter


        // Load photos from external storage in a coroutine
        lifecycleScope.launch {
            photosList = loadPhotosFromExternalStorage()
            // Update the adapter with the loaded photos
            photosAdapter = SharedPhotoAdapter(photosList)
            recyclerView.adapter = photosAdapter
        }
        */

        ///*
        setContent{
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
                val viewModel = viewModel<GalleryViewModel>()
                val photos by viewModel.photos.collectAsState()

                // Creating Scaffold to house gallery
                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetPeekHeight = 0.dp,
                    sheetContent = {
                        GalleryContent(
                            photos = photos,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // CameraPreview: Shows what the camera sees (Specified inn its own file)
                        CameraPreview(
                            controller = controller,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Button to flip camera between front and back
                        IconButton(
                            onClick = {
                                controller.cameraSelector =
                                    if(controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
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

                        // Bottom button container, contains: Gallery, Take Photo
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
                                        contentResolver = contentResolver,
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
                                    recordVideo(controller)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Record Video"
                                )
                            }
                        }

                    }
                }
            }
        }
         //*/
    }

    /*
    //TODO: Find a way to organize files more clearly
    //TODO: Add visual and audio indicator of taking photo
    /**
     * Takes an image and then runs {onPhotoTaken} function on the resulting image
     * @param {controller} : LifecycleCameraController
     *      the controller for the camera
     * @param {onPhotoTaken} : (Bitmap) -> Unit
     *      function ran on resulting image bitmap
     */
    private fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (SharedStoragePhoto) -> Unit
    ) {

        // Edge: Handle missing permissions
        if(!hasRequiredPermissions()) {
            return
        }

        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object: OnImageCapturedCallback() {
                // Handling image capture success
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    // Rotate image to correct orientation
                    val matrix = Matrix().apply{
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                        //postScale(-1f, 1f) // Flip camera
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    val photo: SharedStoragePhoto? = savePhotoToExternalStorage(UUID.randomUUID().toString(), rotatedBitmap)
                    if (photo != null) {
                        onPhotoTaken(photo)
                    }
                }

                // Handling image capture failure
                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                }
            }
        )
    }

     */

    //TODO: Find a way to organize files more clearly
    //TODO: Add visual and audio indicator of recording video start and stop
    /**
     * Records a video from now until this function is called again, and saves the resulting video
     * to external/scoped storage
     * @param {controller} : LifecycleCameraController
     *      the controller for the camera
     */
    @SuppressLint("MissingPermission")
    private fun recordVideo(controller: LifecycleCameraController) {

        // If recording in process, stop recording
        if(recording != null) {
            recording?.stop()
            recording = null
            return
        }

        // Edge: Handle missing permissions
        if(!hasRequiredPermissions(applicationContext)) {
            return
        }

        // If no recording in process, start recording
        val outputFile = File(filesDir, "my-recording.mp4")
        recording = controller.startRecording(
            FileOutputOptions.Builder(outputFile).build(),
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(applicationContext),
        ) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    if(event.hasError()) {
                        recording?.close()
                        recording = null

                        Toast.makeText(
                            applicationContext,
                            "Video capture failed",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Video capture succeeded",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        }
    }

    /*
    /**
     * Checks if proper permissions have been granted to use CameraX
     * @return {boolean}
     *      True if all permissions granted, false otherwise
     */
    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Permission object for CameraX permission checking
     */
    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }


     */

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

    /*
    /**
     * Saves a given image in bitmap form to External/Scoped storage
     * @param {controller} : LifeCycleCameraController
     *      the Camera controller
     * @param {onPhotoTaken} : Function
     *      The function to be performed on the image
     */
    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap): SharedStoragePhoto? {
        // Get MediaStore URI dependent on build version
        val imageCollection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        // Create image Metadata
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }

        return try {
            // Store image Metadata at URI
            val uri = contentResolver.insert(imageCollection, contentValues)
            uri?.let{
                // Store image Bitmap through an output stream
                contentResolver.openOutputStream(uri).use { outputStream ->
                    outputStream?.let {
                        if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                            //if(!outputStream?.let { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }!!) {
                            throw IOException("Couldn't save bitmap")
                        }
                    } ?: throw IOException("Null output stream")
                }
            } ?: throw IOException("Couldn't create MediaStore entry")

            val id = uri.lastPathSegment?.toLongOrNull() ?: -1L
            SharedStoragePhoto(
                id = id,
                name = displayName,
                width = bmp.width,
                height = bmp.height,
                contentUri = uri
            )

        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
     */

    private suspend fun loadPhotosFromExternalStorage(): List<SharedStoragePhoto> {
        return withContext(Dispatchers.IO) {
            val collection = sdk29AndUp {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
            )
            val photos = mutableListOf<SharedStoragePhoto>()
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while(cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    photos.add(SharedStoragePhoto(id, displayName, width, height, contentUri))
                }
                photos.toList()
            } ?: listOf()
        }
    }

    /*
    val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        val isPrivate = false
        val isSavedSuccessfully = bmp?.let {
            if (writePermissionGranted) {
                savePhotoToExternalStorage(UUID.randomUUID().toString(), it)
            } else false
        } ?: false

        if(isPrivate) {
            //loadPhotosFromInternalStorageIntoRecyclerView()
        }
        if(isSavedSuccessfully) {
            Toast.makeText(this, "Photo saved successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show()
        }

    }
     */
}