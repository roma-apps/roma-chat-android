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


import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.databinding.LayoutFeedListItemBinding
import tech.bigfig.romachat.view.utils.ContentClickListener
import tech.bigfig.romachat.view.utils.CustomEmojiHelper
import tech.bigfig.romachat.view.utils.DateUtils
import tech.bigfig.romachat.view.utils.TextFormatter
import java.util.*


class FeedAdapter(
    private val listener: ContentClickListener?
) : ListAdapter<Status, FeedAdapter.ViewHolder>(DiffCallback()) {

    fun setItems(newValues: List<Status>) {
        submitList(newValues)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutFeedListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(val binding: LayoutFeedListItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Status) {
            binding.post = item
            binding.executePendingBindings()

            val emojifiedText = CustomEmojiHelper.emojifyText(item.content, item.emojis, binding.content)
            TextFormatter.setClickableText(binding.content, emojifiedText, item.mentions, listener)
            //
//            binding.root.setOnClickListener { listener?.onUserClick(item) }
//            binding.status.setOnClickListener { listener?.onAddClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Status>() {
        override fun areItemsTheSame(oldItem: Status, newItem: Status): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Status, newItem: Status): Boolean {
            return oldItem.account.displayName == newItem.account.displayName
                    && oldItem.account.avatar == newItem.account.avatar
                    && SpannableStringBuilder(oldItem.content) == SpannableStringBuilder(newItem.content)
                    && oldItem.createdAt == newItem.createdAt
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
