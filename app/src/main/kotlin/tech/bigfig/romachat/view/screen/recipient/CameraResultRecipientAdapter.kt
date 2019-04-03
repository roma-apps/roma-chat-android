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

package tech.bigfig.romachat.view.screen.recipient


import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.databinding.LayoutRecipientListItemBinding


class CameraResultRecipientAdapter(
    private val listener: CameraResultRecipientAdapterListener?
) : RecyclerView.Adapter<CameraResultRecipientAdapter.ViewHolder>() {

    private var values: MutableList<ChatAccountEntity> = mutableListOf()

    fun setItems(newValues: List<ChatAccountEntity>) {
        values.clear()
        values.addAll(newValues)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutRecipientListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(values[position])
    }

    override fun getItemCount() = values.size

    inner class ViewHolder(val binding: LayoutRecipientListItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(account: ChatAccountEntity) {
            binding.account = account
            binding.executePendingBindings()

            binding.root.setOnClickListener { listener?.onAccountClick(account) }
        }
    }

    interface CameraResultRecipientAdapterListener {
        fun onAccountClick(account: ChatAccountEntity)
    }
}

@BindingAdapter("app:avatarUrl")
fun loadImage(view: ImageView, avatarUrl: String) {
    Picasso.get().load(avatarUrl).error(R.drawable.default_user_avatar).into(view)
}
