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

import tech.bigfig.romachat.data.api.RestApi
import tech.bigfig.romachat.data.api.apiCallToLiveData
import tech.bigfig.romachat.data.db.AccountManager
import tech.bigfig.romachat.data.db.entity.AccountEntity
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val restApi: RestApi, private val accountManager: AccountManager
) {

    fun getCurrentAccount(): AccountEntity? {
        return accountManager.activeAccount
    }

    fun getAccount(accountId: String) = apiCallToLiveData(restApi.account(accountId)) { it }

    fun search(query: String) = apiCallToLiveData(restApi.search(query, true)) { it }

    fun getFollowing() =
        apiCallToLiveData(
            restApi.accountFollowing(
                accountManager.activeAccount?.accountId ?: throw IllegalStateException("Active account is null"), null
            )
        ) { it }

    fun follow(accountId: String) = apiCallToLiveData(restApi.followAccount(accountId, true)) { it }
}