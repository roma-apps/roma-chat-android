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
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import timber.log.Timber
import java.lang.Long
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


class Camera constructor(
    private val cameraManager: CameraManager,
    private val cameraSettingsStorage: CameraSettingsStorage
) {

    private var cameraId: String
    private var characteristics: CameraCharacteristics

    companion object {
        @Volatile
        var instance: Camera? = null
            private set

        fun initInstance(cameraManager: CameraManager, cameraSettingsStorage: CameraSettingsStorage): Camera {
            val i = instance
            if (i != null) {
                return i
            }
            return synchronized(this) {
                val created = Camera(cameraManager, cameraSettingsStorage)
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

    private var backCameraId: String? = null
    private var frontCameraId: String? = null
    private val cameraSettings = cameraSettingsStorage.read()

    init {
        cameraId = setupCameraId(cameraManager)
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

    private var flashMode = CaptureRequest.CONTROL_AE_MODE_ON

    fun switchCamera() {
        if (!isSwitchCameraSupported()) return

        cameraId = if (cameraId == backCameraId) frontCameraId!! else backCameraId!!
        cameraSettingsStorage.save(CameraSettings(cameraId))

        characteristics = cameraManager.getCameraCharacteristics(cameraId)
    }

    fun setFlashMode(flashEnabled: Boolean) {
        Timber.d("setFlashMode flashEnabled = $flashEnabled")
        flashMode =
            if (flashEnabled) CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH else CaptureRequest.CONTROL_AE_MODE_ON
    }

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
            Timber.e("Error closing camera $e")
        } finally {
            openLock.release()

            cameraHost = null
        }
    }

    /**
     * Set up camera Id from id list
     */
    private fun setupCameraId(manager: CameraManager): String {
        for (cameraId in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraId)

            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection != null) {
                when (cameraDirection) {
                    CameraCharacteristics.LENS_FACING_BACK -> backCameraId = cameraId
                    CameraCharacteristics.LENS_FACING_FRONT -> frontCameraId = cameraId
                }
            }
        }

        return if (cameraSettings.cameraId == frontCameraId && frontCameraId != null) frontCameraId!!
        else if (cameraSettings.cameraId == backCameraId && backCameraId != null) backCameraId!!
        else throw IllegalStateException("Can't find any camera id")
    }

    fun getCaptureSize() = characteristics.getCaptureSize(CompareSizesByArea())

    /**
     * Get sensor orientation.
     * 0, 90, 180, 270.
     */
    fun getSensorOrientation() = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

    fun isFlashSupported() = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

    fun isSwitchCameraSupported() = frontCameraId != null && backCameraId != null

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
            Timber.e("CameraCaptureSession.StateCallback.onConfigureFailed")
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
            val builder = createPreviewRequestBuilder()
            captureSession?.setRepeatingRequest(
                builder?.build(), captureCallback, backgroundHelper.backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Timber.e(e.toString())
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        private fun process(result: CaptureResult) {
            when (state) {
                State.WAITING_LOCK -> {
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                    // Auto Focus state is not ready in the first place
                    if (afState == null) {
                        runPreCapture()
                    } else if (CaptureResult.CONTROL_AF_STATE_INACTIVE == afState ||
                        CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                        CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                    ) {
                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            captureStillPicture()
                        } else {
                            runPreCapture()
                        }
                    } else {
                        captureStillPicture()
                    }
                }

                State.WAITING_PRECAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null
                        || aeState == CaptureRequest.CONTROL_AE_STATE_PRECAPTURE
                        || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED
                        || aeState == CaptureRequest.CONTROL_AE_STATE_CONVERGED
                    ) {
                        state = State.WAITING_NON_PRECAPTURE
                    }
                }

                State.WAITING_NON_PRECAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureRequest.CONTROL_AE_STATE_PRECAPTURE) {
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

    private fun runPreCapture() {
        try {
            state = State.WAITING_PRECAPTURE
            val builder = createPreviewRequestBuilder()
            builder?.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            captureSession?.capture(builder?.build(), captureCallback, backgroundHelper.backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e("runPreCapture $e")
        }
    }

    fun takePicture(handler: ImageHandler) {
        if (cameraDevice == null) {
            throw IllegalStateException("Camera device not ready")
        }
        if (isClosed) return

        imageReader?.setOnImageAvailableListener({ reader ->
            Timber.d("onImageAvailable")
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
            state = State.WAITING_LOCK

            val builder = createPreviewRequestBuilder()
            if (!characteristics.isContinuousAutoFocusSupported()) {
                // If continuous AF is not supported, start AF here
                builder?.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START
                )
            }

            // Tell #captureCallback to wait for the lock.
            captureSession?.capture(
                builder?.build(), captureCallback,
                backgroundHelper.backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Timber.e(e.toString())
        }
    }

    @Throws(CameraAccessException::class)
    private fun createPreviewRequestBuilder(): CaptureRequest.Builder? {
        val builder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder?.addTarget(surface)
        enableDefaultModes(builder)
        return builder
    }

    private fun enableDefaultModes(builder: CaptureRequest.Builder?) {
        if (builder == null) return

        // Auto focus should be continuous for camera preview.
        // Use the same AE and AF modes as the preview.
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
        if (characteristics.isContinuousAutoFocusSupported()) {
            builder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
        } else {
            builder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_AUTO
            )
        }

        if (characteristics.isAutoExposureSupported(flashMode)) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, flashMode)
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }

        builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY)
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private fun unlockFocus() {
        try {
            val builder = createPreviewRequestBuilder()
            enableDefaultModes(builder)
            if (!characteristics.isContinuousAutoFocusSupported()) {
                // If continuous AF is not supported, start AF here
                builder?.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL
                )
            }
            state = State.PREVIEW
            captureSession?.setRepeatingRequest(
                builder?.build(), captureCallback,
                backgroundHelper.backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Timber.e(e.toString())
        }

    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * [.captureCallback] from both [.lockFocus].
     */
    private fun captureStillPicture() {
        Timber.d("captureStillPicture")

        state = State.TAKEN
        try {
            val rotation = cameraHost?.getRotation() ?: 0

            // This is the CaptureRequest.Builder that we use to take a picture.
            val builder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            enableDefaultModes(builder)

            builder?.apply {
                addTarget(imageReader?.surface)
                addTarget(surface)

                // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
                // We have to take that into account and rotate JPEG properly.
                // For devices with orientation of 90, we return our mapping from ORIENTATIONS.
                // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
                set(
                    CaptureRequest.JPEG_ORIENTATION,
                    (ORIENTATIONS.get(rotation) + getSensorOrientation() + 270) % 360
                )
            }

            captureSession?.apply {
                stopRepeating()
                captureSession?.capture(
                    builder?.build(),
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            // Once still picture is captured, ImageReader.OnImageAvailable gets called
                            unlockFocus()
                            cameraHost?.onCaptured(cameraId == frontCameraId)
                        }
                    },
                    backgroundHelper.backgroundHandler
                )
            }

        } catch (e: CameraAccessException) {
            Timber.e("captureStillPicture $e")
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
            Timber.e(e.toString())
        }
    }
}

interface CameraHost {
    fun getRotation(): Int

    fun onCaptured(fromFrontCamera: Boolean)
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