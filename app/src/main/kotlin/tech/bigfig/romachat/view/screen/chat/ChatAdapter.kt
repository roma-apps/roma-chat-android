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


import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.entity.Attachment
import tech.bigfig.romachat.databinding.LayoutChatMessageItemBinding
import tech.bigfig.romachat.view.utils.TextFormatter


class ChatAdapter(
    private val listener: ChatAdapterListener?
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {


    private var values: MutableList<MessageViewData> = mutableListOf()

    fun setItems(newValues: List<MessageViewData>) {
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

        fun bind(message: MessageViewData) {
            binding.message = message
            binding.executePendingBindings()

            binding.root.setOnClickListener { listener?.onMessageClick(message) }
        }
    }

    interface ChatAdapterListener {
        fun onMessageClick(message: MessageViewData)
    }
}

@BindingAdapter("app:attachment")
fun loadImage(view: ImageView, attachment: Attachment?) {
    if (attachment == null) return

    view.contentDescription = if (TextUtils.isEmpty(attachment.description))
        view.context.getString(R.string.a11y_message_media) else attachment.description

    val maxSize = view.context.resources.getDimensionPixelSize(R.dimen.chat_message_media_max_width)

    val request = if (attachment.type == Attachment.Type.IMAGE) Picasso.get().load(attachment.previewUrl)
    else Picasso.get().load(R.drawable.video_preview_background)

    request.error(R.drawable.video_preview_background)
        .resize(maxSize, 0).onlyScaleDown()
        .into(view)
}

@BindingAdapter("app:message")
fun formatText(view: TextView, message: MessageViewData) {
    if (!message.isMedia && message.content != null) {
        TextFormatter.setClickableText(view, message.content, message.mentions)
    }
}