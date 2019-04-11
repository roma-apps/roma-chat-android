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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import tech.bigfig.romachat.databinding.FragmentMessageItemDialogBinding

class MessageItemDialogFragment : BottomSheetDialogFragment() {

    private var listener: Listener? = null

    private lateinit var binding: FragmentMessageItemDialogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMessageItemDialogBinding.inflate(layoutInflater, container, false)

        val messageId = arguments?.getString(ARG_MESSAGE_ID) ?: throw IllegalArgumentException("Empty message id")
        binding.delete.setOnClickListener {
            listener?.onDeleteClick(messageId)
            dismiss()
        }

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        if (parent != null) {
            listener = parent as Listener
        } else {
            listener = context as Listener
        }
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    interface Listener {
        fun onDeleteClick(messageId: String)
    }

    companion object {

        fun newInstance(messageId: String): MessageItemDialogFragment =
            MessageItemDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE_ID, messageId)
                }
            }

        private const val ARG_MESSAGE_ID = "ARG_MESSAGE_ID"
    }
}
