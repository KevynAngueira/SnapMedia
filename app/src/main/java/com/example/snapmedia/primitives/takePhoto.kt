package com.example.snapmedia.primitives

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import com.example.snapmedia.SharedStoragePhoto
import com.example.snapmedia.externalstorage.savePhotoToExternalStorage
import java.util.UUID


/**
 * Takes an image and then runs {onPhotoTaken} function on the resulting image
 * @param {context: Context}
 *      the application context
 * @param {controller : LifecycleCameraController}
 *      the controller for the camera
 * @param {onPhotoTaken} : (Bitmap) -> Unit
 *      function ran on resulting image bitmap
 */
fun takePhoto(
    context: Context,
    controller: LifecycleCameraController,
    onPhotoTaken: (Bitmap) -> Unit
) {

    // Edge: Handle missing permissions
    if(!hasRequiredPermissions(context)) {
        return
    }

    controller.takePicture(
        ContextCompat.getMainExecutor(context),
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

                onPhotoTaken(rotatedBitmap)
            }
        }
    )
}