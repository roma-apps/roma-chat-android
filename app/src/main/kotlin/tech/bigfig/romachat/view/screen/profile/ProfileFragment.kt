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

package tech.bigfig.romachat.view.screen.profile

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
import androidx.navigation.fragment.navArgs
import tech.bigfig.romachat.NavGraphDirections
import tech.bigfig.romachat.R
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.databinding.FragmentProfileBinding
import tech.bigfig.romachat.view.screen.feed.FeedFragment
import tech.bigfig.romachat.view.utils.RetryListener
import timber.log.Timber
import javax.inject.Inject

class ProfileFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ProfileViewModel
    private lateinit var binding: FragmentProfileBinding

    private val navArgs: ProfileFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(ProfileViewModel::class.java)

        viewModel.user.observe(this, Observer { user ->
            if (user != null) {
                Timber.d("showing user profile for $user")

                binding.collapsingToolbar.title = user.displayName
                binding.chat.setOnClickListener {

                    findNavController().navigate(
                        NavGraphDirections.actionGlobalChatFragment(
                            ChatAccountEntity(
                                user.id,
                                user.username,
                                user.localUsername,
                                user.displayName,
                                user.avatarUrl
                            )
                        )
                    )
                }
            }
        })

        viewModel.initData(navArgs.accountId, navArgs.account)

        binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.retryListener = object : RetryListener {
            override fun onRetry() {
                viewModel.loadData()
            }
        }

        (activity as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
        binding.collapsingToolbar.title = " "

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FeedFragment.newInstanceAccount(navArgs.accountId)).commit()
        }

        return binding.root
    }
}
