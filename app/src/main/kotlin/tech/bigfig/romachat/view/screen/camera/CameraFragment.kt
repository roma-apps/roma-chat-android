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


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tech.bigfig.romachat.databinding.FragmentCameraBinding


class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCameraBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    companion object {

        @JvmStatic
        fun newInstance() = CameraFragment()

        private const val LOG_TAG = "CameraFragment"
    }
}
