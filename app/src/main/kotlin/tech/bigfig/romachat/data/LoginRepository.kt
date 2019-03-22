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
import tech.bigfig.romachat.data.api.RestApi
import tech.bigfig.romachat.data.api.apiCallToLiveData
import tech.bigfig.romachat.data.db.AccountManager
import tech.bigfig.romachat.data.entity.AccessToken
import tech.bigfig.romachat.data.entity.Account
import tech.bigfig.romachat.data.entity.AppCredentials
import javax.inject.Inject

class LoginRepository @Inject constructor(
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
        return apiCallToLiveData(
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
        return apiCallToLiveData(
            restApi.fetchOAuthToken(domain, clientName, clientSecret, redirectUri, code, "authorization_code")
        ) { it }
    }

    fun verifyAccount(): LiveData<Result<Account?>> {
        return apiCallToLiveData(restApi.accountVerifyCredentials())
        { it }
    }
}