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
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size

private const val MAX_PREVIEW_WIDTH = 1920
private const val MAX_PREVIEW_HEIGHT = 1080

fun CameraCharacteristics.getCaptureSize(comparator: Comparator<Size>): Size {
    val map: StreamConfigurationMap =
        get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return Size(0, 0)
    return map.getOutputSizes(ImageFormat.JPEG)
        .asList()
        .maxWith(comparator) ?: Size(0, 0)
}

/**
 * Given `choices` of `Size`s supported by a camera, choose the smallest one that
 * is at least as large as the respective texture view size, and that is at most as large as the
 * respective max size, and whose aspect ratio matches with the specified value. If such size
 * doesn't exist, choose the largest one that is at most as large as the respective max size,
 * and whose aspect ratio matches with the specified value.
 *
 * @param textureViewWidth  The width of the texture view relative to sensor coordinate
 * @param textureViewHeight The height of the texture view relative to sensor coordinate
 * @param maxWidth          The maximum width that can be chosen
 * @param maxHeight         The maximum height that can be chosen
 * @param aspectRatio       The aspect ratio
 * @return The optimal `Size`, or an arbitrary one if none were big enough
 */
fun CameraCharacteristics.chooseOptimalSize(
    textureViewWidth: Int,
    textureViewHeight: Int,
    maxWidth: Int,
    maxHeight: Int,
    aspectRatio: Size
): Size {
    var _maxWidth = maxWidth
    var _maxHeight = maxHeight

    if (_maxWidth > MAX_PREVIEW_WIDTH) {
        _maxWidth = MAX_PREVIEW_WIDTH
    }

    if (_maxHeight > MAX_PREVIEW_HEIGHT) {
        _maxHeight = MAX_PREVIEW_HEIGHT
    }

    val map = get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return Size(0, 0)

    val choices = map.getOutputSizes(SurfaceTexture::class.java)

    // Collect the supported resolutions that are at least as big as the preview Surface
    val bigEnough = ArrayList<Size>()
    // Collect the supported resolutions that are smaller than the preview Surface
    val notBigEnough = ArrayList<Size>()
    val w = aspectRatio.width
    val h = aspectRatio.height
    for (option in choices) {
        if (option.width <= _maxWidth &&
            option.height <= _maxHeight &&
            option.height == option.width * h / w
        ) {
            if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                bigEnough.add(option)
            } else {
                notBigEnough.add(option)
            }
        }
    }
    // Pick the smallest of those big enough. If there is no one big enough, pick the
    // largest of those not big enough.
    return when {
        bigEnough.size > 0 -> bigEnough.asSequence().sortedWith(CompareSizesByArea()).first()
        notBigEnough.size > 0 -> notBigEnough.asSequence().sortedWith(CompareSizesByArea()).last()
        else -> choices[0]
    }
}

fun CameraCharacteristics.isContinuousAutoFocusSupported(): Boolean =
    isSupported(
        CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES,
        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE
    )

fun CameraCharacteristics.isAutoExposureSupported(mode: Int): Boolean =
    isSupported(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, mode)

private fun CameraCharacteristics.isSupported(
    modes: CameraCharacteristics.Key<IntArray>, mode: Int
): Boolean {
    val ints = this.get(modes) ?: return false
    for (value in ints) {
        if (value == mode) {
            return true
        }
    }
    return false
}
