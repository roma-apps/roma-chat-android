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
import javax.inject.Inject

class StatusRepository @Inject constructor(private val restApi: RestApi) {

    fun favorite(statusId: String) = apiCallToLiveData(restApi.favoriteStatus(statusId)) { it }

    fun unfavorite(statusId: String) = apiCallToLiveData(restApi.unfavoriteStatus(statusId)) { it }

    fun reblog(statusId: String) = apiCallToLiveData(restApi.reblogStatus(statusId)) { it }

    fun unreblog(statusId: String) = apiCallToLiveData(restApi.unreblogStatus(statusId)) { it }

    fun getContext(statusId: String) = apiCallToLiveData(restApi.statusContext(statusId)) { it }
}