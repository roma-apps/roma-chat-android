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

package tech.bigfig.romachat.view.screen.compose

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.ChatRepository
import tech.bigfig.romachat.data.UserRepository
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.view.screen.chat.CHARACTER_LIMIT
import tech.bigfig.romachat.view.screen.chat.ChatMessagesService
import timber.log.Timber
import javax.inject.Inject

class NewPostViewModel @Inject constructor(
    val repository: UserRepository,
    val chatRepository: ChatRepository,
    val context: Context
) : ViewModel() {

    var statusToReply: MutableLiveData<Status> = MutableLiveData()
    private var currentAccount = repository.getCurrentAccount()

    private val loadData = MutableLiveData<Boolean>()

    val initialText = MutableLiveData<String>()

    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val isError: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage: MutableLiveData<String> = MutableLiveData()

    private val submitMessage: MutableLiveData<String> = MutableLiveData()

    fun loadData() {
        loadData.value = true
    }

    fun initData(statusToReply: Status?) {
        this.statusToReply.postValue(statusToReply)

        if (statusToReply != null) {

            initialText.postValue(
                statusToReply.mentions.filter { it.id != currentAccount?.accountId }.joinToString(
                    separator = " ", postfix = " "
                ) {
                    context.getString(
                        R.string.username_pattern,
                        it.username
                    )
                }
            )
        }
    }

    // User entered text and clicked submit
    fun onSubmitClick(text: String) {
        Timber.d("onSubmitClick $text")

        if (text.isEmpty()) {
            showError(context.getString(R.string.chat_send_error_no_text))
            return
        }
        if (text.length >= CHARACTER_LIMIT) {
            showError(context.getString(R.string.chat_send_error_too_long_text))
            return
        }

//        //@username mention is required
//        var message = text
//        if (!text.contains("@${account?.username}")) {
//            message = "@${account?.username} $text"
//        }

        isError.value = false
        isLoading.value = true

        //initiate post
        submitMessage.value = text
    }

    // Subscribe to message, make an API call to post it
    val postMessage: LiveData<Status?> = Transformations.switchMap(submitMessage) {
        Transformations.map(
            chatRepository.postMessage(
                it,
                statusToReply.value?.inReplyToId,
                statusToReply.value?.visibility ?: Status.Visibility.DIRECT
            )
        ) { result ->
            if (result.error != null) {
                showError(
                    context.getString(R.string.chat_send_error_post),
                    result.error
                )
                null
            } else {//success
                isLoading.value = false
                if (result.data != null) {

                    ChatMessagesService.startFetchingLastMessages(context)

                    result.data

                } else {

                    null
                }
            }
        }
    }

    private fun showError(error: String, logError: String = "") {
        isError.value = true
        errorMessage.value = error

        val sb = StringBuilder(error)
        if (logError.isNotEmpty()) sb.append(" ").append(logError)

        Timber.e(sb.toString())

        isLoading.value = false
    }
}
