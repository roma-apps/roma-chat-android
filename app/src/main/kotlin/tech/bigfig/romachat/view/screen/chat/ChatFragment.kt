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
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.theartofdev.edmodo.cropper.CropImage
import pub.devrel.easypermissions.EasyPermissions
import tech.bigfig.romachat.R
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.data.entity.Attachment
import tech.bigfig.romachat.data.entity.Media
import tech.bigfig.romachat.data.entity.MediaType
import tech.bigfig.romachat.databinding.FragmentChatBinding
import tech.bigfig.romachat.utils.isImageMedia
import tech.bigfig.romachat.view.screen.media.ViewMediaActivity
import tech.bigfig.romachat.view.utils.RetryListener
import timber.log.Timber
import javax.inject.Inject


class ChatFragment : Fragment(), MessageItemDialogFragment.Listener {

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
                Timber.d("showing ${messages.size} messages")
                adapter.setItems(messages)
            }
        })

        viewModel.postMessage.observe(this, Observer { result ->
            Timber.d("result $result")
        })

        viewModel.uploadMedia.observe(this, Observer { result ->
            Timber.d("uploadMedia $result")
        })

        viewModel.deleteMessage.observe(this, Observer { result ->
            Timber.d("deleteMessage $result")
        })

        viewModel.shortError.observe(this, Observer { message ->
            if (activity != null) {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            }
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

        (activity as AppCompatActivity).let {
            it.setSupportActionBar(binding.toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.title = viewModel.account?.displayName
        }
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

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
                    val uri = data.data
                    if (uri != null && isImageMedia(activity!!.contentResolver, uri))
                        startCropActivity(uri)
                    else {
                        viewModel.processMedia(data.data)
                    }
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {

                when (resultCode) {
                    RESULT_CANCELED -> viewModel.processMedia(imageForCrop)

                    RESULT_OK -> {
                        val result = CropImage.getActivityResult(data)
                        if (result != null) {
                            viewModel.processMedia(result.uri)
                        }
                    }

                    CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> {
                        Timber.e("Error while cropping")
                        Toast.makeText(activity!!, R.string.error_media_crop, Toast.LENGTH_LONG).show()
                        viewModel.processMedia(imageForCrop)
                    }
                }
                imageForCrop = null
            }
        }
    }

    private var imageForCrop: Uri? = null
    private fun startCropActivity(uri: Uri) {
        if (activity != null) {
            imageForCrop = uri
            CropImage.activity(uri)
                .setInitialCropWindowPaddingRatio(0f)
                .start(activity!!, this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private var adapterListener = object : ChatAdapter.ChatAdapterListener {
        override fun onMessageClick(message: MessageViewData, view: View) {
            if (message.isMedia && message.attachment != null) {
                val media = Media(
                    message.attachment.url,
                    when (message.attachment.type) {
                        Attachment.Type.IMAGE -> MediaType.IMAGE
                        Attachment.Type.VIDEO,
                        Attachment.Type.GIFV -> MediaType.VIDEO
                        else -> {
                            Timber.d("Unknown media type: ${message.attachment.type}")
                            return
                        }
                    }
                )

                //TODO replace with Navigation component
                val intent = ViewMediaActivity.newIntent(context, media)
                val url = message.attachment.url
                ViewCompat.setTransitionName(view, url)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity!!,
                    view, url
                )
                startActivity(intent, options.toBundle())
            }
        }

        override fun onMessageLongClick(message: MessageViewData) {
            MessageItemDialogFragment.newInstance(message.id).show(childFragmentManager, "dialog")
        }
    }

    override fun onDeleteClick(messageId: String) {
        viewModel.deleteMessage(messageId)
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

        private const val REQUEST_CODE_PERMISSION = 2322
        private const val REQUEST_CODE_MEDIA_PICK = 1991
    }
}
