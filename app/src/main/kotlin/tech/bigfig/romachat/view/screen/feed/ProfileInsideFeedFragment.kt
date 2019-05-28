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

package tech.bigfig.romachat.view.screen.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import tech.bigfig.romachat.R
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.databinding.FragmentProfileInsideFeedBinding
import tech.bigfig.romachat.view.screen.profile.ProfileViewModel
import tech.bigfig.romachat.view.utils.RetryListener
import timber.log.Timber
import javax.inject.Inject

class ProfileInsideFeedFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ProfileViewModel
    private lateinit var binding: FragmentProfileInsideFeedBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(ProfileViewModel::class.java)

        viewModel.user.observe(this, Observer { user ->
            if (user != null) {
                Timber.d("showing user profile for $user")
            }
        })

        viewModel.initDataForCurrentUser()

        binding = FragmentProfileInsideFeedBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.retryListener = object : RetryListener {
            override fun onRetry() {
                viewModel.loadData()
            }
        }

        if (savedInstanceState == null && childFragmentManager.findFragmentByTag("TAG") == null) {
            childFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                FeedFragment.newInstance(FeedType.ACCOUNT),
                "TAG"
            ).commit()
        }

        return binding.root
    }

    companion object {

        fun newInstance() = ProfileInsideFeedFragment()
    }
}
