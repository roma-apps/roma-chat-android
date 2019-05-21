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

package tech.bigfig.romachat.view.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.*

interface BackFragment {
    fun onBackPressed(): Boolean
}

fun fireOnBackPressedEvent(activity: FragmentActivity): Boolean {
    val fragmentList = getAllActivityFragments(activity)
    return fireOnBackPressedEvent(fragmentList)
}

private fun getAllActivityFragments(activity: FragmentActivity): List<Fragment> {
    val fragmentList = activity.supportFragmentManager.fragments

    if (fragmentList.isNotEmpty()) {
        val result = ArrayList<Fragment>(fragmentList.size)

        fragmentList.reversed().forEach { fragment ->
            if (fragment != null) {
                result.add(fragment)

                val nestedFragmentList = fragment.childFragmentManager.fragments
                if (nestedFragmentList.isNotEmpty()) {
                    result.addAll(nestedFragmentList)
                }
            }
        }

        return result
    } else {
        return ArrayList(0)
    }
}

private fun fireOnBackPressedEvent(fragmentList: List<*>): Boolean {

    // find all fragments with back support
    val backFragmentList = ArrayList<BackFragment>(fragmentList.size)
    for (fragment in fragmentList) {
        if (fragment is BackFragment) {
            backFragmentList.add(fragment)
        }
    }

    // send them onBackPressed event
    var handled = false
    for (fragment in backFragmentList) {
        if (fragment is Fragment) {
            if ((fragment as Fragment).userVisibleHint) {
                handled = fragment.onBackPressed()
            }
        }

        if (handled) {
            break
        }
    }

    return handled
}