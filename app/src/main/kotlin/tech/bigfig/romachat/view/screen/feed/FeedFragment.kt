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


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.ActivityNavigator
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.paginate.Paginate
import tech.bigfig.romachat.NavGraphDirections
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.entity.Attachment
import tech.bigfig.romachat.data.entity.Media
import tech.bigfig.romachat.data.entity.MediaType
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.databinding.FragmentFeedBinding
import tech.bigfig.romachat.utils.OpenLinkHelper
import tech.bigfig.romachat.view.screen.profile.ProfileFragment
import tech.bigfig.romachat.view.utils.RetryListener
import timber.log.Timber
import javax.inject.Inject


class FeedFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentFeedBinding
    private lateinit var viewModel: FeedViewModel
    private lateinit var adapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(FeedViewModel::class.java)

        viewModel.feedType =
            arguments?.getSerializable(ARG_TYPE) as FeedType? ?: throw IllegalArgumentException("Empty type")
        viewModel.hashTag = arguments?.getString(ARG_HASHTAG)

        viewModel.posts.observe(this, Observer { posts ->
            if (posts != null) {
                Timber.d("showing ${posts.size} posts")

                adapter.setItems(posts)

                processPagination(posts.size)
            }
        })

        viewModel.loadData()

        binding = FragmentFeedBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.retryListener = object : RetryListener {
            override fun onRetry() {
                viewModel.loadData()
            }
        }

        adapter = FeedAdapter(adapterListener)

        binding.feedList.layoutManager = LinearLayoutManager(context)
        binding.feedList.adapter = adapter

        return binding.root
    }

    private val adapterListener = object : FeedAdapter.FeedAdapterListener {
        override fun onMediaClick(status: Status, mediaIndex: Int, view: View) {
            if (status.attachments.isEmpty()) return

            val attachments = status.attachments.map { attachment ->
                Media(
                    attachment.url, when (attachment.type) {
                        Attachment.Type.IMAGE -> MediaType.IMAGE
                        Attachment.Type.VIDEO,
                        Attachment.Type.GIFV -> MediaType.VIDEO
                        else -> {
                            Timber.d("Unknown media type: ${attachment.type}")
                            return
                        }
                    }
                )
            }

            val currentMediaUrl: String = status.attachments[mediaIndex].url
            ViewCompat.setTransitionName(view, currentMediaUrl)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity!!, view, currentMediaUrl)
            val extras = ActivityNavigator.Extras.Builder().setActivityOptions(options).build()
            view.findNavController()
                .navigate(
                    NavGraphDirections.actionGlobalViewMediaActivity(attachments.toTypedArray(), mediaIndex),
                    extras
                )
        }

        override fun onTagClick(tag: String) {
            findNavController().navigate(NavGraphDirections.actionGlobalHashTagFragment(tag))
        }

        override fun onAccountClick(id: String) {
            ProfileFragment.newInstance(id).show(childFragmentManager, "dialog")
        }

        override fun onUrlClick(url: String) {
            if (activity != null) {
                OpenLinkHelper.openLink(url, activity!!)
            }
        }

        override fun onClick() {
        }

        override fun onLongClick() {
        }
    }

    //-----------
    // PAGINATION

    private var paginate: Paginate? = null
    private var prevTotalSize = 0
    private var loadingMore = false

    private fun processPagination(totalSize: Int) {
        if (paginate == null) {
            // Init pagination after first part of data is loaded
            paginate = Paginate.with(binding.feedList, paginateCallbacks)
                .setLoadingTriggerThreshold(5)
                .addLoadingListItem(true)
                .build()

            return
        }

        loadingMore = false
        paginate?.setHasMoreDataToLoad(prevTotalSize < totalSize) // Stop showing loader if there is no new data
        prevTotalSize = totalSize
    }

    private val paginateCallbacks = object : Paginate.Callbacks {
        override fun onLoadMore() {
            // Load next page of data (e.g. network or database)
            Timber.d("onLoadMore")
            if (loadingMore) return
            loadingMore = true
            viewModel.loadData()
        }

        override fun isLoading(): Boolean {
            // Indicate whether new page loading is in progress or not
            return loadingMore
        }

        override fun hasLoadedAllItems(): Boolean {
            // Indicate whether all data (pages) are loaded or not
            return false
        }
    }

    companion object {

        fun newInstance(feedType: FeedType): FeedFragment =
            FeedFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_TYPE, feedType)
                }
            }

        fun newInstanceHashTag(hashTag: String): FeedFragment =
            FeedFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_TYPE, FeedType.HASHTAG)
                    putString(ARG_HASHTAG, hashTag)
                }
            }

        private const val ARG_TYPE = "ARG_TYPE"
        private const val ARG_HASHTAG = "ARG_HASHTAG"
    }
}