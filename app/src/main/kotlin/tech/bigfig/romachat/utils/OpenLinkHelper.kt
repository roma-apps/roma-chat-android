/* Copyright 2017 Andrew Dawson
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
 * see <http://www.gnu.org/licenses>. */

package tech.bigfig.romachat.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import tech.bigfig.romachat.R
import timber.log.Timber

object OpenLinkHelper {

    /**
     * Opens a link, depending on the settings, either in the browser or in a custom tab
     *
     * @param url     a string containing the url to open
     * @param context context
     */
    fun openLink(url: String, context: Context) {
        val uri = Uri.parse(url).normalizeScheme()

        val useCustomTabs = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("customTabs", false)
        if (useCustomTabs) {
            openLinkInCustomTab(uri, context)
        } else {
            openLinkInBrowser(uri, context)
        }
    }

    /**
     * Opens a link in the browser via Intent.ACTION_VIEW
     *
     * @param uri     the uri to open
     * @param context context
     */
    fun openLinkInBrowser(uri: Uri, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.w("Actvity was not found for intent, $intent")
        }

    }

    /**
     * tries to open a link in a custom tab
     * falls back to browser if not possible
     *
     * @param uri     the uri to open
     * @param context context
     */
    fun openLinkInCustomTab(uri: Uri, context: Context) {
        val toolbarColor = ContextCompat.getColor(context, R.color.custom_tab_toolbar)

        val customTabsIntent = CustomTabsIntent.Builder()
            .setToolbarColor(toolbarColor)
            .build()
        try {
            val packageName = CustomTabsHelper.getPackageNameToUse(context)

            //If we cant find a package name, it means theres no browser that supports
            //Chrome Custom Tabs installed. So, we fallback to the webview
            if (packageName == null) {
                openLinkInBrowser(uri, context)
            } else {
                customTabsIntent.intent.setPackage(packageName)
                customTabsIntent.launchUrl(context, uri)
            }
        } catch (e: ActivityNotFoundException) {
            Timber.w("Activity was not found for intent, $customTabsIntent")
        }

    }
}
