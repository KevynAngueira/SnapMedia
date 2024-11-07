package com.example.snapmedia

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.snapmedia.externalstorage.loadPhotosFromExternalStorage
import com.example.snapmedia.externalstorage.savePhotoToExternalStorage
import java.util.UUID

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val contentResolver: ContentResolver = application.contentResolver

    private val _photos = MutableStateFlow<List<SharedStoragePhoto>>(emptyList())
    val photos = _photos.asStateFlow()

    init {
        loadExternalImages()
    }

    // Load external images from MediaStore
    private fun loadExternalImages() {
        viewModelScope.launch {
            val externalImages = loadPhotosFromExternalStorage(contentResolver)
            _photos.value = externalImages
        }
    }

    // Add newly captured image to the list
    fun onTakePhoto(bitmap: Bitmap) {
        val newPhoto: SharedStoragePhoto? = savePhotoToExternalStorage(contentResolver, UUID.randomUUID().toString(), bitmap)
        if(newPhoto != null) {
            _photos.value += newPhoto
        }
    }
}