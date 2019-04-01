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

package tech.bigfig.romachat.view.screen.chat


import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import pub.devrel.easypermissions.EasyPermissions
import tech.bigfig.romachat.R
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.databinding.FragmentChatBinding
import tech.bigfig.romachat.view.utils.RetryListener
import javax.inject.Inject


class ChatFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(ChatViewModel::class.java)

        viewModel.account = arguments?.getParcelable(ARG_ACCOUNT) ?: throw IllegalArgumentException("Empty account id")

        viewModel.messageList.observe(this, Observer { messages ->
            if (messages != null) {
                Log.d(LOG_TAG, "showing ${messages.size} messages")
                adapter.setItems(messages)
            }
        })

        viewModel.postMessage.observe(this, Observer { result ->
            Log.d(LOG_TAG, "result $result")
        })

        viewModel.uploadMedia.observe(this, Observer { result ->
            Log.d(LOG_TAG, "uploadMedia $result")
        })

        viewModel.loadData()

        binding = FragmentChatBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.retryListener = object : RetryListener {
            override fun onRetry() {
                viewModel.loadData()
            }
        }

        adapter = ChatAdapter(adapterListener)

        binding.chatMessageList.layoutManager = LinearLayoutManager(context)
        binding.chatMessageList.adapter = adapter
        binding.chatMessageList.isNestedScrollingEnabled = false

        binding.chatMessage.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                viewModel.onSubmitClick(binding.chatMessage.text.toString())
            }
            false
        }

        binding.chatAttach.setOnClickListener { v -> openMediaPicker() }

        return binding.root
    }

    private fun openMediaPicker() {
        if (EasyPermissions.hasPermissions(activity!!, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            startActivityForResult(
                Intent(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*")
                    .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")), REQUEST_CODE_MEDIA_PICK
            )
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.storage_permission_rationale),
                REQUEST_CODE_PERMISSION, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_MEDIA_PICK -> {
                if (resultCode == RESULT_OK && data != null) {
                    viewModel.processMedia(data.data)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private var adapterListener = object : ChatAdapter.ChatAdapterListener {
        override fun onMessageClick(message: MessageViewData) {
        }
    }

    companion object {

        const val ARG_ACCOUNT = "ARG_ACCOUNT"

        @JvmStatic
        fun newInstance(account: ChatAccountEntity) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACCOUNT, account)
                }
            }

        private const val LOG_TAG = "ChatFragment"

        private const val REQUEST_CODE_PERMISSION = 2322
        private const val REQUEST_CODE_MEDIA_PICK = 1991
    }
}
