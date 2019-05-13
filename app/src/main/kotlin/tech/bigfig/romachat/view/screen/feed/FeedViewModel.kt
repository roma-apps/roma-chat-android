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

    private val loadData = MutableLiveData<Boolean>()

    fun loadData() {
        loadData.value = true
    }

    private var loadPostsInstance: LiveData<Result<List<Status>>>? = null
    val loadPosts: LiveData<Result<List<Status>>>
        get() {
            if (loadPostsInstance == null) {
                loadPostsInstance = when (feedType) {
                    FeedType.HOME -> repository.getHomeFeed()
                    FeedType.ALL -> repository.getAllFeed()
                    FeedType.ME -> repository.getUserFeed()
                    else -> repository.getHomeFeed()
                }
            }
            return loadPostsInstance!!
        }

    val posts: LiveData<List<Status>?> = Transformations.switchMap(loadData) {
        Transformations.map(loadPosts) { result ->
            when (result.status) {
                ResultStatus.LOADING ->
                    null

                ResultStatus.SUCCESS ->
                    result.data

                ResultStatus.ERROR -> {
                    null
                }
            }
        }
    }
}
