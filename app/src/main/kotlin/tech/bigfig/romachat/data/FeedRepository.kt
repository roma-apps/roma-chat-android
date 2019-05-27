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
import javax.inject.Inject

class FeedRepository @Inject constructor(
    private val restApi: RestApi, private val accountManager: AccountManager
) {

    fun getHomeFeed(fromId: String?) = apiCallToLiveData(restApi.homeTimeline(fromId, null, null)) { it }

    fun getAllFeed(fromId: String?) = apiCallToLiveData(restApi.publicTimeline(true, fromId, null, null)) { it }

    fun getCurrentUserFeed(fromId: String?) = getUserFeed(
        accountManager.activeAccount?.accountId ?: throw IllegalStateException("Active account is null"),
        fromId
    )

    fun getUserFeed(userId: String, fromId: String?) = apiCallToLiveData(
        restApi.accountStatuses(
            userId,
            fromId,
            null,
            null,
            true,
            null,
            null
        )
    ) { it }

    fun getHashFeed(hashTag: String, fromId: String?) = apiCallToLiveData(
        restApi.hashtagTimeline(
            hashTag,
            null,
            fromId,
            null,
            null
        )
    ) { it }
}