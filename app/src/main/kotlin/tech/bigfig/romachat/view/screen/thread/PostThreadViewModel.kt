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

package tech.bigfig.romachat.view.screen.thread

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.Result
import tech.bigfig.romachat.data.ResultStatus
import tech.bigfig.romachat.data.StatusRepository
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.view.screen.feed.FeedViewData
import tech.bigfig.romachat.view.screen.feed.convertStatusToViewData
import javax.inject.Inject

class PostThreadViewModel @Inject constructor(
    private val repository: StatusRepository
) :
    ViewModel() {

    var status: Status? = null

    val firstTimeStatus: MutableLiveData<Result<Unit>> = MutableLiveData()
    private var firstTimeLoading = true

    private val loadData = MutableLiveData<Boolean>()

    private val postList = mutableListOf<FeedViewData>()

    val errorToShow: MutableLiveData<Int?> = MutableLiveData()

    fun loadData() {
        if (firstTimeStatus.value?.status != ResultStatus.SUCCESS)
            loadData.value = true
    }

    fun reloadData() {
        postList.clear()
        loadData.value = true
    }

    val posts: LiveData<List<FeedViewData>?> = Transformations.switchMap(loadData) {
        Transformations.map(
            repository.getContext(status?.id ?: throw IllegalStateException("Status is empty"))
        ) { result ->
            when (result.status) {
                ResultStatus.LOADING -> {
                    if (firstTimeLoading) {
                        firstTimeStatus.postValue(Result.loading())
                    }
                    errorToShow.postValue(null)
                    null
                }

                ResultStatus.SUCCESS -> {

                    firstTimeLoading = false
                    firstTimeStatus.postValue(Result.success(null))
                    errorToShow.postValue(null)

                    if (result.data != null) {
                        postList.addAll(0, result.data.descendants.map { status -> convertStatusToViewData(status) })
                        postList.add(
                            0,
                            convertStatusToViewData(status ?: throw IllegalStateException("Status is empty"))
                        )
                        postList.addAll(0, result.data.ancestors.map { status -> convertStatusToViewData(status) })
                    }

                    // ListAdapter requires new copy of the list
                    postList.filter { true }
                }

                ResultStatus.ERROR -> {
                    if (firstTimeLoading) {
                        firstTimeStatus.postValue(Result.error(result.error ?: ""))
                    } else {
                        errorToShow.postValue(R.string.feed_error_load)
                    }
                    null
                }
            }
        }
    }
}
