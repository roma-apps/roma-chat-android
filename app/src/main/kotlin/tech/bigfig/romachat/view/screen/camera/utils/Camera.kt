/*
 * Copyright (c) 2019 Vasily Kabunov
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>.
 */

package tech.bigfig.romachat.view.screen.camera.utils

import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import java.lang.Long
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

private const val LOG_TAG = "CAMERA"

class Camera constructor(private val cameraManager: CameraManager) {

    private val cameraId: String
    private val characteristics: CameraCharacteristics

    companion object {
        @Volatile
        var instance: Camera? = null
            private set

        fun initInstance(cameraManager: CameraManager): Camera {
            val i = instance
            if (i != null) {
                return i
            }
            return synchronized(this) {
                val created = Camera(cameraManager)
                instance = created
                created
            }
        }

        /**
         * Conversion from screen rotation to JPEG orientation.
         */
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }

    init {
        cameraId = chooseCameraId(cameraManager)
        characteristics = cameraManager.getCameraCharacteristics(cameraId)
    }

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val openLock = Semaphore(1)
    private var isClosed = true

    private val backgroundHelper = BackgroundHelper()

    private var cameraDevice: CameraDevice? = null

    private var surface: Surface? = null
    private var captureSession: CameraCaptureSession? = null

    private var state = State.PREVIEW
    private var imageReader: ImageReader? = null

    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null

    /**
     * Open camera and setup background handler
     */
    fun open(surface: Surface) {

        this.surface = surface

        try {
            if (!openLock.tryAcquire(3L, TimeUnit.SECONDS)) {
                throw IllegalStateException("Camera launch failed")
            }

            if (cameraDevice != null) {
                openLock.release()
                return
            }

            backgroundHelper.startBackgroundThread()

            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHelper.backgroundHandler)
        } catch (e: SecurityException) {

        }
    }

    /**
     * Start camera. Should be called after open() is successful
     */
    fun startCamera(surface: Surface?) {
        this.surface = surface

        // We set up a CaptureRequest.Builder with the output Surface.
        previewRequestBuilder = cameraDevice!!.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        )
        previewRequestBuilder?.addTarget(surface)

        // setup camera session
        val size = characteristics.getCaptureSize(CompareSizesByArea())
        imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
        cameraDevice?.createCaptureSession(
            listOf(surface, imageReader?.surface),
            captureStateCallback,
            backgroundHelper.backgroundHandler
        )
    }

    fun close() {
        try {
            if (openLock.tryAcquire(3, TimeUnit.SECONDS))
                isClosed = true
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            surface?.release()
            surface = null

            imageReader?.close()
            imageReader = null

            backgroundHelper.stopBackgroundThread()

        } catch (e: InterruptedException) {
            Log.e(LOG_TAG, "Error closing camera $e")
        } finally {
            openLock.release()

            cameraHost = null
        }
    }

    /**
     * Set up camera Id from id list
     */
    private fun chooseCameraId(manager: CameraManager): String {
        for (cameraId in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraId)

            //TODO add support for front camera too
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection != null &&
                cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
            ) {
                continue
            }
            return cameraId
        }
        throw IllegalStateException("Could not set Camera Id")
    }

    fun getCaptureSize() = characteristics.getCaptureSize(CompareSizesByArea())

    /**
     * Get sensor orientation.
     * 0, 90, 180, 270.
     */
    fun getSensorOrientation() = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

    fun isFlashSupported() = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

    fun chooseOptimalSize(
        textureViewWidth: Int,
        textureViewHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
        aspectRatio: Size
    ): Size =
        characteristics.chooseOptimalSize(
            textureViewWidth,
            textureViewHeight,
            maxWidth,
            maxHeight,
            aspectRatio
        )

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice?) {
            cameraDevice = camera
            openLock.release()
            isClosed = false

            startCamera(surface)
        }

        override fun onClosed(camera: CameraDevice?) {
            isClosed = true
        }

        override fun onDisconnected(camera: CameraDevice?) {
            openLock.release()
            camera?.close()
            cameraDevice = null
            isClosed = true
        }

        override fun onError(camera: CameraDevice?, error: Int) {
            openLock.release()
            camera?.close()
            cameraDevice = null
            isClosed = true
        }
    }

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(LOG_TAG, "CameraCaptureSession.StateCallback.onConfigureFailed")
        }

        override fun onConfigured(session: CameraCaptureSession) {
            // The camera is already closed
            if (isClosed) return
            // When the session is ready, we startCamera displaying the preview.
            captureSession = session
            startPreview()
        }
    }

    private fun startPreview() {
        try {
            // Auto focus should be continuous for camera preview.
            previewRequestBuilder?.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            // Flash is automatically enabled when necessary.
            setAutoFlash(previewRequestBuilder)

            // Finally, we startCamera displaying the camera preview.
            previewRequest = previewRequestBuilder?.build()
            captureSession?.setRepeatingRequest(
                previewRequestBuilder?.build(),
                captureCallback, backgroundHelper.backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(LOG_TAG, e.toString())
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        private fun process(result: CaptureResult) {
//            Log.d(LOG_TAG, "captureCallback $state")
            when (state) {
                State.PREVIEW -> {
//                    val afState = result.get(CaptureResult.CONTROL_AF_STATE) ?: return
//                    if (afState == preAfState) {
//                        return
//                    }
//                    preAfState = afState
//                    focusListener?.onFocusStateChanged(afState)
                }

                State.WAITING_LOCK -> {
                    capturePicture(result)
                }

                State.WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                    ) {
                        state = State.WAITING_NON_PRECAPTURE
                    }
                }

                State.WAITING_NON_PRECAPTURE -> {

                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = State.TAKEN
                        captureStillPicture()
                    }
                }
                else -> {
                }
            }
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }

    }

    private fun capturePicture(result: CaptureResult) {
        val afState = result.get(CaptureResult.CONTROL_AF_STATE)
        if (afState == null) {
            captureStillPicture()
        } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
            || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
        ) {
            // CONTROL_AE_STATE can be null on some devices
            val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
            if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                state = State.TAKEN
                captureStillPicture()
            } else {
                runPrecaptureSequence()
            }
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in [.captureCallback] from [.lockFocus].
     */
    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder?.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            // Tell #captureCallback to wait for the precapture sequence to be set.
            state = State.WAITING_PRECAPTURE
            captureSession?.capture(
                previewRequestBuilder?.build(), captureCallback,
                backgroundHelper.backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(LOG_TAG, e.toString())
        }

    }

    fun takePicture(handler: ImageHandler) {
        if (cameraDevice == null) {
            throw IllegalStateException("Camera device not ready")
        }
        if (isClosed) return
        imageReader?.setOnImageAvailableListener({ reader ->
            Log.d(LOG_TAG, "onImageAvailable")
            val image = reader.acquireNextImage()
            backgroundHelper.backgroundHandler?.post(handler.handleImage(image))
        }, backgroundHelper.backgroundHandler)

        lockFocus()
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private fun lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            previewRequestBuilder?.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            // Tell #captureCallback to wait for the lock.
            state = State.WAITING_LOCK
            captureSession?.capture(
                previewRequestBuilder?.build(), captureCallback,
                backgroundHelper.backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(LOG_TAG, e.toString())
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder?.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )
            setAutoFlash(previewRequestBuilder)
            captureSession?.capture(
                previewRequestBuilder?.build(), captureCallback,
                backgroundHelper.backgroundHandler
            )
            // After this, the camera will go back to the normal state of preview.
            state = State.PREVIEW
            captureSession?.setRepeatingRequest(
                previewRequest, captureCallback,
                backgroundHelper.backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(LOG_TAG, e.toString())
        }

    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder?) {
        if (isFlashSupported()) {
            requestBuilder?.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * [.captureCallback] from both [.lockFocus].
     */
    private fun captureStillPicture() {

        Log.d(LOG_TAG, "captureStillPicture")

        try {
            cameraDevice ?: return
            val rotation = cameraHost?.getRotation() ?: 0

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = cameraDevice?.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE
            )?.apply {
                addTarget(imageReader?.surface)

                // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
                // We have to take that into account and rotate JPEG properly.
                // For devices with orientation of 90, we return our mapping from ORIENTATIONS.
                // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
                set(
                    CaptureRequest.JPEG_ORIENTATION,
                    (ORIENTATIONS.get(rotation) + getSensorOrientation() + 270) % 360
                )

                // Use the same AE and AF modes as the preview.
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }?.also { setAutoFlash(it) }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    Log.d(LOG_TAG, "onCaptureCompleted")
                    unlockFocus()
                }
            }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                capture(captureBuilder?.build(), captureCallback, null)
            }
        } catch (e: CameraAccessException) {
            Log.e(LOG_TAG, e.toString())
        }
    }

    var cameraHost: CameraHost? = null
}

class BackgroundHelper {
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    var backgroundHandler: Handler? = null

    /**
     * Starts a background thread and its [Handler].
     */
    fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(LOG_TAG, e.toString())
        }
    }
}

interface CameraHost {
    fun getRotation(): Int
}

interface ImageHandler {
    fun handleImage(image: Image): Runnable
}

private enum class State {
    PREVIEW,
    WAITING_LOCK,
    WAITING_PRECAPTURE,
    WAITING_NON_PRECAPTURE,
    TAKEN
}

class CompareSizesByArea : Comparator<Size> {

    // We cast here to ensure the multiplications won't overflow
    override fun compare(lhs: Size, rhs: Size) =
        Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)

}