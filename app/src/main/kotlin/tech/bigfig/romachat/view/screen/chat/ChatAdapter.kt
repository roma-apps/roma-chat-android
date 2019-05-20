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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.entity.Attachment
import tech.bigfig.romachat.databinding.LayoutChatMessageItemBinding
import tech.bigfig.romachat.view.utils.CustomEmojiHelper
import tech.bigfig.romachat.view.utils.ContentClickListener
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

            if (!message.isMedia && message.content != null) {
                val emojifiedText =
                    CustomEmojiHelper.emojifyText(message.content, message.emojis, binding.chatMessageContent)
                TextFormatter.setClickableText(binding.chatMessageContent, emojifiedText, message.mentions,
                    object : ContentClickListener {
                        override fun onTagClick(tag: String) {
                        }

                        override fun onAccountClick(id: String) {
                            listener?.onAccountClick(id)
                        }

                        override fun onUrlClick(url: String) {
                            listener?.onUrlClick(url)
                        }

                        override fun onClick() {
                            listener?.onMessageClick(message, binding.chatMessageMediaPreview)
                        }

                        override fun onLongClick() {
                            listener?.onMessageLongClick(message)
                        }
                    })
            }

            //we need to set click listeners for the media view as well as for spanned text
            binding.chatMessageMediaPreviewContainer.setOnClickListener { listener?.onMessageClick(message, binding.chatMessageMediaPreview) }
            binding.chatMessageMediaPreviewContainer.setOnLongClickListener {
                listener?.onMessageLongClick(message)
                true
            }
        }
    }

    interface ChatAdapterListener {
        fun onMessageClick(message: MessageViewData, view: View)
        fun onMessageLongClick(message: MessageViewData)
        fun onUrlClick(url: String)
        fun onAccountClick(accountId: String)
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