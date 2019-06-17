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
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.ResultStatus
import tech.bigfig.romachat.data.StatusRepository
import timber.log.Timber
import javax.inject.Inject

class FeedActionsViewModel @Inject constructor(
    private val statusRepository: StatusRepository
) : ViewModel() {

    val errorToShow: MutableLiveData<Int?> = MutableLiveData()

    fun favorite(feedViewData: FeedViewData) {
        Timber.d("favorite ${feedViewData.status.id}")
        statusToFavorite.postValue(feedViewData)
    }

    private val statusToFavorite = MutableLiveData<FeedViewData>()
    val favorite: LiveData<FeedViewData?> =
        Transformations.switchMap(statusToFavorite) { feed ->
            Transformations.map(
                if (feed.status.favourited) statusRepository.unfavorite(feed.status.id)
                else statusRepository.favorite(feed.status.id)
            ) { result ->
                when (result.status) {
                    ResultStatus.LOADING -> {
                        null
                    }

                    ResultStatus.SUCCESS -> {
                        if (result.data != null) {
                            convertStatusToViewData(result.data)
                        } else {
                            null
                        }
                    }

                    ResultStatus.ERROR -> {
                        Timber.d("favorite error ${result.error}")
                        errorToShow.postValue(R.string.feed_error_update)
                        null
                    }
                }
            }
        }

    fun repost(feedViewData: FeedViewData) {
        Timber.d("repost ${feedViewData.status.id}")
        statusToRepost.postValue(feedViewData)
    }

    private val statusToRepost = MutableLiveData<FeedViewData>()
    val repost: LiveData<FeedViewData?> =
        Transformations.switchMap(statusToRepost) { feed ->
            Transformations.map(
                if (feed.status.reblogged) statusRepository.unreblog(feed.status.id)
                else statusRepository.reblog(feed.status.id)
            ) { result ->
                when (result.status) {
                    ResultStatus.LOADING -> {
                        null
                    }

                    ResultStatus.SUCCESS -> {
                        if (result.data != null) {
                            //original post which was reposted is in "reblog" field of response
                            convertStatusToViewData(
                                result.data.reblog ?: result.data
                            )
                        } else {
                            null
                        }
                    }

                    ResultStatus.ERROR -> {
                        Timber.d("repost error ${result.error}")
                        errorToShow.postValue(R.string.feed_error_update)
                        null
                    }
                }
            }
        }
}