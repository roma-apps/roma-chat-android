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

package tech.bigfig.romachat.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.api.ProgressRequestBody
import tech.bigfig.romachat.data.api.RestApi
import tech.bigfig.romachat.data.api.apiCallToLiveData
import tech.bigfig.romachat.data.db.AccountManager
import tech.bigfig.romachat.data.db.AppDatabase
import tech.bigfig.romachat.data.db.entity.AccountEntity
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.data.db.entity.MessageEntity
import tech.bigfig.romachat.data.entity.Account
import tech.bigfig.romachat.data.entity.Attachment
import tech.bigfig.romachat.data.entity.ChatInfo
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.utils.StringUtils
import tech.bigfig.romachat.utils.getMediaSize
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val MESSAGES_AMOUNT = 40 // max available amount to fetch at a time
private const val LOG_TAG = "ChatRepository"

class ChatRepository @Inject constructor(
    private val restApi: RestApi, private val accountManager: AccountManager, private val db: AppDatabase,
    private val context: Context
) {

    fun getCurrentAccount(): AccountEntity? {
        return accountManager.activeAccount
    }

    fun getAllChats(): LiveData<List<ChatInfo>> {
        return db.messageDao().loadAllChats()
    }

    fun getRecipients(): LiveData<List<ChatAccountEntity>> {
        return db.chatAccountDao().loadAll()
    }

    fun getChatMessages(accountId: String): LiveData<List<MessageEntity>> {
        return db.messageDao().loadAll(accountId)
    }

    fun postMessage(message: String): LiveData<Result<Status>> {
        return apiCallToLiveData(
            restApi.createStatus(
                "Bearer " + accountManager.activeAccount?.accessToken,
                accountManager.activeAccount?.domain!!,
                message,
                null,
                null,
                Status.Visibility.DIRECT.serverString(),
                false,
                null,
                StringUtils.randomAlphanumericString(16)
            )
        ) { it }
    }

    fun postMedia(sendTo: String, uri: Uri): LiveData<Result<Status>> {
        return Transformations.switchMap(uploadMedia(uri)) { attachmentResult ->
            if (attachmentResult.data != null) {
                apiCallToLiveData(
                    restApi.createStatus(
                        "Bearer " + accountManager.activeAccount?.accessToken,
                        accountManager.activeAccount?.domain!!,
                        sendTo,
                        null,
                        null,
                        Status.Visibility.DIRECT.serverString(),
                        false,
                        listOf(attachmentResult.data.id),
                        StringUtils.randomAlphanumericString(16)
                    )
                ) { it }
            } else if (attachmentResult.error != null) {
                val l = MutableLiveData<Result<Status>>()
                l.postValue(Result.error(attachmentResult.error))
                return@switchMap l
            } else null
        }
    }

    private fun uploadMedia(uri: Uri): LiveData<Result<Attachment>> {
        return object : LiveData<Result<Attachment>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    postValue(Result.loading())

                    var mimeType = context.contentResolver.getType(uri)
                    val map = MimeTypeMap.getSingleton()
                    val fileExtension = map.getExtensionFromMimeType(mimeType)
                    val filename = String.format(
                        "%s_%s_%s.%s",
                        context.getString(R.string.app_name),
                        Date().time.toString(),
                        StringUtils.randomAlphanumericString(10),
                        fileExtension
                    )

                    val stream: InputStream?

                    try {
                        stream = context.contentResolver.openInputStream(uri)
                    } catch (e: FileNotFoundException) {
                        Log.w(LOG_TAG, e)
                        return
                    }


                    if (mimeType == null) mimeType = "multipart/form-data"

//                    item.preview.setProgress(0)

                    val fileBody =
                        ProgressRequestBody(stream,
                            getMediaSize(context.contentResolver, uri),
                            MediaType.parse(mimeType),
                            object :
                                ProgressRequestBody.UploadCallback { // may reference activity longer than I would like to
                                var lastProgress = -1

                                override fun onProgressUpdate(percentage: Int) {
//                                    if (percentage != lastProgress) {
//                                        runOnUiThread { item.preview.setProgress(percentage) }
//                                    }
                                    lastProgress = percentage
                                    Log.d(LOG_TAG, "progress $percentage")
                                }
                            })

                    val body = MultipartBody.Part.createFormData("file", filename, fileBody)

                    val uploadRequest = restApi.uploadMedia(body)

                    uploadRequest.enqueue(object : Callback<Attachment> {
                        override fun onResponse(call: Call<Attachment>, response: retrofit2.Response<Attachment>) {
                            if (response.isSuccessful) {
                                postValue(Result.success(response.body()))
                            } else {
                                Log.d(LOG_TAG, "Upload request failed. " + response.message())
                                postValue(Result.error(response.message()))
                            }
                        }

                        override fun onFailure(call: Call<Attachment>, t: Throwable) {
                            Log.d(LOG_TAG, "Upload request failed. " + t.message)
                            postValue(Result.error(t.message ?: "Upload media call onFailure()"))
                        }
                    })
                }
            }
        }
    }

    fun deleteMessage(messageId: String): LiveData<Result<Boolean>> {
        return object : LiveData<Result<Boolean>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    postValue(Result.loading())

                    restApi.deleteStatus(messageId).enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                            if (response.isSuccessful) {

                                //remove stored message from db
                                db.messageDao().delete(messageId)

                                postValue(Result.success(true))
                            } else {
                                Log.d(LOG_TAG, "deleteMessage request failed. " + response.message())
                                postValue(Result.error(response.message()))
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.d(LOG_TAG, "deleteMessage request failed. " + t.message)
                            postValue(Result.error(t.message ?: "Upload media call onFailure()"))
                        }
                    })
                }
            }
        }
    }

    /**
     * Load the messages from api and store them to db.
     *
     * Current api restrictions don't allow to get the concrete chat messages directly. So we agreed to group the messages
     * by chats on the client-side
     */
    fun storeMessages(pages: Int): LiveData<Result<Boolean>> {

        return object : LiveData<Result<Boolean>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    postValue(Result.loading())

                    var lastId: String? = null
                    var loadedCount = Int.MAX_VALUE

                    val messages: MutableList<MessageEntity> = mutableListOf()
                    val accounts: MutableMap<String, ChatAccountEntity?> = mutableMapOf()

                    GlobalScope.launch(Dispatchers.Main) {
                        async(Dispatchers.IO) {
                            var repeatCount = 0
                            do {
                                repeatCount++
                                try {
                                    val response = restApi.directTimeline(lastId, null, MESSAGES_AMOUNT).execute()
                                    if (response.isSuccessful) {
                                        val body = response.body()

                                        loadedCount = body?.size ?: 0
                                        Log.d(LOG_TAG, "Loaded messages: $loadedCount ($repeatCount iteration)")

                                        if (loadedCount > 0) {
                                            lastId = body?.last()?.id
                                            Log.d(LOG_TAG, "Last message id: $lastId")

                                            body?.forEach { message -> processMessage(message, messages, accounts) }

                                            postValue(Result.success(true))
                                        }
                                        if (loadedCount == 0 || repeatCount == pages) {// no more messages OR last iteration
                                            Log.d(LOG_TAG, "Saving to db ($repeatCount iteration)")
                                            fillAccounts(accounts)

                                            saveToDb(messages, accounts)

                                            postValue(Result.success(false))
                                        }

                                    } else {
                                        val msg = response.errorBody()?.string()
                                        val errorMsg = if (msg.isNullOrEmpty()) response.message() else msg
                                        postValue(Result.error(errorMsg ?: "unknown error"))
                                    }
                                } catch (e: Exception) {
                                    Log.d(LOG_TAG, Log.getStackTraceString(e))
                                    postValue(Result.error(e.message ?: "unknown error"))
                                }

                            } while (loadedCount > 0 && repeatCount < pages)
                        }.await()

                    }
                }
            }
        }
    }

    private fun processMessage(
        message: Status,
        messages: MutableList<MessageEntity>,
        accounts: MutableMap<String, ChatAccountEntity?>
    ) {

        //api returns mention for current user too, so excluding it
        val mentions = message.mentions.filter { it.id != accountManager.activeAccount?.accountId }

        if (mentions.isEmpty()) {
            //for example message is marked as direct but there is no correct @user mentions
            Log.d(LOG_TAG, "Skipping insert to db, no mentions for message ${message.id}")
            return
        }

        val currentUserIsAuthor = message.account.id == accountManager.activeAccount?.accountId

        //TODO what to do if user mentioned two user in one message ("@user1 @user2 message")? use only the first one for now
        val userId = if (currentUserIsAuthor) mentions.first().id else message.account.id
        val username = if (currentUserIsAuthor) mentions.first().username else message.account.username

        //api doesn't return all the required account data in mentions, so we need to fetch missing account later
        if (!currentUserIsAuthor) {
            if (!accounts.contains(message.account.id) || accounts[message.account.id] == null) {
                accounts[message.account.id] = accountToChatAccount(message.account)
            }
        } else {
            if (!accounts.contains(mentions.first().id)) {
                accounts[mentions.first().id!!] = null
            }
        }

        if (userId.isNullOrEmpty() || username.isNullOrEmpty()) {
            Log.d(
                LOG_TAG,
                "Skipping insert to db because of empty data: userId = $userId username = $username isAuthor = $currentUserIsAuthor"
            )
            return
        }

        Log.d(LOG_TAG, "$currentUserIsAuthor $userId $username")

        messages.add(
            MessageEntity(
                message.id,
                message.content,
                userId,
                username,
                currentUserIsAuthor,
                message.createdAt,
                mentions.toTypedArray(),
                message.attachments,
                message.emojis
            )
        )
    }

    /**
     * Api doesn't return all the required account data in mentions, so we need to fetch missing account data to show
     * chat correctly
     */
    private fun fillAccounts(accounts: MutableMap<String, ChatAccountEntity?>) {
        for (accountEntry in accounts.filter { it.value == null }) {
            try {
                val response = restApi.account(accountEntry.key).execute()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        accounts[accountEntry.key] = accountToChatAccount(body)
                    }
                } else {
                    val msg = response.errorBody()?.string()
                    val errorMsg = if (msg.isNullOrEmpty()) response.message() else msg
                    Log.d(LOG_TAG, "Error during fetching account $errorMsg")
                }
            } catch (e: Exception) {
                Log.d(LOG_TAG, Log.getStackTraceString(e))
            }
        }
    }

    private fun saveToDb(messages: MutableList<MessageEntity>, accounts: MutableMap<String, ChatAccountEntity?>) {
        Log.d(LOG_TAG, "Saving ${accounts.size} accounts and ${messages.size} messages")
        db.runInTransaction {
            accounts.forEach {
                if (it.value != null) {
                    db.chatAccountDao().insert(it.value!!)
                }
            }
            messages.forEach { db.messageDao().insert(it) }
        }
    }

    private fun accountToChatAccount(account: Account): ChatAccountEntity {
        return ChatAccountEntity(
            account.id,
            account.username,
            account.displayName,
            account.avatar
        )
    }
}