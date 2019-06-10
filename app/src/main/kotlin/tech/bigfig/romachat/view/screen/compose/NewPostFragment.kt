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

package tech.bigfig.romachat.view.screen.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import tech.bigfig.romachat.R
import tech.bigfig.romachat.app.App
import tech.bigfig.romachat.databinding.FragmentNewPostBinding
import timber.log.Timber
import javax.inject.Inject

class NewPostFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: NewPostViewModel
    private lateinit var binding: FragmentNewPostBinding

    private val navArgs: NewPostFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        App.getApplication(activity!!).appComponent.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(NewPostViewModel::class.java)

        viewModel.postMessage.observe(this, Observer { result ->
            Timber.d("result $result")
            if (result != null) {
                activity?.onBackPressed()
            }
        })

        viewModel.initialText.observe(this, Observer { initialText ->
            binding.text.setText(initialText)
            binding.text.setSelection(binding.text.length())
        })

        viewModel.initData(navArgs.statusToReply)

        binding = FragmentNewPostBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        (activity as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            if (navArgs.statusToReply != null) setTitle(R.string.new_post_title_reply)
        }
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        binding.text.requestFocus()

        return binding.root
    }
}
