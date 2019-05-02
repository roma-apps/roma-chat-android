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
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import tech.bigfig.romachat.NavGraphDirections
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.databinding.FragmentProfileBinding
import tech.bigfig.romachat.view.screen.search.UserSearchResultViewData

class ProfileFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        val user =
            arguments?.getParcelable<UserSearchResultViewData>(ARG_USER) ?: throw IllegalArgumentException("Empty user")

        binding.user = user

        binding.chat.setOnClickListener {

            findNavController().navigate(
                NavGraphDirections.actionGlobalChatFragment(
                    ChatAccountEntity(
                        user.account.id,
                        user.account.username,
                        user.account.localUsername,
                        user.account.displayName,
                        user.account.avatar
                    )
                )
            )
            dismiss()
        }

        return binding.root
    }

    companion object {

        fun newInstance(message: UserSearchResultViewData): ProfileFragment =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_USER, message)
                }
            }

        private const val ARG_USER = "ARG_USER"
    }
}
