/* Copyright 2017 Andrew Dawson
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
 * see <http://www.gnu.org/licenses>. */

package tech.bigfig.romachat.view.screen.media

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import tech.bigfig.romachat.databinding.FragmentMediaViewBinding

class ViewImageFragment : ViewMediaFragment() {
    interface PhotoActionsListener {
        fun onBringUp()
        fun onDismiss()
        fun onPhotoTap()
    }

    private lateinit var attacher: PhotoViewAttacher
    private lateinit var photoActionsListener: PhotoActionsListener

    private lateinit var binding: FragmentMediaViewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        binding = FragmentMediaViewBinding.inflate(layoutInflater, container, false)

        binding.videoPlayer.visibility = View.GONE
        binding.photoView.visibility = View.VISIBLE

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        photoActionsListener = context as PhotoActionsListener
    }

    override fun setupMediaView(url: String) {
        binding.photoView.transitionName = url
        attacher = PhotoViewAttacher(binding.photoView)

        // Clicking outside the photo closes the viewer.
        attacher.setOnOutsidePhotoTapListener { photoActionsListener.onDismiss() }

        attacher.setOnClickListener { onMediaTap() }

        /* A vertical swipe motion also closes the viewer. This is especially useful when the photo
         * mostly fills the screen so clicking outside is difficult. */
        attacher.setOnSingleFlingListener { _, _, velocityX, velocityY ->
            var result = false
            if (Math.abs(velocityY) > Math.abs(velocityX)) {
                photoActionsListener.onDismiss()
                result = true
            }
            result
        }

        // If we are the view to be shown initially...
        if (arguments!!.getBoolean(ViewMediaFragment.ARG_START_POSTPONED_TRANSITION)) {
            // Try to load image from disk.
            Picasso.get()
                .load(url)
                .noFade()
                .networkPolicy(NetworkPolicy.OFFLINE)
                .resize(MAX_WIDTH, MAX_HEIGHT)
                .onlyScaleDown()
                .centerInside()
                .into(binding.photoView, object : Callback {
                    override fun onSuccess() {
                        // if we loaded image from disk, we should check that view is attached.
                        if (binding.photoView.isAttachedToWindow) {
                            finishLoadingSuccessfully()
                        } else {
                            // if view is not attached yet, wait for an attachment and
                            // start transition when it's finally ready.
                            binding.photoView.addOnAttachStateChangeListener(
                                object : View.OnAttachStateChangeListener {
                                    override fun onViewAttachedToWindow(v: View?) {
                                        finishLoadingSuccessfully()
                                        binding.photoView.removeOnAttachStateChangeListener(this)
                                    }

                                    override fun onViewDetachedFromWindow(v: View?) {}
                                })
                        }
                    }

                    override fun onError(e: Exception) {
                        // if there's no image in cache, load from network and start transition
                        // immediately.
                        if (isAdded) {
                            photoActionsListener.onBringUp()
                            loadImageFromNetwork(url, binding.photoView)
                        }
                    }
                })
        } else {
            // if we're not initial page, don't bother.
            loadImageFromNetwork(url, binding.photoView)
        }

    }

    private fun onMediaTap() {
        photoActionsListener.onPhotoTap()
    }

    override fun onToolbarVisibilityChange(visible: Boolean) {
        if (!userVisibleHint) {
            return
        }
    }

    override fun onDetach() {
        super.onDetach()
        Picasso.get().cancelRequest(binding.photoView)
    }

    private fun loadImageFromNetwork(url: String, photoView: ImageView) {
        Picasso.get()
            .load(url)
            .noPlaceholder()
            .networkPolicy(NetworkPolicy.NO_STORE)
            .resize(MAX_WIDTH, MAX_HEIGHT)
            .onlyScaleDown()
            .centerInside()
            .into(photoView, object : Callback {
                override fun onSuccess() {
                    finishLoadingSuccessfully()
                }

                override fun onError(e: Exception) {
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    private fun finishLoadingSuccessfully() {
        binding.progressBar.visibility = View.GONE
        attacher.update()
        photoActionsListener.onBringUp()
    }

    companion object {
        private const val MAX_WIDTH = 1920
        private const val MAX_HEIGHT = 1920
    }
}
