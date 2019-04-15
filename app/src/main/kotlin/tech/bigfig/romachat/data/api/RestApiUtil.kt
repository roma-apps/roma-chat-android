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

package tech.bigfig.romachat.data.api

import android.util.Log
import androidx.lifecycle.LiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tech.bigfig.romachat.data.Pagination
import tech.bigfig.romachat.data.Result
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

fun <T, R> apiCallToLiveData(call: Call<T>, transform: (T) -> R): LiveData<Result<R>> {

    return object : LiveData<Result<R>>() {
        private var started = AtomicBoolean(false)
        override fun onActive() {
            super.onActive()
            if (started.compareAndSet(false, true)) {
                postValue(Result.loading())
                call.enqueue(object : Callback<T> {
                    override fun onResponse(call: Call<T>, response: Response<T>) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body == null || response.code() == 204) {
                                postValue(Result.success(null))
                            } else {
                                var pagination: Pagination? = null
                                val linkHeader = response.headers().get("link")
                                if (!linkHeader.isNullOrEmpty()) {
                                    val links = HttpHeaderLink.parse(linkHeader)
                                    pagination = Pagination(
                                        HttpHeaderLink.findByRelationType(links, "prev")?.uri,
                                        HttpHeaderLink.findByRelationType(links, "next")?.uri
                                    )
                                }

                                postValue(Result.success(transform(response.body()!!), pagination))
                            }
                        } else {
                            val msg = response.errorBody()?.string()
                            val errorMsg = if (msg.isNullOrEmpty()) response.message() else msg
                            postValue(Result.error(errorMsg ?: "unknown error"))
                        }
                    }

                    override fun onFailure(call: Call<T>, throwable: Throwable) {
                        Timber.d( Log.getStackTraceString(throwable))
                        postValue(Result.error(throwable.message ?: "api call onFailure()"))
                    }
                })
            }
        }
    }
}