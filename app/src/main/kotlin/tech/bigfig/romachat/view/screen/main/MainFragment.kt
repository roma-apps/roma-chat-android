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

package tech.bigfig.romachat.view.screen.main


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.navigation.fragment.findNavController
import tech.bigfig.romachat.R
import tech.bigfig.romachat.databinding.FragmentMainBinding
import tech.bigfig.romachat.view.screen.camera.CameraFragment
import tech.bigfig.romachat.view.screen.chatlist.ChatListFragment
import tech.bigfig.romachat.view.screen.feed.FeedHostFragment


class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        setHasOptionsMenu(true)

        binding = FragmentMainBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.mainPager.adapter = MainFragmentPagerAdapter(childFragmentManager)
        binding.mainPager.currentItem = 1

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.chat_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_search_user -> {
                findNavController().navigate(MainFragmentDirections.actionToUserSearchFragment())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class MainFragmentPagerAdapter(fragmentManager: FragmentManager) :
        FragmentPagerAdapter(fragmentManager) {

        override fun getCount(): Int {
            return 3
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> ChatListFragment.newInstance()
                2 -> FeedHostFragment.newInstance()
                else -> CameraFragment.newInstance()
            }
        }
    }
}
