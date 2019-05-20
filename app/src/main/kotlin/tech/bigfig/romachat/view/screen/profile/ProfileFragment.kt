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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import tech.bigfig.romachat.NavGraphDirections
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.databinding.FragmentProfileBinding
import tech.bigfig.romachat.view.screen.search.UserSearchResultViewData
import tech.bigfig.romachat.view.utils.RetryListener
import timber.log.Timber
import javax.inject.Inject

class ProfileFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ProfileViewModel
    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(ProfileViewModel::class.java)

        val userId = arguments?.getString(ARG_USER_ID)
        val searchResult = arguments?.getParcelable<UserSearchResultViewData>(ARG_SEARCH_RESULT)

        viewModel.user.observe(this, Observer { user ->
            if (user != null) {
                Timber.d("showing user profile for $user")
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
                    dismiss()
                }
            }
        })

        viewModel.initData(userId, searchResult)

        binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.retryListener = object : RetryListener {
            override fun onRetry() {
                viewModel.loadData()
            }
        }

        return binding.root
    }

    companion object {

        fun newInstance(userId: String): ProfileFragment =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                }
            }

        fun newInstanceFromSearch(searchResult: UserSearchResultViewData): ProfileFragment =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SEARCH_RESULT, searchResult)
                }
            }

        private const val ARG_USER_ID = "ARG_USER_ID"
        private const val ARG_SEARCH_RESULT = "ARG_SEARCH_RESULT"
    }
}
