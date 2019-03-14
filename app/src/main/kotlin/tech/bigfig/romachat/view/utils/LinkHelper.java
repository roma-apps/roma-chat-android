/* Copyright 2017 Andrew Dawson
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

package tech.bigfig.romachat.view.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import tech.bigfig.romachat.R;

public class LinkHelper {

    /**
     * Opens a link, depending on the settings, either in the browser or in a custom tab
     *
     * @param url     a string containing the url to open
     * @param context context
     */
    public static void openLink(String url, Context context) {
        Uri uri = Uri.parse(url).normalizeScheme();

        boolean useCustomTabs = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("customTabs", false);
        if (useCustomTabs) {
            openLinkInCustomTab(uri, context);
        } else {
            openLinkInBrowser(uri, context);
        }
    }

    /**
     * Opens a link in the browser via Intent.ACTION_VIEW
     *
     * @param uri     the uri to open
     * @param context context
     */
    public static void openLinkInBrowser(Uri uri, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w("URLSpan", "Actvity was not found for intent, " + intent.toString());
        }
    }

    /**
     * tries to open a link in a custom tab
     * falls back to browser if not possible
     *
     * @param uri     the uri to open
     * @param context context
     */
    public static void openLinkInCustomTab(Uri uri, Context context) {
        int toolbarColor = ContextCompat.getColor(context, R.color.custom_tab_toolbar);

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(toolbarColor);
        CustomTabsIntent customTabsIntent = builder.build();
        try {
            String packageName = CustomTabsHelper.getPackageNameToUse(context);

            //If we cant find a package name, it means theres no browser that supports
            //Chrome Custom Tabs installed. So, we fallback to the webview
            if (packageName == null) {
                openLinkInBrowser(uri, context);
            } else {
                customTabsIntent.intent.setPackage(packageName);
                customTabsIntent.launchUrl(context, uri);
            }
        } catch (ActivityNotFoundException e) {
            Log.w("URLSpan", "Activity was not found for intent, " + customTabsIntent.toString());
        }

    }


}
