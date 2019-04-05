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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import tech.bigfig.romachat.data.entity.Media
import tech.bigfig.romachat.data.entity.MediaType
import java.lang.IllegalArgumentException

abstract class ViewMediaFragment : Fragment() {
    private var toolbarVisibilityDisposable: Function0<Boolean>? = null

    abstract fun setupMediaView(url: String)
    abstract fun onToolbarVisibilityChange(visible: Boolean)

    lateinit var media: Media

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arguments = this.arguments!!
        media = arguments.getParcelable(ARG_MEDIA) ?: throw IllegalArgumentException("No media attached")

        finalizeViewSetup(media.uri)
    }

    private fun finalizeViewSetup(url: String) {
        setupMediaView(url)

        toolbarVisibilityDisposable = (activity as ViewMediaActivity).addToolbarVisibilityListener(object :
            ViewMediaActivity.ToolbarVisibilityListener {
            override fun onToolbarVisiblityChanged(isVisible: Boolean) {
                onToolbarVisibilityChange(isVisible)
            }
        })
    }

    override fun onDestroyView() {
        toolbarVisibilityDisposable?.invoke()
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        protected val ARG_START_POSTPONED_TRANSITION = "startPostponedTransition"
        @JvmStatic
        protected val ARG_MEDIA = "ARG_MEDIA"

        @JvmStatic
        fun newInstance(media: Media, shouldStartPostponedTransition: Boolean): ViewMediaFragment {
            val arguments = Bundle(2)
            arguments.putParcelable(ARG_MEDIA, media)
            arguments.putBoolean(ARG_START_POSTPONED_TRANSITION, shouldStartPostponedTransition)

            val fragment = when (media.type) {
                MediaType.IMAGE -> ViewImageFragment()
                MediaType.VIDEO -> ViewVideoFragment()
            }
            fragment.arguments = arguments
            return fragment
        }
    }
}
