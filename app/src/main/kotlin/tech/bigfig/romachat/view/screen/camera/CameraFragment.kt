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

package tech.bigfig.romachat.view.screen.camera


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.theartofdev.edmodo.cropper.CropImage
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import tech.bigfig.romachat.R
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.databinding.FragmentCameraBinding
import tech.bigfig.romachat.view.screen.camera.utils.*
import tech.bigfig.romachat.view.screen.main.MainFragmentDirections
import timber.log.Timber
import java.io.File
import javax.inject.Inject


class CameraFragment : Fragment(), EasyPermissions.PermissionCallbacks, CameraHost,
    CameraFragmentHandler {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: CameraViewModel
    private lateinit var binding: FragmentCameraBinding

    private lateinit var textureView: AutoFitTextureView

    private var camera: Camera? = null

    private lateinit var previewSize: Size

    private lateinit var outputFile: File

    private var isFlashSupported = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(CameraViewModel::class.java)

        binding = FragmentCameraBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.handler = this
        binding.lifecycleOwner = this

        textureView = binding.cameraTexture

        //don't show permissions UI by default
        viewModel.hasPermissions.postValue(true)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        outputFile = File(activity?.getExternalFilesDir(null), "pic.jpg")

        camera = Camera.initInstance(
            activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager,
            CameraSettingsStorage(activity!!)
        )

        viewModel.switchCameraSupported.postValue(camera?.isSwitchCameraSupported())
    }

    override fun onResume() {
        super.onResume()

        if (!checkPermission()) return

        startCamera()
    }

    override fun onPause() {
        camera?.close()
        super.onPause()
    }

    private fun startCamera() {
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and startCamera preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onTakePictureClick() {
        Timber.d("onTakePictureClick")
        camera?.takePicture(object : ImageHandler {
            override fun handleImage(image: Image): Runnable {
                Timber.d("handleImage")

                return ImageSaver(image, outputFile)
            }
        })
    }

    override fun onCameraSwitchClick() {
        camera?.apply {
            switchCamera()
            close()
            startCamera()
        }
    }

    override fun onFlashClick() {

    }

    override fun onTurnOnPermissionClick() {
        if (activity != null) {
            activity?.startActivity(
                Intent()
                    .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", activity!!.packageName, null))
            )
        }
    }

    override fun getRotation(): Int {
        return activity?.windowManager?.defaultDisplay?.rotation ?: 0
    }

    private var imageForCrop: Uri? = null
    override fun onCaptured(fromFrontCamera: Boolean) {
        Timber.d("onCaptured callback")

        if (activity != null) {
            val uri = Uri.fromFile(outputFile)
            imageForCrop = uri
            CropImage.activity(uri)
                .setFlipHorizontally(fromFrontCamera)
                .setInitialCropWindowPaddingRatio(0f)
                .start(activity!!, this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {

                when (resultCode) {
                    Activity.RESULT_CANCELED -> Unit

                    Activity.RESULT_OK -> {
                        val result = CropImage.getActivityResult(data)
                        if (result != null) {
                            redirectToNextStep(result.uri)
                        }
                    }

                    CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> {
                        Timber.e("Error while cropping")
                        Toast.makeText(activity!!, R.string.error_media_crop, Toast.LENGTH_LONG).show()
                    }
                }
                imageForCrop = null
            }
        }
    }

    private fun redirectToNextStep(uri: Uri?) {
        if (uri != null) {
            findNavController().navigate(MainFragmentDirections.actionToCameraResultRecipientFragment(uri))
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private fun setUpCameraOutputs(width: Int, height: Int, camera: Camera) {
        try {
            // we want to make sure captured image fits to screen size,
            // so choose the largest one we can get from supported capture sizes
            val captureSize: Size = camera.getCaptureSize()

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            val displayRotation = activity!!.windowManager.defaultDisplay.rotation

            val swappedDimensions = areDimensionsSwapped(camera.getSensorOrientation(), displayRotation)

            val displaySize = Point()
            activity?.windowManager?.defaultDisplay?.getSize(displaySize)

            if (swappedDimensions) {
                previewSize = camera.chooseOptimalSize(
                    height,
                    width,
                    displaySize.y,
                    displaySize.x,
                    captureSize
                )
            } else {
                previewSize = camera.chooseOptimalSize(
                    width,
                    height,
                    displaySize.x,
                    displaySize.y,
                    captureSize
                )
            }

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.width, previewSize.height)
            } else {
                textureView.setAspectRatio(previewSize.height, previewSize.width)
            }

            // Check if the flash is supported.
            isFlashSupported = camera.isFlashSupported()

        } catch (e: CameraAccessException) {
            Timber.e(e.toString())
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
//            ErrorDialog.newInstance(getString(R.string.camera_error))
//                .show(childFragmentManager, FRAGMENT_DIALOG)
        }

    }

    /**
     * Determines if the dimensions are swapped given the phone's current rotation.
     *
     * @param sensorOrientation The current sensor orientation
     * @param displayRotation The current rotation of the display
     * @return true if the dimensions are swapped, false otherwise.
     */
    private fun areDimensionsSwapped(sensorOrientation: Int, displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Timber.e("Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    private fun openCamera(width: Int, height: Int) {
        try {
            camera?.cameraHost = this
            camera?.let {
                setUpCameraOutputs(width, height, it)
                configureTransform(width, height)

                val texture = textureView.surfaceTexture
                texture.setDefaultBufferSize(previewSize.width, previewSize.height)
                it.open(Surface(texture))
            }
        } catch (e: CameraAccessException) {
            Timber.e(e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        activity ?: return
        val rotation = activity!!.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }

        textureView.setTransform(matrix)

        resizePreview(textureView, previewSize.width, previewSize.height)
    }

    private fun resizePreview(view: View, previewWidth: Int, previewHeight: Int) {

        Timber.d("previewWidth = $previewWidth previewHeight = $previewHeight")

        val display = activity!!.windowManager.defaultDisplay
        val widthIsMax = display.width > display.height

        Timber.d("display = ${display.width} ${display.height}")

        val rectDisplay = RectF()
        val rectPreview = RectF()

        // RectF for screen
        rectDisplay.set(0f, 0f, display.width.toFloat(), display.height.toFloat())

        // RectF for preview
        if (widthIsMax) {
            // horizontal
            rectPreview.set(0f, 0f, previewWidth.toFloat(), previewHeight.toFloat())
        } else {
            // vertical
            rectPreview.set(0f, 0f, previewHeight.toFloat(), previewWidth.toFloat())
        }

        val fullScreen = true//to fill all the screen with preview
        val matrix = Matrix()
        if (!fullScreen) {
            matrix.setRectToRect(
                rectPreview, rectDisplay,
                Matrix.ScaleToFit.START
            )
        } else {
            matrix.setRectToRect(
                rectDisplay, rectPreview,
                Matrix.ScaleToFit.START
            )
            matrix.invert(matrix)
        }
        matrix.mapRect(rectPreview)

        view.layoutParams.height = rectPreview.bottom.toInt()
        view.layoutParams.width = rectPreview.right.toInt()
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(REQUEST_CODE_PERMISSION)
    private fun checkPermission(): Boolean {
        if (hasCameraPermission()) {
            viewModel.hasPermissions.postValue(true)
            return true
        } else {
            viewModel.hasPermissions.postValue(false)
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this, getString(R.string.camera_permission_rationale),
                REQUEST_CODE_PERMISSION, Manifest.permission.CAMERA
            )
            return false
        }
    }

    private fun hasCameraPermission() = EasyPermissions.hasPermissions(activity!!, Manifest.permission.CAMERA)

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Timber.d("onPermissionsDenied: $requestCode")
        viewModel.hasPermissions.postValue(false)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Timber.d("onPermissionsGranted: $requestCode")
        viewModel.hasPermissions.postValue(false)
    }

    companion object {

        @JvmStatic
        fun newInstance() = CameraFragment()

        private const val REQUEST_CODE_PERMISSION = 2334
    }
}

interface CameraFragmentHandler {
    fun onTakePictureClick()

    fun onCameraSwitchClick()

    fun onFlashClick()

    fun onTurnOnPermissionClick()
}
