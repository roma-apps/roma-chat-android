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

package tech.bigfig.romachat.view.screen.profile

import androidx.lifecycle.*
import tech.bigfig.romachat.data.Result
import tech.bigfig.romachat.data.ResultStatus
import tech.bigfig.romachat.data.UserRepository
import tech.bigfig.romachat.data.entity.Account
import javax.inject.Inject

class ProfileViewModel @Inject constructor(val repository: UserRepository) : ViewModel() {

    private var accountId: String? = null

    private val loadData = MutableLiveData<Boolean>()

    val showProfile = MutableLiveData<Boolean>()
    private val accountViewData = MutableLiveData<ProfileViewData?>()

    fun loadData() {
        loadData.value = true
    }

    fun initData(accountId: String, account: Account?) {
        if (account != null) {// We are from search results, all data is ready
            accountViewData.postValue(convertAccountToViewData(account))
            showProfile.postValue(true)
        } else if (accountId.isNotEmpty()) { //We know only user Id, load all the rest
            this.accountId = accountId
            loadData.value = true
        } else {
            throw IllegalArgumentException("Both userId and searchResults are empty")
        }
    }

    val userCall: LiveData<Result<Account>> = Transformations.switchMap(loadData) {
        Transformations.map(
            repository.getAccount(
                accountId ?: throw IllegalStateException("User id is empty")
            )
        ) { it }
    }

    val user: LiveData<ProfileViewData?> = MediatorLiveData<ProfileViewData?>().apply {
        addSource(Transformations.map(userCall) { result ->
            when (result.status) {
                ResultStatus.LOADING -> {
                    showProfile.postValue(false)
                    null
                }

                ResultStatus.SUCCESS -> {
                    if (result.data != null) {
                        showProfile.postValue(true)
                        convertAccountToViewData(result.data)
                    } else {
                        showProfile.postValue(false)
                        null
                    }
                }

                ResultStatus.ERROR -> {
                    showProfile.postValue(false)
                    null
                }
            }
        }) { data -> this.postValue(data) }

        addSource(accountViewData) { data -> this.postValue(data) }
    }

    private fun convertAccountToViewData(account: Account): ProfileViewData {
        return ProfileViewData(
            account.id,
            account.username,
            account.localUsername,
            account.displayName,
            account.avatar,
            account.note.toString()
        )
    }

}
