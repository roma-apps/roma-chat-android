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
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import tech.bigfig.romachat.databinding.FragmentMessageItemDialogBinding
import tech.bigfig.romachat.view.utils.CustomEmojiHelper
import tech.bigfig.romachat.view.utils.TextFormatter

class MessageItemDialogFragment : DialogFragment() {

    private var listener: Listener? = null

    private lateinit var binding: FragmentMessageItemDialogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMessageItemDialogBinding.inflate(inflater, container, false)

        val message =
            arguments?.getParcelable<MessageViewData>(ARG_MESSAGE) ?: throw IllegalArgumentException("Empty message")

        message.showDate = false
        message.showAccount = true

        binding.message = message
        if (!message.isMedia && message.content != null) {
            val emojifiedText =
                CustomEmojiHelper.emojifyText(
                    message.content,
                    message.emojis,
                    binding.messageContent.chatMessageContent
                )
            TextFormatter.setClickableText(
                binding.messageContent.chatMessageContent,
                emojifiedText,
                message.mentions,
                null
            )
        }

        binding.delete.setOnClickListener {
            listener?.onDeleteClick(message.id)
            dismiss()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
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

        fun newInstance(message: MessageViewData): MessageItemDialogFragment =
            MessageItemDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MESSAGE, message)
                }
            }

        private const val ARG_MESSAGE = "ARG_MESSAGE"
    }
}
