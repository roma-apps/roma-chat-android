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

package tech.bigfig.romachat.view.screen.media

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import tech.bigfig.romachat.databinding.FragmentMediaViewBinding

class ViewVideoFragment : ViewMediaFragment() {
    private val handler = Handler(Looper.getMainLooper())
    private val hideToolbar = Runnable {
        // Hoist toolbar hiding to activity so it can track state across different fragments
        // This is explicitly stored as runnable so that we pass it to the handler later for cancellation
        mediaActivity.onPhotoTap()
    }
    private lateinit var mediaActivity: ViewMediaActivity
    private lateinit var mediaController: MediaController

    private lateinit var binding: FragmentMediaViewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mediaActivity = activity as ViewMediaActivity

        binding = FragmentMediaViewBinding.inflate(layoutInflater, container, false)

        binding.videoPlayer.visibility = View.VISIBLE
        binding.photoView.visibility = View.GONE

        return binding.root
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        // Start/pause/resume video playback as fragment is shown/hidden
        super.setUserVisibleHint(isVisibleToUser)
        try {
            if (isVisibleToUser) {
                if (mediaActivity.isToolbarVisible()) {
                    handler.postDelayed(hideToolbar, TOOLBAR_HIDE_DELAY_MS)
                }
                binding.videoPlayer.start()
            } else {

                handler.removeCallbacks(hideToolbar)
                binding.videoPlayer.pause()
                mediaController.hide()

            }
        } catch (e: Exception) {
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setupMediaView(url: String) {
        val videoView = binding.videoPlayer
        videoView.transitionName = url
        videoView.setVideoPath(url)
        mediaController = MediaController(mediaActivity)
        mediaController.setMediaPlayer(binding.videoPlayer)
        binding.videoPlayer.setMediaController(mediaController)
        videoView.requestFocus()
        videoView.setOnTouchListener { _, _ ->
            mediaActivity.onPhotoTap()
            false
        }
        videoView.setOnPreparedListener { mp ->
            binding.progressBar.visibility = View.GONE
            mp.isLooping = true
            if (arguments!!.getBoolean(ARG_START_POSTPONED_TRANSITION)) {
                hideToolbarAfterDelay(TOOLBAR_HIDE_DELAY_MS)
                videoView.start()
            }
        }

        if (arguments!!.getBoolean(ARG_START_POSTPONED_TRANSITION)) {
            mediaActivity.onBringUp()
        }
    }

    private fun hideToolbarAfterDelay(delayMilliseconds: Long) {
        handler.postDelayed(hideToolbar, delayMilliseconds)
    }

    override fun onToolbarVisibilityChange(visible: Boolean) {
        if (!userVisibleHint) {
            return
        }

        if (visible) {
            hideToolbarAfterDelay(TOOLBAR_HIDE_DELAY_MS)
        } else {
            handler.removeCallbacks(hideToolbar)
        }
    }

    companion object {
        private const val TOOLBAR_HIDE_DELAY_MS = 3000L
    }
}
