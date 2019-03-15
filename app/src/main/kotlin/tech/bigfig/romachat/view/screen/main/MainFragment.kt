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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import tech.bigfig.romachat.databinding.FragmentMainBinding
import tech.bigfig.romachat.view.screen.camera.CameraFragment
import tech.bigfig.romachat.view.screen.chatlist.ChatListFragment


class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        binding = FragmentMainBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = this

        binding.mainPager.adapter = MainFragmentPagerAdapter(childFragmentManager)
        binding.mainPager.currentItem = 1

        return binding.root
    }


    class MainFragmentPagerAdapter(fragmentManager: FragmentManager) :
        FragmentPagerAdapter(fragmentManager) {

        override fun getCount(): Int {
            return 2
        }

        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> return ChatListFragment.newInstance()
                else -> return CameraFragment.newInstance()
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = MainFragment()
    }
}
