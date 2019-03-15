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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tech.bigfig.romachat.data.api.RestApi
import tech.bigfig.romachat.data.db.AccountManager
import tech.bigfig.romachat.data.entity.AccessToken
import tech.bigfig.romachat.data.entity.Account
import tech.bigfig.romachat.data.entity.AppCredentials
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class Repository @Inject constructor(
    private val restApi: RestApi, private val accountManager: AccountManager
) {

    fun isLoggedIn(): LiveData<Boolean> {
        val res = MutableLiveData<Boolean>()
        res.postValue(accountManager.activeAccount != null)
        return res
    }

    fun addNewAccount(accessToken: String, domain: String) {
        accountManager.addAccount(accessToken, domain)
    }

    fun updateAccount(account: Account) {
        accountManager.updateActiveAccount(account)
    }

    fun login(
        domain: String,
        clientName: String,
        redirectUri: String,
        oauthScopes: String,
        website: String
    ): LiveData<Result<AppCredentials>> {
        return request(
            restApi.authenticateApp(domain, clientName, redirectUri, oauthScopes, website)
        ) { it }
    }

    fun fetchOAuthToken(
        domain: String,
        clientName: String,
        clientSecret: String,
        redirectUri: String,
        code: String
    ): LiveData<Result<AccessToken?>> {
        return request(
            restApi.fetchOAuthToken(domain, clientName, clientSecret, redirectUri, code, "authorization_code")
        ) { it }
    }

    fun verifyAccount(): LiveData<Result<Account?>> {
        return request(restApi.accountVerifyCredentials())
        { it }
    }
}

private fun <T, R> request(call: Call<T>, transform: (T) -> R): LiveData<Result<R>> {

    return object : LiveData<Result<R>>() {
        private var started = AtomicBoolean(false)
        override fun onActive() {
            super.onActive()
            if (started.compareAndSet(false, true)) {
                call.enqueue(object : Callback<T> {
                    override fun onResponse(call: Call<T>, response: Response<T>) {
                        if (response.body() != null) {
                            postValue(Result.success(transform((response.body()!!))))
                        } else {
                            postValue(Result.success(null))
                        }
                    }

                    override fun onFailure(call: Call<T>, throwable: Throwable) {
                        postValue(Result.error(throwable))
                    }
                })
            }
        }
    }
}