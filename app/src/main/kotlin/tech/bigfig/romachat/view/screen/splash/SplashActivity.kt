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

package tech.bigfig.romachat.view.screen.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.view.screen.login.LoginActivity
import tech.bigfig.romachat.view.screen.main.MainActivity
import timber.log.Timber
import javax.inject.Inject


class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: SplashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        App.getApplication(this).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SplashViewModel::class.java)

        viewModel.isUserLoggedIn.observe(this, Observer { isLoggedIn ->
            Timber.d("user logged in = $isLoggedIn")

            if (isLoggedIn) {
                // redirect to main
                MainActivity.start(this)
            } else {
                // redirect to login
                LoginActivity.start(this)
            }
            finish()
        })
    }
}
