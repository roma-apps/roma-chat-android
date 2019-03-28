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


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import tech.bigfig.romachat.app.App
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

        viewModel.accountId = arguments?.getString(ARG_ACCOUNT_ID) ?: throw IllegalArgumentException("Empty account id")
        viewModel.accountDisplayName = arguments?.getString(ARG_ACCOUNT_DISPLAY_NAME) ?: throw IllegalArgumentException(
            "Empty account display name"
        )

        viewModel.messageList.observe(this, Observer { messages ->
            if (messages != null) {
                Log.d(LOG_TAG, "showing ${messages.size} messages")
                adapter.setItems(messages)
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

        adapter = ChatAdapter(adapterListener)

        binding.chatMessageList.layoutManager = LinearLayoutManager(context)
        binding.chatMessageList.adapter = adapter
        binding.chatMessageList.isNestedScrollingEnabled = false

        return binding.root
    }

    private var adapterListener = object : ChatAdapter.ChatAdapterListener {
        override fun onMessageClick(message: MessageViewData) {
        }
    }

    companion object {

        const val ARG_ACCOUNT_ID = "ARG_ACCOUNT_ID"
        const val ARG_ACCOUNT_DISPLAY_NAME = "ARG_ACCOUNT_DISPLAY_NAME"

        @JvmStatic
        fun newInstance(accountId: String, accountDisplayName: String) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ACCOUNT_ID, accountId)
                    putString(ARG_ACCOUNT_DISPLAY_NAME, accountDisplayName)
                }
            }

        private const val LOG_TAG = "ChatFragment"
    }
}
