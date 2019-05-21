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

package tech.bigfig.romachat.view.screen.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tech.bigfig.romachat.data.FeedRepository
import tech.bigfig.romachat.data.Result
import tech.bigfig.romachat.data.ResultStatus
import tech.bigfig.romachat.data.entity.Status
import javax.inject.Inject

class FeedViewModel @Inject constructor(val repository: FeedRepository) : ViewModel() {

    var feedType: FeedType? = null
    var hashTag: String? = null

    // Show loader/error/list of items depending on api response only for first page,
    // for next pages recyclerview loader will be shown
    val firstPageStatus: MutableLiveData<Result<Unit>> = MutableLiveData()
    private var firstTimeLoading = true

    private val loadData = MutableLiveData<Boolean>()

    private val postList = mutableListOf<Status>()

    fun loadData() {
        loadData.value = true
    }

    private fun getFeed(): LiveData<Result<List<Status>>> {
        return when (feedType) {
            FeedType.HOME -> repository.getHomeFeed(getLastPostId())
            FeedType.ALL -> repository.getAllFeed(getLastPostId())
            FeedType.ME -> repository.getUserFeed(getLastPostId())
            FeedType.HASHTAG -> repository.getHashFeed(
                hashTag ?: throw IllegalStateException("Empty hashtag"),
                getLastPostId()
            )
            else -> repository.getHomeFeed(getLastPostId())
        }
    }

    private fun getLastPostId(): String? {
        return postList.lastOrNull()?.id
    }

    val posts: LiveData<List<Status>?> = Transformations.switchMap(loadData) {
        Transformations.map(getFeed()) { result ->
            when (result.status) {
                ResultStatus.LOADING -> {
                    if (firstTimeLoading) {
                        firstPageStatus.postValue(Result.loading())
                    }
                    null
                }

                ResultStatus.SUCCESS -> {

                    firstTimeLoading = false
                    firstPageStatus.postValue(Result.success(null))

                    if (result.data != null) {
                        postList.addAll(result.data)
                    }

                    // ListAdapter requires new copy of the list
                    postList.filter { true }
                }

                ResultStatus.ERROR -> {
                    if (firstTimeLoading) {
                        firstPageStatus.postValue(Result.error(result.error ?: ""))
                    }
                    null
                }
            }
        }
    }
}
