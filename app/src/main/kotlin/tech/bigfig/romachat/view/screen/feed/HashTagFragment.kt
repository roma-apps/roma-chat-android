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

package tech.bigfig.romachat.view.screen.feed


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import tech.bigfig.romachat.R
import tech.bigfig.romachat.databinding.FragmentHashtagBinding


class HashTagFragment : Fragment() {

    private lateinit var binding: FragmentHashtagBinding

    private val navArgs: HashTagFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = FragmentHashtagBinding.inflate(layoutInflater, container, false)

        (activity as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            title = "#${navArgs.hashTag}"
        }
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        if (savedInstanceState == null && childFragmentManager.findFragmentByTag("TAG") == null) {
            childFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                FeedFragment.newInstanceHashTag(navArgs.hashTag),
                "TAG"
            ).commit()
        }

        return binding.root
    }
}
