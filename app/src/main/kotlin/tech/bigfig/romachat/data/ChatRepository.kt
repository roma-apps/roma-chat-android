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

import android.util.Log
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import tech.bigfig.romachat.data.api.RestApi
import tech.bigfig.romachat.data.api.apiCallToLiveData
import tech.bigfig.romachat.data.db.AccountManager
import tech.bigfig.romachat.data.db.AppDatabase
import tech.bigfig.romachat.data.db.entity.MessageEntity
import tech.bigfig.romachat.data.entity.Account
import tech.bigfig.romachat.data.entity.Status
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val MESSAGES_AMOUNT = 40//max available amount
private const val LOG_TAG = "ChatRepository"

class ChatRepository @Inject constructor(
    private val restApi: RestApi, private val accountManager: AccountManager, private val db: AppDatabase
) {

    fun getFollowingUsers(): LiveData<Result<List<Account>>> {
        return apiCallToLiveData(restApi.accountFollowing(accountManager.activeAccount?.accountId ?: "", "")) { it }
    }

    /**
     * Load the messages from api and store them to db.
     *
     * Current api restrictions don't allow to get the concrete chat messages directly. So we agreed to group the messages
     * by chats on the client-side
     */
    fun storeMessages(): LiveData<Result<Boolean>> {

        return object : LiveData<Result<Boolean>>() {
            private var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    postValue(Result.loading())

                    var lastId: String? = null
                    var loadedCount = Int.MAX_VALUE

                    GlobalScope.launch(Dispatchers.Main) {
                        async(Dispatchers.IO) {
                            do {
                                try {
                                    val response = restApi.directTimeline(lastId, null, MESSAGES_AMOUNT).execute()
                                    if (response.isSuccessful) {
                                        val body = response.body()

                                        loadedCount = body?.size ?: 0
                                        Log.d(LOG_TAG, "Loaded messages: $loadedCount")

                                        if (loadedCount > 0) {
                                            lastId = body?.last()?.id
                                            Log.d(LOG_TAG, "Last message id: $lastId")

                                            body?.forEach { message -> insertToDb(message) }

                                            postValue(Result.success(true))
                                        } else {
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
                            } while (loadedCount > 0)
                        }.await()

                    }
                }
            }
        }
    }

    private fun insertToDb(message: Status) {

        //api returns mention for current user too, so excluding it
        val mentions = message.mentions.filter { it.id != accountManager.activeAccount?.accountId }

        if (mentions.isEmpty()) {
            //for example message is marked as direct but there is no correct @user mentions
            Log.d(LOG_TAG, "Skipping insert to db, no mentions for message ${message.id}")
            return
        }

        val currentUserIsAuthor = message.account.id == accountManager.activeAccount?.accountId

        //TODO what to do if user mentioned two user in one message ("@user1 @user2 message")? use only the first one for now
        val userId = if (currentUserIsAuthor) message.mentions.first().id else message.account.id
        val username = if (currentUserIsAuthor) message.mentions.first().username else message.account.displayName

        if (userId.isNullOrEmpty() || username.isNullOrEmpty()) {
            Log.d(
                LOG_TAG,
                "Skipping insert to db because of empty data: userId = $userId username = $username isAuthor = $currentUserIsAuthor"
            )
            return
        }

        val res = MessageEntity(
            message.id,
            message.content.toString(),
            userId,
            username,
            currentUserIsAuthor,
            message.createdAt.time
        )

        db.messageDao().insert(res)
    }
}