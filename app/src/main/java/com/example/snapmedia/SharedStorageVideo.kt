package com.example.snapmedia

import android.graphics.Bitmap
import android.net.Uri

data class SharedStorageVideo(
    val id: Long,
    val name: String,
    val size: Long,
    val contentUri: Uri
)