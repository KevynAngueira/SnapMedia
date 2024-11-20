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

/**
 * The view model that controls the Image Gallery and image external storage. Displays, saves, loads,
 * and deletes images stored in the "snapmedia" image subdirectory
 *
 * Attributes
 * ----------
 * @param {application : Application}
 *      the current application
 *
 * Public Methods
 * --------------
 * @fun loadExternalImages()
 *      Loads images from the "snapmedia" subdirectory to the gallery
 * @fun onTakePhoto(bitmap)
 *      Saves the photo to "snapmedia" subdirectory and adds the photo to the gallery
 * @fun deletePhoto(photo)
 *      Deletes the chosen photo and removes it from the gallery
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val contentResolver: ContentResolver = application.contentResolver
    private val imageDirectory = "snapmedia"

    private val _photos = MutableStateFlow<List<SharedStoragePhoto>>(emptyList())
    val photos = _photos.asStateFlow()

    init {
        loadExternalImages()
    }

    /**
     * Loads images from the "snapmedia" subdirectory to the gallery
     */
    private fun loadExternalImages() {
        viewModelScope.launch {
            val externalImages = loadPhotosFromDirectory(contentResolver, imageDirectory)
            _photos.value = externalImages
        }
    }

    /**
     * Saves the photo to "snapmedia" subdirectory and adds the photo to the gallery
     * @param {bitmap : Bitmap}
     *      the image to save in bitmap form
     */
    fun onTakePhoto(bitmap: Bitmap) {
        val newPhoto: SharedStoragePhoto? = savePhotoToDirectory(contentResolver, imageDirectory, UUID.randomUUID().toString(), bitmap)
        if(newPhoto != null) {
            _photos.value += newPhoto
        }
    }

    /**
     * Deletes the chosen photo and removes it from the gallery
     * @param {photo : SharedStoragePhoto}
     *      the photo to delete
     */
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