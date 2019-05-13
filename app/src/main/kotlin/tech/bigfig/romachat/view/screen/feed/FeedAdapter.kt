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


import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.databinding.LayoutFeedListItemBinding
import tech.bigfig.romachat.view.utils.CustomEmojiHelper
import tech.bigfig.romachat.view.utils.DateUtils
import tech.bigfig.romachat.view.utils.MessageClickListener
import tech.bigfig.romachat.view.utils.TextFormatter
import java.util.*


class FeedAdapter(
    private val listener: UserSearchAdapterListener?
) : RecyclerView.Adapter<FeedAdapter.ViewHolder>() {

    private var values: MutableList<Status> = mutableListOf()

    fun setItems(newValues: List<Status>) {
        values.clear()
        values.addAll(newValues)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutFeedListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(values[position])
    }

    override fun getItemCount() = values.size

    inner class ViewHolder(val binding: LayoutFeedListItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Status) {
            binding.post = item
            binding.executePendingBindings()

            val emojifiedText = CustomEmojiHelper.emojifyText(item.content, item.emojis, binding.content)
            TextFormatter.setClickableText(binding.content, emojifiedText, item.mentions,
                object : MessageClickListener {
                    override fun onTagClick(tag: String) {
                    }

                    override fun onAccountClick(id: String) {
                    }

                    override fun onUrlClick(url: String) {
                    }

                    override fun onClick() {
//                        listener?.onMessageClick(message, binding.chatMessageMediaPreview)
                    }

                    override fun onLongClick() {
//                        listener?.onMessageLongClick(message)
                    }
                })
            //
//            binding.root.setOnClickListener { listener?.onUserClick(item) }
//            binding.status.setOnClickListener { listener?.onAddClick(item) }
        }
    }

    interface UserSearchAdapterListener {
//        fun onUserClick(item: UserSearchResultViewData)
//
//        fun onAddClick(item: UserSearchResultViewData)
    }
}

@BindingAdapter("app:date")
fun formatDate(view: TextView, date: Date) {
    view.text = DateUtils.getRelativeTimeSpanString(view.context, date.time, Date().time)
}
