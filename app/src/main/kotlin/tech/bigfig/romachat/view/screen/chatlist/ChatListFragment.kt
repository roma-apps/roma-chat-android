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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.Repository
import tech.bigfig.romachat.data.entity.Account
import tech.bigfig.romachat.databinding.FragmentChatListBinding
import tech.bigfig.romachat.view.utils.RetryListener
import javax.inject.Inject


class ChatListFragment : Fragment() {

    @Inject
    lateinit var repository: Repository//TODO add dagger rules to inject directly to viewmodel

    private lateinit var binding: FragmentChatListBinding
    private lateinit var viewModel: ChatListViewModel
    private lateinit var adapter: ChatListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        App.getApplication(activity!!).appComponent.injectChatListFragment(this)

        viewModel = ViewModelProviders.of(this, ChatListViewModel.ModelFactory(activity!!.application, repository))
            .get(ChatListViewModel::class.java)

        viewModel.friendList.observe(this, Observer { accounts ->
            if (accounts.data != null) {
                Log.d(LOG_TAG, "showing ${accounts.data.size} accounts")
                adapter.setItems(accounts.data)
            }
        })

        viewModel.loadData()

        binding = FragmentChatListBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.retryListener = object : RetryListener {
            override fun onRetry() {
                viewModel.loadData()
            }
        }

        adapter = ChatListAdapter(activity!!, adapterListener)

        binding.chatListView.layoutManager = LinearLayoutManager(context)
        binding.chatListView.adapter = adapter
        binding.chatListView.isNestedScrollingEnabled = false

        return binding.root
    }

    private var adapterListener = object : ChatListAdapter.ChatListAdapterListener {
        override fun onChatClick(account: Account) {

        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = ChatListFragment()

        private const val LOG_TAG = "ChatListFragment"
    }
}
