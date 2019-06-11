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
import tech.bigfig.romachat.data.FeedRepository
import tech.bigfig.romachat.data.Result
import tech.bigfig.romachat.data.ResultStatus
import tech.bigfig.romachat.data.StatusRepository
import tech.bigfig.romachat.data.entity.Status
import timber.log.Timber
import javax.inject.Inject

class FeedViewModel @Inject constructor(
    private val repository: FeedRepository,
    private val statusRepository: StatusRepository
) :
    ViewModel() {

    var feedType: FeedType? = null
    var hashTag: String? = null
    var accountId: String? = null

    // Show loader/error/list of items depending on api response only for first page,
    // for next pages recyclerview loader will be shown
    val firstPageStatus: MutableLiveData<Result<Unit>> = MutableLiveData()
    private var firstTimeLoading = true

    private val loadData = MutableLiveData<Boolean>()

    private val postList = mutableListOf<FeedViewData>()

    val errorToShow: MutableLiveData<Int?> = MutableLiveData()

    fun loadData() {
        if (firstPageStatus.value?.status != ResultStatus.SUCCESS)
            loadData.value = true
    }

    fun loadMore() {
        loadData.value = true
    }

    fun reloadData() {
        postList.clear()
        loadData.value = true
    }

    private fun getFeed(): LiveData<Result<List<Status>>> {
        return when (feedType) {
            FeedType.HOME -> repository.getHomeFeed(getLastPostId())
            FeedType.ALL -> repository.getAllFeed(getLastPostId())
            FeedType.ACCOUNT -> if (accountId == null) repository.getCurrentUserFeed(getLastPostId())
            else repository.getUserFeed(
                accountId ?: throw IllegalStateException("Empty accountId"),
                getLastPostId()
            )
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

    val posts: LiveData<List<FeedViewData>?> = Transformations.switchMap(loadData) {
        Transformations.map(getFeed()) { result ->
            when (result.status) {
                ResultStatus.LOADING -> {
                    if (firstTimeLoading) {
                        firstPageStatus.postValue(Result.loading())
                    }
                    errorToShow.postValue(null)
                    null
                }

                ResultStatus.SUCCESS -> {

                    firstTimeLoading = false
                    firstPageStatus.postValue(Result.success(null))
                    errorToShow.postValue(null)

                    if (result.data != null) {
                        postList.addAll(result.data.map { status -> status.toViewData() })
                    }

                    // ListAdapter requires new copy of the list
                    postList.filter { true }
                }

                ResultStatus.ERROR -> {
                    if (firstTimeLoading) {
                        firstPageStatus.postValue(Result.error(result.error ?: ""))
                    } else {
                        errorToShow.postValue(R.string.feed_error_load)
                    }
                    null
                }
            }
        }
    }

    private fun Status.toViewData(): FeedViewData {
        return FeedViewData(
            this.id,
            this.reblog != null,
            this.account,
            reblog ?: this
        )
    }

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
                            postList.forEach {post->
                                if (post.status.id == feed.status.id) {
                                    post.status = result.data
                                    return@map post
                                }
                            }
                            null
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
                            postList.forEach {post->
                                if (post.status.id == feed.status.id ) {
                                    //original post which was reposted is in "reblog" field of response
                                    if (result.data.reblog!=null) {
                                        post.status = result.data.reblog
                                    } else {
                                        //TODO check the case when user unrepost from his own feed (in this case post should disappear?)
                                        post.status = result.data
                                    }
                                    return@map post
                                }
                            }
                            null
                        } else {
                            null
                        }
                    }

                    ResultStatus.ERROR -> {
                        Timber.d("reblog error ${result.error}")
                        errorToShow.postValue(R.string.feed_error_update)
                        null
                    }
                }
            }
        }
}
