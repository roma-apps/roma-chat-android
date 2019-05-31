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
import tech.bigfig.romachat.data.entity.Relationship
import tech.bigfig.romachat.view.screen.search.AddUserStatus
import timber.log.Timber
import javax.inject.Inject

class ProfileViewModel @Inject constructor(val repository: UserRepository) : ViewModel() {

    private var accountId: String? = null
    private var account: Account? = null
    private var currentAccount = repository.getCurrentAccount()

    private val loadData = MutableLiveData<Boolean>()

    val showProfile = MutableLiveData<Boolean>()
    private val accountViewData = MutableLiveData<ProfileViewData?>()

    var addUserStatus = MutableLiveData<AddUserStatus>()

    fun loadData() {
        loadData.value = true
    }

    fun initData(accountId: String, account: Account?) {
        when {
            account != null -> { // We are from search results, all data is ready
                if (this.account != account) {
                    this.account = account
                    accountViewData.postValue(convertAccountToViewData(account))
                    showProfile.postValue(true)
                }
            }
            accountId.isNotEmpty() -> //We know only user Id, load all the rest
                if (this.accountId != accountId) {
                    this.accountId = accountId
                    loadData.value = true
                }
            else -> throw IllegalArgumentException("Both accountId and account are empty")
        }
    }

    fun initDataForCurrentUser() {
        if (accountId == null) {
            accountId = currentAccount?.accountId ?: throw IllegalStateException("Current user is null")
            loadData.value = true
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

    val relationship: LiveData<Relationship?> = Transformations.switchMap(user) { profile ->
        if (profile != null) {
            Transformations.map(repository.getRelationship(profile.id)) { result ->
                when (result.status) {
                    ResultStatus.LOADING -> {
                        null
                    }

                    ResultStatus.SUCCESS -> {
                        showProfile.postValue(true)

                        if (result.data != null) {
                            val relationship = result.data.firstOrNull()
                            if (relationship == null) null
                            else {
                                addUserStatus.postValue(relationship.getStatus())
                                relationship
                            }
                        } else {
                            addUserStatus.postValue(AddUserStatus.NOT_ADDED)
                            null
                        }
                    }

                    ResultStatus.ERROR -> {
                        showProfile.postValue(true)
                        Timber.d("relationship error ${result.error}")
                        null
                    }
                }
            }
        } else null
    }

    fun addOrRemoveUser() {
        when (addUserStatus.value) {
            AddUserStatus.ADDING -> return
            AddUserStatus.NOT_ADDED -> accountToAdd.value = accountId
            AddUserStatus.ADDED -> accountToRemove.value = accountId
        }
    }

    private val accountToAdd = MutableLiveData<String>()
    val addUser: LiveData<Unit> =
        Transformations.switchMap(accountToAdd) { accountId ->
            Transformations.map(repository.follow(accountId)) { result ->
                Timber.d("follow ${result.status}")
                when (result.status) {
                    ResultStatus.LOADING ->
                        addUserStatus.postValue(AddUserStatus.ADDING)

                    ResultStatus.SUCCESS ->
                        addUserStatus.postValue(if (result.data != null) result.data.getStatus() else AddUserStatus.NOT_ADDED)

                    ResultStatus.ERROR -> {
                        Timber.d("follow error ${result.error}")
                        addUserStatus.postValue(AddUserStatus.NOT_ADDED)
                    }
                }
            }
        }

    private val accountToRemove = MutableLiveData<String>()
    val removeUser: LiveData<Unit> =
        Transformations.switchMap(accountToRemove) { accountId ->
            Transformations.map(repository.unfollow(accountId)) { result ->
                Timber.d("unfollow ${result.status}")
                when (result.status) {
                    ResultStatus.LOADING ->
                        addUserStatus.postValue(AddUserStatus.ADDING)

                    ResultStatus.SUCCESS ->
                        addUserStatus.postValue(if (result.data != null) result.data.getStatus() else AddUserStatus.ADDED)

                    ResultStatus.ERROR -> {
                        Timber.d("unfollow error ${result.error}")
                        addUserStatus.postValue(AddUserStatus.ADDED)
                    }
                }
            }
        }

    private fun convertAccountToViewData(account: Account): ProfileViewData {
        return ProfileViewData(
            account.id,
            account.username,
            account.localUsername,
            account.displayName,
            account.avatar,
            account.note.toString(),
            account.id == currentAccount?.accountId
        )
    }

    private fun Relationship.getStatus(): AddUserStatus {
        return if (this.following) AddUserStatus.ADDED else AddUserStatus.NOT_ADDED
    }

}
