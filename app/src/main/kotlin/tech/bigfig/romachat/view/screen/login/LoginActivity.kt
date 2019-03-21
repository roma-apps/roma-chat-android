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

package tech.bigfig.romachat.view.screen.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import tech.bigfig.romachat.R
import tech.bigfig.romachat.app.App
import javax.inject.Inject


class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_fragment)

        App.getApplication(this).appComponent.inject(this)

        viewModel = obtainViewModel()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, LoginFragment.newInstance()).commit()
        }
    }

    /* Called when OAuth browser login finished */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null && intent.data != null) {
            /* Check if we are resuming during authorization by seeing if the intent contains the
            * redirect that was given to the server. If so, its response is here! */
            val uri = intent.data!!

            Log.d(LOG_TAG, "onNewIntent uri = $uri")

            viewModel.onOauthRedirect(uri)
        }
    }

    /* Share the same viewmodel between activity and fragment (instead of getting viewmodel directly in fragment)
     * to easily pass oauth results to viewmodel*/
    fun obtainViewModel(): LoginViewModel {

        return ViewModelProviders.of(this, viewModelFactory)
            .get(LoginViewModel::class.java)
    }

    companion object {
        private const val LOG_TAG = "LoginActivity"
    }
}
