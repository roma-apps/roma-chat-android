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
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.ChatRepository
import javax.inject.Inject

/**
 * Service to init fetching/storing chat messages.
 *
 * Should be removed and replaced with direct api calls when api is ready.
 */
class ChatMessagesService : LifecycleService() {

    @Inject
    lateinit var repository: ChatRepository

    override fun onCreate() {
        super.onCreate()

        App.getApplication(this).appComponent.inject(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val mode = intent.getSerializableExtra(EXTRA_MODE) as FetchMode
        Log.d(LOG_TAG, "onStartCommand fetchMessages $mode")
        fetchMessages(mode)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun fetchMessages(mode: FetchMode) {
        repository.storeMessages(mode.pages).observe(this, Observer { result ->
            Log.d(LOG_TAG, "fetchMessages ${result.status} ${result.data}")
        })
    }

    companion object {

        @JvmStatic
        fun startFetchingAllMessages(context: Context) {
            val intent = Intent(context, ChatMessagesService::class.java)
                .putExtra(EXTRA_MODE, FetchMode.ALL)
            context.startService(intent)
        }

        @JvmStatic
        fun startFetchingLastMessages(context: Context) {
            val intent = Intent(context, ChatMessagesService::class.java)
                .putExtra(EXTRA_MODE, FetchMode.LAST)
            context.startService(intent)
        }

        private const val LOG_TAG = "ChatMessagesService"

        private const val EXTRA_MODE = "EXTRA_MODE"
    }

    private enum class FetchMode(val pages: Int) {
        ALL(10),//max 10 repeats of fetching n messages
        LAST(1);
    }
}
