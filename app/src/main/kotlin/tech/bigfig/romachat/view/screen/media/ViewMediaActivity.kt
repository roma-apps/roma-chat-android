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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.navArgs
import androidx.viewpager.widget.ViewPager
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.entity.Media
import tech.bigfig.romachat.databinding.ActivityViewMediaBinding
import java.util.*

class ViewMediaActivity : AppCompatActivity(), ViewImageFragment.PhotoActionsListener {

    private val navArgs: ViewMediaActivityArgs by navArgs()

    private lateinit var binding: ActivityViewMediaBinding

    private var toolbarVisible = true
    private val toolbarVisibilityListeners = ArrayList<ToolbarVisibilityListener>()

    interface ToolbarVisibilityListener {
        fun onToolbarVisiblityChanged(isVisible: Boolean)
    }

    fun addToolbarVisibilityListener(listener: ToolbarVisibilityListener): Function0<Boolean> {
        this.toolbarVisibilityListeners.add(listener)
        listener.onToolbarVisiblityChanged(toolbarVisible)
        return { toolbarVisibilityListeners.remove(listener) }
    }

    fun isToolbarVisible(): Boolean {
        return toolbarVisible
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_media)

        supportPostponeEnterTransition()

        val media = navArgs.mediaList
        val initialPosition = navArgs.currentMediaIndex

        binding = DataBindingUtil.setContentView(this, R.layout.activity_view_media)

        val adapter = ImagePagerAdapter(supportFragmentManager, media, initialPosition)
        binding.viewPager.adapter = adapter
        binding.viewPager.currentItem = initialPosition
        binding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                binding.toolbar.title = adapter.getPageTitle(position)
            }
        })

        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.title = adapter.getPageTitle(initialPosition)
        }
        binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
        window.statusBarColor = Color.BLACK
    }

    override fun onBringUp() {
        supportStartPostponedEnterTransition()
    }

    override fun onDismiss() {
        supportFinishAfterTransition()
    }

    override fun onPhotoTap() {
        toolbarVisible = !toolbarVisible
        for (listener in toolbarVisibilityListeners) {
            listener.onToolbarVisiblityChanged(toolbarVisible)
        }
        val visibility = if (toolbarVisible) View.VISIBLE else View.INVISIBLE

        val alpha = if (toolbarVisible) 1.0f else 0.0f

        binding.toolbar.animate().alpha(alpha)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.toolbar.visibility = visibility
                    animation.removeListener(this)
                }
            })
            .start()
    }

    class ImagePagerAdapter(
        fragmentManager: FragmentManager,
        private val attachments: Array<Media>,
        private val initialPosition: Int
    ) : FragmentStatePagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return if (position >= 0 && position < attachments.size) {
                ViewMediaFragment.newInstance(attachments[position], position == initialPosition)
            } else {
                throw IllegalStateException()
            }
        }

        override fun getCount(): Int {
            return attachments.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return if (attachments.size <= 1) "" else "${position + 1}/${attachments.size}"
        }
    }

}
