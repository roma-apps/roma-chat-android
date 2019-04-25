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

import android.content.Context
import android.content.SharedPreferences


data class CameraSettings(val cameraId:String?)

class CameraSettingsStorage(context: Context) {

    private val preferences: SharedPreferences

    init {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    fun save(settings: CameraSettings) {
        preferences.edit()
            .putString(CAMERA_ID, settings.cameraId)
            .apply()
    }

    fun read(): CameraSettings {

        return CameraSettings(
            preferences.getString(CAMERA_ID, "")
        )
    }

    companion object {
        private const val FILE_NAME = "tech.bigfig.romachat.cameraprefs"
        private const val CAMERA_ID = "CAMERA_ID"
    }
}