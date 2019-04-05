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
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_view_media.*
import tech.bigfig.romachat.R
import tech.bigfig.romachat.data.entity.Media
import java.util.*

class ViewMediaActivity : AppCompatActivity(), ViewImageFragment.PhotoActionsListener {
    companion object {
        private const val EXTRA_MEDIA = "EXTRA_MEDIA"

        @JvmStatic
        fun newIntent(context: Context?, media: Media): Intent {
            val intent = Intent(context, ViewMediaActivity::class.java)
            intent.putExtra(EXTRA_MEDIA, media)
            return intent
        }
    }

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

        val media = intent.getParcelableExtra<Media>(EXTRA_MEDIA)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, ViewMediaFragment.newInstance(media, true)).commit()
        }

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.title = ""
        }
        toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }

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

        toolbar.animate().alpha(alpha)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    toolbar.visibility = visibility
                    animation.removeListener(this)
                }
            })
            .start()
    }
}
