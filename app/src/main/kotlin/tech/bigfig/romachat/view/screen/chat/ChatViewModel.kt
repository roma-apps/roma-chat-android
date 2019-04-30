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
import android.net.Uri
import android.text.Spanned
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.ChatRepository
import tech.bigfig.romachat.data.ResultStatus
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.utils.HtmlUtils
import tech.bigfig.romachat.utils.MEDIA_SIZE_UNKNOWN
import tech.bigfig.romachat.utils.getMediaSize
import tech.bigfig.romachat.utils.getMimeType
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ChatViewModel @Inject constructor(val repository: ChatRepository, val context: Context) : ViewModel() {

    var account: ChatAccountEntity? = null
    private var currentAccount = repository.getCurrentAccount()

    private val loadData = MutableLiveData<Boolean>()
    private val submitMessage: MutableLiveData<String> = MutableLiveData()
    private val fileToUpload: MutableLiveData<Uri> = MutableLiveData()
    private val messageToDelete: MutableLiveData<String> = MutableLiveData()

    val messageText: MutableLiveData<String> = MutableLiveData()

    //LiveDatas to update UI
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()

    val isError: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage: MutableLiveData<String> = MutableLiveData()

    val shortError: MutableLiveData<String> = MutableLiveData()

    fun loadData() {
        loadData.value = true
    }

    val messageList: LiveData<List<MessageViewData>> = Transformations.switchMap(loadData) {
        Transformations.map(
            repository.getChatMessages(
                account?.id ?: throw IllegalArgumentException("accountId is null")
            )
        )
        { messages ->
            @Suppress("DEPRECATION")
            var lastDate = Date(1970, 1, 1)
            var lastFromMe: Boolean? = null

            val res: MutableList<MessageViewData> = mutableListOf()

            messages.forEach { message ->
                val theSameDay = message.createdAt.theSameDay(lastDate)

                val content = formatContent(message.content)

                if (content.isNotEmpty()) {//might be empty if message contained only @user mention
                    res.add(
                        MessageViewData(
                            message.id,
                            !theSameDay,
                            formatDate(message.createdAt),
                            message.fromMe != lastFromMe || !theSameDay,
                            if (message.fromMe) context.getString(R.string.chat_message_user_me) else account!!.displayName,
                            message.fromMe,
                            false,
                            content,
                            message.mentions,
                            message.emojis,
                            null
                        )
                    )
                }

                //show each attachment as a separate message
                message.attachments.forEach { attachment ->
                    res.add(
                        MessageViewData(
                            message.id,
                            false,
                            null,
                            false,
                            null,
                            message.fromMe,
                            true,
                            null,
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
        Timber.d("onSubmitClick $text")

        if (text.isEmpty()) {
            showError(context.getString(R.string.chat_send_error_no_text))
            return
        }
        if (text.length >= CHARACTER_LIMIT) {
            showError(context.getString(R.string.chat_send_error_too_long_text))
            return
        }

        //@username mention is required
        var message = text
        if (!text.contains("@${account?.username}")) {
            message = text.plus(" @${account?.username}")
        }

        isError.value = false

        //initiate post
        submitMessage.value = message
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

                    ChatMessagesService.startFetchingLastMessages(context)

                    result.data

                } else {
                    null
                }
            }
        }
    }

    fun processMedia(uri: Uri?) {
        if (uri != null) {
            val mediaSize = getMediaSize(context.contentResolver, uri)
            pickMedia(uri, mediaSize, null)
        }
    }

    private fun pickMedia(uri: Uri, mediaSize: Long, description: String?) {
        if (mediaSize == MEDIA_SIZE_UNKNOWN) {
            showError(context.getString(R.string.chat_send_error_media_upload_opening))
            return
        }

        val mimeType = getMimeType(uri, context.contentResolver)

        if (mimeType != null) {
            val topLevelType = mimeType.substring(0, mimeType.indexOf('/'))
            when (topLevelType) {
                "video" -> {
                    if (mediaSize > VIDEO_SIZE_LIMIT) {
                        showError(context.getString(R.string.chat_send_error_video_upload_size))
                        return
                    }
                    fileToUpload.postValue(uri)
                }
                "image" -> {
                    if (mediaSize > IMAGE_SIZE_LIMIT) {
                        showError(context.getString(R.string.chat_send_error_image_upload_size))
                        return
                    }
                    fileToUpload.postValue(uri)
                }
                else -> {
                    showError(context.getString(R.string.chat_send_error_media_upload_type))
                }
            }
        } else {
            showError(context.getString(R.string.chat_send_error_media_upload_type))
        }
    }

    val uploadMedia: LiveData<Status?> = Transformations.switchMap(fileToUpload) {
        //since we group messages by chats using mentions, we add @user as a message text
        Transformations.map(repository.postMedia("@${account!!.username}", it)) { result ->
            if (result.error != null) {
                showError(
                    context.getString(R.string.chat_send_error_post),
                    result.error
                )
                null
            } else {//success
                if (result.data != null) {
                    messageText.postValue("")

                    ChatMessagesService.startFetchingLastMessages(context)

                    result.data

                } else {
                    null
                }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        messageToDelete.postValue(messageId)
    }

    val deleteMessage: LiveData<Boolean?> = Transformations.switchMap(messageToDelete) {
        Transformations.map(repository.deleteMessage(it)) { result ->
            when (result.status) {
                ResultStatus.SUCCESS -> {
                    if (result.data != null) {
                        result.data
                    } else {
                        shortError.postValue(context.getString(R.string.chat_error_delete))
                        null
                    }
                }
                ResultStatus.ERROR -> {
                    shortError.postValue(context.getString(R.string.chat_error_delete))
                    null
                }
                ResultStatus.LOADING -> null
            }
        }
    }

    private fun formatContent(content: Spanned): Spanned {

        val res = HtmlUtils.toHtml(content)
            .removeUsername(account?.localUsername)
            .removeUsername(currentAccount?.username)
            .replace("  ", " ")
            .trim()

        return HtmlUtils.fromHtml(res)
    }

    private fun String.removeUsername(username: String?): String {
        return this.replace(Regex("<a href=\"https://.*/users/$username\">@$username</a><br>\n"), "")
            .replace(Regex("<a href=\"https://.*/users/$username\">@$username</a>"), "")
    }

    private fun showError(error: String, logError: String = "") {
        isError.value = true
        errorMessage.value = error

        val sb = StringBuilder(error)
        if (!logError.isEmpty()) sb.append(" ").append(logError)

        Timber.e(sb.toString())

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
        private const val CHARACTER_LIMIT = 500

        private const val IMAGE_SIZE_LIMIT = 8388608 // 8MB
        private const val VIDEO_SIZE_LIMIT = 41943040 // 40MB
    }
}
