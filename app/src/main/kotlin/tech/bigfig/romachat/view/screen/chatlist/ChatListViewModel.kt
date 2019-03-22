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

package tech.bigfig.romachat.view.screen.chatlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tech.bigfig.romachat.data.ChatRepository
import tech.bigfig.romachat.data.Result
import tech.bigfig.romachat.data.entity.Account
import javax.inject.Inject

class ChatListViewModel @Inject constructor(repository: ChatRepository) : ViewModel() {

    //TODO known issue: app is loading 40 following accounts, need to add fetching next 40 items after scroll to bottom

    private val loadData = MutableLiveData<Boolean>()

    fun loadData() {
        loadData.value = true
    }

    val friendList: LiveData<Result<List<Account>>> = Transformations.switchMap(loadData) {
        repository.getFollowingUsers()
    }
}
