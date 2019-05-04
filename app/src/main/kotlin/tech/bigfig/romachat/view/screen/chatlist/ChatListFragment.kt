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

package tech.bigfig.romachat.view.screen.chatlist


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import tech.bigfig.romachat.R
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.entity.ChatInfo
import tech.bigfig.romachat.databinding.FragmentChatListBinding
import tech.bigfig.romachat.view.screen.main.MainFragmentDirections
import tech.bigfig.romachat.view.utils.RetryListener
import timber.log.Timber
import javax.inject.Inject


class ChatListFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentChatListBinding
    private lateinit var viewModel: ChatListViewModel
    private lateinit var adapter: ChatListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(ChatListViewModel::class.java)

        viewModel.chatList.observe(this, Observer { chats ->
            if (chats != null) {
                Timber.d("showing ${chats.size} chats")
                adapter.setItems(chats)
            }
        })

        viewModel.loadData()

        binding = FragmentChatListBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.retryListener = object : RetryListener {
            override fun onRetry() {
                viewModel.loadData()
            }
        }

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        activity?.setTitle(R.string.chat_list_title)

        adapter = ChatListAdapter(adapterListener)

        binding.chatListView.layoutManager = LinearLayoutManager(context)
        binding.chatListView.adapter = adapter
        binding.chatListView.isNestedScrollingEnabled = false

        return binding.root
    }

    private val adapterListener = object : ChatListAdapter.ChatListAdapterListener {
        override fun onChatClick(chatInfo: ChatInfo) {
            findNavController().navigate(MainFragmentDirections.actionToChatFragment(chatInfo.account))
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = ChatListFragment()
    }
}
