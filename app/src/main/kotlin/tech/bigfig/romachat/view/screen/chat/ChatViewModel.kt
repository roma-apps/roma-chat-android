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
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.ChatRepository
import tech.bigfig.romachat.data.entity.Status
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ChatViewModel @Inject constructor(repository: ChatRepository, val context: Context) : ViewModel() {

    var accountId: String? = null
    var accountDisplayName: String? = null

    private val loadData = MutableLiveData<Boolean>()
    private val submitMessage: MutableLiveData<String> = MutableLiveData()

    val messageText: MutableLiveData<String> = MutableLiveData()

    //LiveDatas to update UI
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()

    val isError: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage: MutableLiveData<String> = MutableLiveData()

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
                        !theSameDay,
                        formatDate(message.createdAt),
                        message.fromMe != lastFromMe || !theSameDay,
                        if (message.fromMe) context.getString(R.string.chat_message_user_me) else accountDisplayName,
                        message.fromMe,
                        false,
                        message.content,
                        message.mentions,
                        null
                    )
                )

                //show each attachment as a separate message
                message.attachments.forEach { attachment ->
                    res.add(
                        MessageViewData(
                            false,
                            null,
                            false,
                            null,
                            message.fromMe,
                            true,
                            null,
                            null,
                            attachment
                        )
                    )
                }

                lastDate = message.createdAt
                lastFromMe = message.fromMe
            }

            return@map res.toList()
        }
    }

    // User entered a messageText and clicked submit
    fun onSubmitClick(text: String) {
        Log.d(LOG_TAG, "onSubmitClick $text")

        if (text.isEmpty()) {
            showError(context.getString(R.string.chat_send_error_no_text))
            return
        }
        if (text.length >= CHARACTER_LIMIT) {
            showError(context.getString(R.string.chat_send_error_too_long_text))
            return
        }

        isError.value = false

        //initiate post
        submitMessage.value = text
    }

    // Subscribe to message, make an API call to post it
    val postMessage: LiveData<Status?> = Transformations.switchMap(submitMessage) {
        Transformations.map(repository.postMessage(it)) { result ->
            if (result.error != null) {
                showError(
                    context.getString(R.string.chat_send_error_post),
                    result.error
                )
                null
            } else {//success
                if (result.data != null) {
                    messageText.postValue("")

                    result.data

                } else {
                    null
                }
            }
        }
    }

    fun showError(error: String, logError: String = "") {
        isError.value = true
        errorMessage.value = error

        val sb = StringBuilder(error)
        if (!logError.isEmpty()) sb.append(" ").append(logError)

        Log.e(LOG_TAG, sb.toString())

        isLoading.value = false
    }

    @Suppress("DEPRECATION")
    private fun Date.theSameDay(date: Date): Boolean {
        return year == date.year && month == date.month && this.date == date.date
    }

    private val sdf = SimpleDateFormat("MMMM dd", Locale.getDefault())
    private fun formatDate(date: Date): String {
        val today = Date()
        return if (date.theSameDay(today)) context.getString(R.string.chat_message_date_today).toUpperCase() else
            sdf.format(date).toUpperCase()
    }

    companion object {
        private const val LOG_TAG = "LoginViewModel"

        private const val CHARACTER_LIMIT = 500
    }
}
