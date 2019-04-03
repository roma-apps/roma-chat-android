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

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.ChatRepository
import tech.bigfig.romachat.data.ResultStatus
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.data.entity.Status
import javax.inject.Inject

class CameraResultRecipientViewModel @Inject constructor(context: Context, repository: ChatRepository) : ViewModel() {

    var fileUri: Uri? = null

    //LiveDatas to update UI
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val isError: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage: MutableLiveData<String> = MutableLiveData()


    private val loadData = MutableLiveData<Boolean>()

    fun loadData() {
        loadData.value = true
    }

    val recipients: LiveData<List<ChatAccountEntity>> = Transformations.switchMap(loadData) {
        repository.getRecipients()
    }

    fun selectRecipient(recipient: ChatAccountEntity) {
        if (isLoading.value != true) {
            selectedRecipient.postValue(recipient)
        }
    }

    private val selectedRecipient = MutableLiveData<ChatAccountEntity>()

    val uploadMedia: LiveData<Status?> = Transformations.switchMap(selectedRecipient) { account ->
        if (fileUri == null) throw IllegalArgumentException("File uri wasn't set")

        isError.value = false
        isLoading.value = true

        //since we group messages by chats using mentions, we add @user as a message text
        Transformations.map(repository.postMedia("@${account.username}", fileUri!!)) { result ->
            Log.d(LOG_TAG, "result $result")
            when (result.status) {
                ResultStatus.SUCCESS ->
                    if (result.data != null) {
                        return@map result.data

                    } else {
                        showError(context.getString(R.string.chat_send_error_post))
                        return@map null
                    }
                ResultStatus.ERROR -> {
                    showError(context.getString(R.string.chat_send_error_post), result.error ?: "")
                    return@map null
                }
                else -> return@map null
            }
        }
    }

    private fun showError(error: String, logError: String = "") {
        isError.value = true
        errorMessage.value = error

        val sb = StringBuilder(error)
        if (!logError.isEmpty()) sb.append(" ").append(logError)

        Log.e(LOG_TAG, sb.toString())

        isLoading.value = false
    }

    companion object {
        private const val LOG_TAG = "CameraResultRecipientVM"
    }
}
