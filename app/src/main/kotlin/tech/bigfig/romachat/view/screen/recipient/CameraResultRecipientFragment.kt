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

package tech.bigfig.romachat.view.screen.recipient


import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.picasso.Picasso
import tech.bigfig.romachat.R
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.databinding.FragmentCameraResultRecipientBinding
import timber.log.Timber
import javax.inject.Inject


class CameraResultRecipientFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentCameraResultRecipientBinding
    private lateinit var viewModel: CameraResultRecipientViewModel
    private lateinit var adapter: CameraResultRecipientAdapter

    private val navArgs: CameraResultRecipientFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(CameraResultRecipientViewModel::class.java)

        viewModel.fileUri = navArgs.mediaUri

        viewModel.recipients.observe(this, Observer { recipients ->
            if (recipients != null) {
                Timber.d("showing ${recipients.size} recipients")
                adapter.setItems(recipients)
            }
        })

        viewModel.uploadMedia.observe(this, Observer { result ->
            Timber.d("uploadMedia $result")

            if (result != null) {

                Toast.makeText(activity!!, R.string.chat_send_success, Toast.LENGTH_LONG).show()

                //redirect to camera
                fragmentManager?.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        })

        viewModel.loadData()

        binding = FragmentCameraResultRecipientBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        (activity as AppCompatActivity).let {
            it.setSupportActionBar(binding.toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.setTitle(R.string.camera_recipient_title)
        }
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        adapter = CameraResultRecipientAdapter(adapterListener)

        binding.recipientListView.layoutManager = LinearLayoutManager(context)
        binding.recipientListView.adapter = adapter
        binding.recipientListView.isNestedScrollingEnabled = false

        return binding.root
    }

    private val adapterListener = object : CameraResultRecipientAdapter.CameraResultRecipientAdapterListener {
        override fun onAccountClick(account: ChatAccountEntity) {
            viewModel.selectRecipient(account)
        }
    }
}

@BindingAdapter("app:imageUri")
fun loadImage(view: ImageView, uri: Uri) {
    Picasso.get().load(uri)
        .fit().centerCrop()
        .into(view)
}