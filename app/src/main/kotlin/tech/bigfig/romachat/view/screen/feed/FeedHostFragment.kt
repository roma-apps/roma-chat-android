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


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import tech.bigfig.romachat.R
import tech.bigfig.romachat.databinding.FragmentFeedHostBinding


class FeedHostFragment : Fragment() {

    private lateinit var binding: FragmentFeedHostBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = FragmentFeedHostBinding.inflate(layoutInflater, container, false)

        binding.tabLayout.setupWithViewPager(binding.pager)
        binding.pager.offscreenPageLimit = 2
        binding.pager.adapter =
            FeedHostFragmentPagerAdapter(arrayOf(Tab.HOME, Tab.ALL, Tab.ME), childFragmentManager, context)

        return binding.root
    }

    companion object {

        @JvmStatic
        fun newInstance() = FeedHostFragment()
    }

    class FeedHostFragmentPagerAdapter(private val tabs: Array<Tab>, fragmentManager: FragmentManager, val context: Context?) :
        FragmentPagerAdapter(fragmentManager) {

        override fun getCount(): Int {
            return tabs.size
        }

        override fun getItem(position: Int): Fragment {
            return FeedFragment.newInstance(tabs[position].feedType)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return context?.getString(tabs[position].titleResourceId)
        }
    }

    enum class Tab(val feedType: FeedType, val titleResourceId: Int) {
        HOME(FeedType.HOME, R.string.feed_tab_home),
        ALL(FeedType.ALL, R.string.feed_tab_all),
        ME(FeedType.ME, R.string.feed_tab_me)
    }
}
