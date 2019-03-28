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


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tech.bigfig.romachat.data.db.entity.MessageEntity
import tech.bigfig.romachat.databinding.LayoutChatMessageItemBinding
import tech.bigfig.romachat.view.utils.TextFormatter


class ChatAdapter(
    private val listener: ChatAdapterListener?
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {


    private var values: MutableList<MessageEntity> = mutableListOf()

    fun setItems(newValues: List<MessageEntity>) {
        values.clear()
        values.addAll(newValues)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutChatMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(values[position])
    }

    override fun getItemCount() = values.size

    inner class ViewHolder(val binding: LayoutChatMessageItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageEntity) {
            TextFormatter.setClickableText(binding.chatMessageContent, message.content, message.mentions)

            binding.message = message
            binding.executePendingBindings()

            binding.root.setOnClickListener { listener?.onMessageClick(message) }
        }
    }

    interface ChatAdapterListener {
        fun onMessageClick(message: MessageEntity)
    }
}