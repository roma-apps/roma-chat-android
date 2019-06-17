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


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.ActivityNavigator
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import tech.bigfig.romachat.NavGraphDirections
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.data.entity.Attachment
import tech.bigfig.romachat.data.entity.Media
import tech.bigfig.romachat.data.entity.MediaType
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.databinding.FragmentPostThreadBinding
import tech.bigfig.romachat.utils.OpenLinkHelper
import tech.bigfig.romachat.view.screen.feed.FeedActionsViewModel
import tech.bigfig.romachat.view.screen.feed.FeedAdapter
import tech.bigfig.romachat.view.screen.feed.FeedViewData
import tech.bigfig.romachat.view.utils.RetryListener
import timber.log.Timber
import javax.inject.Inject


class PostThreadFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentPostThreadBinding
    private lateinit var viewModel: PostThreadViewModel
    private lateinit var feedActionsViewModel: FeedActionsViewModel
    private lateinit var adapter: FeedAdapter

    private val navArgs: PostThreadFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(PostThreadViewModel::class.java)
        feedActionsViewModel = ViewModelProviders.of(activity ?: throw Exception("Invalid Activity"), viewModelFactory)
            .get(FeedActionsViewModel::class.java)

        viewModel.status = navArgs.status

        viewModel.posts.observe(this, Observer { posts ->
            if (posts != null) {
                Timber.d("showing ${posts.size} posts")

                adapter.setItems(posts)

                posts.forEachIndexed { index, post ->
                    if (post.id == navArgs.status.id) {
                        binding.feedList.smoothScrollToPosition(index)
                        return@forEachIndexed
                    }
                }

                binding.swipeRefresh.isRefreshing = false
            }
        })

        viewModel.errorToShow.observe(this, Observer { messageId ->
            if (activity != null && messageId != null) {
                Toast.makeText(activity, messageId, Toast.LENGTH_LONG).show()
            }
            binding.swipeRefresh.isRefreshing = false
        })
        feedActionsViewModel.errorToShow.observe(this, Observer { messageId ->
            if (activity != null && messageId != null) {
                Toast.makeText(activity, messageId, Toast.LENGTH_LONG).show()
            }
        })

        viewModel.loadData()

        feedActionsViewModel.favorite.observe(this, Observer { action ->
            if (action != null) {
                adapter.updateItem(action)
            }
        })

        feedActionsViewModel.repost.observe(this, Observer { action ->
            if (action != null) {
                adapter.updateItem(action)
            }
        })

        binding = FragmentPostThreadBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.retryListener = object : RetryListener {
            override fun onRetry() {
                viewModel.reloadData()
            }
        }

        (activity as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            title = ""
        }
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.reloadData()
        }

        adapter = FeedAdapter(adapterListener)

        binding.feedList.layoutManager = LinearLayoutManager(context)
        binding.feedList.adapter = adapter

        //fix for image blinking https://stackoverflow.com/a/32227316/2219237
        val animator = binding.feedList.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        return binding.root
    }

    private val adapterListener = object : FeedAdapter.FeedAdapterListener {
        override fun onRepostedByClick(feedViewData: FeedViewData) {
            if (feedViewData.repostedBy != null) {
                findNavController().navigate(NavGraphDirections.actionGlobalProfileFragment(feedViewData.repostedBy.id))
            }
        }

        override fun onRepostClick(feedViewData: FeedViewData) {
            feedActionsViewModel.repost(feedViewData)
        }

        override fun onReplyClick(feedViewData: FeedViewData) {
            findNavController().navigate(NavGraphDirections.actionGlobalNewPostFragment(feedViewData.status))
        }

        override fun onFavoriteClick(feedViewData: FeedViewData) {
            feedActionsViewModel.favorite(feedViewData)
        }

        override fun onAvatarClick(feedViewData: FeedViewData) {
            findNavController().navigate(NavGraphDirections.actionGlobalProfileFragment(feedViewData.status.account.id))
        }

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
            findNavController().navigate(NavGraphDirections.actionGlobalProfileFragment(id))
        }

        override fun onUrlClick(url: String) {
            if (activity != null) {
                OpenLinkHelper.openLink(url, activity!!)
            }
        }

        override fun onClick(param: FeedViewData) {
        }

        override fun onLongClick() {
        }
    }
}
