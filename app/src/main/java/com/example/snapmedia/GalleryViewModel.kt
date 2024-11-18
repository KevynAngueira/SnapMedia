package com.example.snapmedia

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.snapmedia.externalstorage.loadPhotosFromExternalStorage
import com.example.snapmedia.externalstorage.savePhotoToExternalStorage
import com.example.snapmedia.externalstorage.savePhotoToDirectory
import com.example.snapmedia.externalstorage.loadPhotosFromDirectory
import java.util.UUID

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val contentResolver: ContentResolver = application.contentResolver
    private val imageDirectory = "snapmedia"

    private val _photos = MutableStateFlow<List<SharedStoragePhoto>>(emptyList())
    val photos = _photos.asStateFlow()

    init {
        loadExternalImages()
    }

    // Load external images from MediaStore
    private fun loadExternalImages() {
        viewModelScope.launch {
            val externalImages = loadPhotosFromDirectory(contentResolver, imageDirectory)
            _photos.value = externalImages
        }
    }

    // Add newly captured image to the list
    fun onTakePhoto(bitmap: Bitmap) {
        val newPhoto: SharedStoragePhoto? = savePhotoToDirectory(contentResolver, imageDirectory, UUID.randomUUID().toString(), bitmap)
        if(newPhoto != null) {
            _photos.value += newPhoto
        }
    }

    // Delete a photo by its URI
    fun deletePhoto(photo: SharedStoragePhoto) {
        viewModelScope.launch {
            val uri: Uri = photo.contentUri
            val deletedRows = contentResolver.delete(uri, null, null )

            if (deletedRows > 0) {
                _photos.value = photos.value.filter { it.id != photo.id }
            }
        }
    }
}