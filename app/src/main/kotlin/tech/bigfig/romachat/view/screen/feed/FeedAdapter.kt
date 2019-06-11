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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.entity.Attachment
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.databinding.LayoutFeedListItemBinding
import tech.bigfig.romachat.view.utils.ContentClickListener
import tech.bigfig.romachat.view.utils.CustomEmojiHelper
import tech.bigfig.romachat.view.utils.DateUtils
import tech.bigfig.romachat.view.utils.TextFormatter
import java.util.*


class FeedAdapter(
    private val listener: FeedAdapterListener?
) : ListAdapter<FeedViewData, FeedAdapter.ViewHolder>(DiffCallback()) {

    private lateinit var values: MutableList<FeedViewData>

    fun setItems(newValues: List<FeedViewData>) {
        values = newValues.toMutableList()
        submitList(values)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutFeedListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateItem(item: FeedViewData) {
        val index = values.indexOf(item)
        if (index > -1) {
            values[index] = item
            notifyItemChanged(index)
        }
    }

    inner class ViewHolder(val binding: LayoutFeedListItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeedViewData) {
            binding.post = item
            binding.executePendingBindings()

            binding.avatar.setOnClickListener { listener?.onAvatarClick(item) }
            binding.repostUsername.setOnClickListener { listener?.onRepostedByClick(item) }
            binding.repostStatus.setOnClickListener { listener?.onRepostedByClick(item) }
            binding.favorite.setOnClickListener { listener?.onFavoriteClick(item) }
            binding.favoriteCount.setOnClickListener { listener?.onFavoriteClick(item) }
            binding.reply.setOnClickListener { listener?.onReplyClick(item) }
            binding.replyCount.setOnClickListener { listener?.onReplyClick(item) }
            binding.repost.setOnClickListener { listener?.onRepostClick(item) }
            binding.repostCount.setOnClickListener { listener?.onRepostClick(item) }

            val emojifiedText = CustomEmojiHelper.emojifyText(item.status.content, item.status.emojis, binding.content)
            TextFormatter.setClickableText(binding.content, emojifiedText, item.status.mentions, item, listener)

            binding.attachments.visibility = if (item.status.attachments.isNotEmpty()) View.VISIBLE else View.GONE
            if (item.status.attachments.isNotEmpty()) {
                binding.attachments.removeAllViews()
                item.status.attachments.forEachIndexed { index, attachment ->
                    val imageView = ImageView(binding.attachments.context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        adjustViewBounds = false
                        setOnClickListener { listener?.onMediaClick(item.status, index, this) }
                    }

                    val height =
                        binding.attachments.context.resources.getDimensionPixelSize(R.dimen.feed_media_max_height)

                    val request =
                        if (attachment.type == Attachment.Type.IMAGE) Picasso.get().load(attachment.previewUrl)
                        else Picasso.get().load(R.drawable.video_preview_background)

                    request.error(R.drawable.video_preview_background)
                        .resize(0, height).onlyScaleDown()
                        .into(imageView)


                    binding.attachments.addView(imageView)

//                    val lp = imageView.layoutParams as FlexboxLayout.LayoutParams
//                    if (item.attachments.size > 1) {
//                        lp.flexGrow = 1f
//                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FeedViewData>() {
        override fun areItemsTheSame(oldItem: FeedViewData, newItem: FeedViewData): Boolean {
            return oldItem.status.id == newItem.status.id && oldItem.repostedBy == newItem.repostedBy
        }

        override fun areContentsTheSame(oldItem: FeedViewData, newItem: FeedViewData): Boolean {
            return oldItem.status.account.displayName == newItem.status.account.displayName
                    && oldItem.status.account.avatar == newItem.status.account.avatar
                    && SpannableStringBuilder(oldItem.status.content) == SpannableStringBuilder(newItem.status.content)
                    && oldItem.status.createdAt == newItem.status.createdAt
                    && oldItem.isRepost == newItem.isRepost
                    && oldItem.repostedBy?.id == newItem.repostedBy?.id
        }
    }

    interface FeedAdapterListener : ContentClickListener<FeedViewData> {
        fun onMediaClick(status: Status, mediaIndex: Int, view: View)
        fun onAvatarClick(feedViewData: FeedViewData)
        fun onFavoriteClick(feedViewData: FeedViewData)
        fun onReplyClick(feedViewData: FeedViewData)
        fun onRepostClick(feedViewData: FeedViewData)
        fun onRepostedByClick(feedViewData: FeedViewData)
    }
}

@BindingAdapter("app:date")
fun formatDate(view: TextView, date: Date) {
    view.text = DateUtils.getRelativeTimeSpanString(view.context, date.time, Date().time)
}

@BindingAdapter("app:title")
fun formatTitle(view: TextView, status: Status) {
    view.visibility = if (status.spoilerText.isEmpty()) View.GONE else View.VISIBLE
    view.text = CustomEmojiHelper.emojifyString(status.spoilerText, status.emojis, view)
}
