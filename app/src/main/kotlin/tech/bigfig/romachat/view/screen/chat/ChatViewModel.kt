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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.ChatRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ChatViewModel @Inject constructor(repository: ChatRepository, val context: Context) : ViewModel() {

    var accountId: String? = null
    var accountDisplayName: String? = null

    private val loadData = MutableLiveData<Boolean>()

    fun loadData() {
        loadData.value = true
    }

    val messageList: LiveData<List<MessageViewData>> = Transformations.switchMap(loadData) {
        Transformations.map(
            repository.getChatMessages(
                accountId ?: throw IllegalArgumentException("accountId is null")
            )
        )
        { messages ->
            @Suppress("DEPRECATION")
            var lastDate = Date(1970, 1, 1)
            var lastFromMe: Boolean? = null

            val res: MutableList<MessageViewData> = mutableListOf()

            messages.forEach { message ->
                val theSameDay = message.createdAt.theSameDay(lastDate)

                res.add(
                    MessageViewData(
                        message.content,
                        message.mentions,
                        !theSameDay,
                        formatDate(message.createdAt),
                        message.fromMe != lastFromMe || !theSameDay,
                        if (message.fromMe) context.getString(R.string.chat_message_user_me) else accountDisplayName,
                        message.fromMe
                    )
                )
                lastDate = message.createdAt
                lastFromMe = message.fromMe
            }

            return@map res.toList()
        }
    }

    @Suppress("DEPRECATION")
    private fun Date.theSameDay(date: Date): Boolean {
        return year == date.year && month == date.month && this.date == date.date
    }

    private val sdf = SimpleDateFormat("MMMM dd", Locale.getDefault())
    private fun formatDate(date: Date): String {
        val today = Date()
        if (date.theSameDay(today)) return context.getString(R.string.chat_message_date_today).toUpperCase() else
            return sdf.format(date).toUpperCase()
    }
}
