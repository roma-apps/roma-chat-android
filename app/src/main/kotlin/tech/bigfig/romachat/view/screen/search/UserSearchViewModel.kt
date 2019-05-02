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

package tech.bigfig.romachat.view.screen.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tech.bigfig.romachat.data.Result
import tech.bigfig.romachat.data.ResultStatus
import tech.bigfig.romachat.data.UserRepository
import tech.bigfig.romachat.data.entity.Account
import tech.bigfig.romachat.data.entity.SearchResults
import tech.bigfig.romachat.utils.zip
import timber.log.Timber
import javax.inject.Inject

class UserSearchViewModel @Inject constructor(repository: UserRepository) : ViewModel() {

    private var currentAccount = repository.getCurrentAccount()

    private val searchQuery = MutableLiveData<String>()

    private val noDataFound = MutableLiveData<Boolean>()

    fun doSearch(query: String) {
        Timber.d("doSearch query = $query")
        searchQuery.value = query
    }

    // Zip search results with following users to show the complete user list
    val searchCallResult: LiveData<Pair<Result<SearchResults>, Result<List<Account>>>> = Transformations.switchMap(searchQuery) {
        repository.search(it).zip(repository.getFollowing())
    }

    val searchResults: LiveData<List<UserSearchResultViewData>?> =
        Transformations.map(searchCallResult) { callResults ->
            if (callResults.first.status == ResultStatus.SUCCESS && callResults.first.data != null) {//search call status
                if (callResults.first.data!!.accounts.isEmpty()) {
                    noDataFound.postValue(true)
                    null
                } else {
                    noDataFound.postValue(false)

                    processSearchResults(callResults.first.data!!, callResults.second.data)
                }
            } else null
        }

    private fun processSearchResults(
        searchResults: SearchResults, following: List<Account>?
    ): List<UserSearchResultViewData>? {

        val followingMap = following?.associateBy({ it.id }, { it })
        return searchResults.accounts.map { account ->
            UserSearchResultViewData(
                account,
                followingMap?.containsKey(account.id) ?: false,
                account.id == currentAccount?.accountId
            )
        }
    }

    fun addUser(item: UserSearchResultViewData) {
        if (item.isAdded) return

//TODO call api to follow
    }
}
