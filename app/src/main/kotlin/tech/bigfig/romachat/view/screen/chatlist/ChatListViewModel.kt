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

import android.app.Application
import androidx.lifecycle.*
import tech.bigfig.romachat.data.Repository
import tech.bigfig.romachat.data.Result
import tech.bigfig.romachat.data.entity.Account

class ChatListViewModel(application: Application, private val repository: Repository) : AndroidViewModel(application) {

    //TODO known issue: app is loading 40 following accounts, need to add fetching next 40 items after scroll to bottom

    private val loadData = MutableLiveData<Boolean>()

    fun loadData() {
        loadData.value = true
    }

    val friendList: LiveData<Result<List<Account>>> = Transformations.switchMap(loadData) {
        repository.getFollowingUsers()
    }

    class ModelFactory(private val application: Application, private val repository: Repository) :
        ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass == ChatListViewModel::class.java) {
                ChatListViewModel(application, repository) as T
            } else throw IllegalArgumentException("Wrong view model class")
        }
    }
}
