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

package tech.bigfig.romachat.utils

import android.net.Uri
import android.util.Patterns

/** Make sure the user-entered text is just a fully-qualified domain name.  */
fun canonicalizeDomain(domain: String): String {
    // Strip any schemes out.
    var s = domain.replaceFirst("http://", "").replaceFirst("https://", "")

    // If a username was included (e.g. username@example.com), just take what's after the '@'.
    val at = s.lastIndexOf('@')
    if (at != -1) {
        s = s.substring(at + 1)
    }
    return s.trim { it <= ' ' }
}

fun validateDomain(domain: String) : Boolean {
   return Patterns.WEB_URL.matcher(domain).matches()
}

/**
 * Chain together the key-value pairs into a query string, for either appending to a URL or
 * as the content of an HTTP request.
 */
fun buildQueryString(parameters: Map<String, String>): String {
    val s = StringBuilder()
    var between = ""
    for ((key, value) in parameters) {
        s.append(between)
        s.append(Uri.encode(key))
        s.append("=")
        s.append(Uri.encode(value))
        between = "&"
    }
    return s.toString()
}