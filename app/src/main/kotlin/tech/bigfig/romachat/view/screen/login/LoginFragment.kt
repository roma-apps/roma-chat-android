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


import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import tech.bigfig.romachat.R
import tech.bigfig.romachat.databinding.FragmentLoginBinding
import tech.bigfig.romachat.view.utils.LinkHelper


class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        viewModel = (activity as LoginActivity).obtainViewModel()

        viewModel.login.observe(this, Observer { uri ->
            Log.d(LOG_TAG, "login result uri = ${uri.toString()}")
            if (uri != null) {
                openOauthUrl(uri)
            }
        })

        viewModel.fetchOAuthToken.observe(this, Observer { token ->
            Log.d(LOG_TAG, "oauth token = $token")
            if (token != null) {
                //TODO redirect to main screen
            }
        })

        binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = activity

        binding.loginInstance.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.onSubmitClick(binding.loginInstance.text.toString())
            }
            false
        }

        return binding.root
    }

    private fun openOauthUrl(uri: Uri) {
        try {
            LinkHelper.openLinkInCustomTab(uri, activity!!)
        } catch (e: ActivityNotFoundException) {
            viewModel.showError(getString(R.string.error_no_web_browser_found))
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = LoginFragment()

        private const val LOG_TAG = "LoginFragment"
    }
}
