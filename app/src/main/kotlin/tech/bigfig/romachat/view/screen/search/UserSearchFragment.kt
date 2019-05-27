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

package tech.bigfig.romachat.view.screen.search


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import tech.bigfig.romachat.NavGraphDirections
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.databinding.FragmentUserSearchBinding
import tech.bigfig.romachat.view.utils.RetryListener
import timber.log.Timber
import javax.inject.Inject


class UserSearchFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentUserSearchBinding
    private lateinit var viewModel: UserSearchViewModel
    private lateinit var adapter: UserSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(UserSearchViewModel::class.java)

        viewModel.searchResults.observe(this, Observer { accounts ->
            if (accounts != null) {
                Timber.d("showing ${accounts.size} search results")
                adapter.setItems(accounts)
            }
        })

        viewModel.addUser.observe(this, Observer {
            //TODO update exact row would be better
            adapter.notifyDataSetChanged()
        })

        binding = FragmentUserSearchBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.retryListener = object : RetryListener {
            override fun onRetry() {
                viewModel.doSearch(binding.userSearch.query.toString())
            }
        }

        (activity as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }

        adapter = UserSearchAdapter(adapterListener)

        binding.userSearchResultListView.layoutManager = LinearLayoutManager(context)
        binding.userSearchResultListView.adapter = adapter
        binding.userSearchResultListView.isNestedScrollingEnabled = false

        binding.userSearchBack.setOnClickListener { activity?.onBackPressed() }

        binding.userSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                binding.userSearch.clearFocus()
                viewModel.doSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return true
            }
        })

        return binding.root
    }

    private val adapterListener = object : UserSearchAdapter.UserSearchAdapterListener {
        override fun onUserClick(item: UserSearchResultViewData) {
            findNavController().navigate(NavGraphDirections.actionGlobalProfileFragment(item.account.id))//TODO
        }

        override fun onAddClick(item: UserSearchResultViewData) {
            viewModel.addUser(item)
        }
    }
}
