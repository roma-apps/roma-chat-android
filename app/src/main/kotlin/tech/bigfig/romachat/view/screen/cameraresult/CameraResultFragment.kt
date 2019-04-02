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

package tech.bigfig.romachat.view.screen.cameraresult


import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.squareup.picasso.Picasso
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.databinding.FragmentCameraResultBinding
import javax.inject.Inject


class CameraResultFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentCameraResultBinding
    private lateinit var viewModel: CameraResultViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(CameraResultViewModel::class.java)

        viewModel.fileUri = arguments?.getParcelable(ARG_FILE_URI) ?: throw IllegalArgumentException("Empty uri")

        binding = FragmentCameraResultBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.listener = object : ChatResultFragmentListener {
            override fun onNext() {
                redirectToSelectContact()
            }
        }

        return binding.root
    }

    private fun redirectToSelectContact() {

    }

    companion object {

        const val ARG_FILE_URI = "ARG_FILE_URI"

        @JvmStatic
        fun newInstance(uri: Uri) =
            CameraResultFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FILE_URI, uri)
                }
            }

        private const val LOG_TAG = "CameraResultFragment"
    }
}

interface ChatResultFragmentListener {
    fun onNext()
}

@BindingAdapter("app:imageUri")
fun loadImage(view: ImageView, uri: Uri) {
    Picasso.get().load(uri)
        .fit().centerCrop()
        .into(view)
}