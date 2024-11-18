package com.example.snapmedia

import android.os.Bundle
import androidx.activity.ComponentActivity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.snapmedia.externalstorage.loadPhotosFromExternalStorage
import com.example.snapmedia.old.SharedPhotoAdapter
import kotlinx.coroutines.launch

import com.example.snapmedia.primitives.hasRequiredPermissions
import com.example.snapmedia.primitives.CAMERAX_PERMISSIONS

class RecyclerMain : ComponentActivity() {

    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>


    private lateinit var recyclerView: RecyclerView
    private lateinit var photosAdapter: SharedPhotoAdapter
    private lateinit var photosList: List<SharedStoragePhoto>

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


        setContentView(R.layout.activity_photos)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        photosAdapter = SharedPhotoAdapter(emptyList()) // Initialize with empty list
        recyclerView.adapter = photosAdapter


        // Load photos from external storage in a coroutine
        lifecycleScope.launch {
            photosList = loadPhotosFromExternalStorage(contentResolver)
            // Update the adapter with the loaded photos
            photosAdapter = SharedPhotoAdapter(photosList)
            recyclerView.adapter = photosAdapter
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
}