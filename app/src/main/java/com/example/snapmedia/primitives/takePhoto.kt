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
import java.util.UUID


/**
 * Takes an image and then runs {onPhotoTaken} function on the resulting image
 * @param {controller} : LifecycleCameraController
 *      the controller for the camera
 * @param {onPhotoTaken} : (Bitmap) -> Unit
 *      function ran on resulting image bitmap
 */
fun takePhoto(
    context: Context,
    contentResolver: ContentResolver,
    controller: LifecycleCameraController,
    onPhotoTaken: (SharedStoragePhoto) -> Unit
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

                val photo: SharedStoragePhoto? = savePhotoToExternalStorage(contentResolver, UUID.randomUUID().toString(), rotatedBitmap)
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