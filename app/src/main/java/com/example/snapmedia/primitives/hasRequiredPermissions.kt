package com.example.snapmedia.primitives

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Checks if proper permissions have been granted to use CameraX
 * @param {context: Context}
 *      the application context
 * @return {boolean}
 *      True if all permissions granted, false otherwise
 */
fun hasRequiredPermissions(context: Context): Boolean {
    return CAMERAX_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Permission object for CameraX permission checking
 */
val CAMERAX_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
)
