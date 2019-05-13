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

package tech.bigfig.romachat.view.utils

import android.content.Context
import tech.bigfig.romachat.R


object DateUtils {
    /**
     * This is a rough duplicate of [android.text.format.DateUtils.getRelativeTimeSpanString],
     * but even with the FORMAT_ABBREV_RELATIVE flag it wasn't abbreviating enough.
     */
    @Suppress("LocalVariableName")
    fun getRelativeTimeSpanString(context: Context, then: Long, now: Long): String {
        val MINUTE: Long = 60
        val HOUR = 60 * MINUTE
        val DAY = 24 * HOUR
        val YEAR = 365 * DAY
        var span = (now - then) / 1000
        var future = false
        if (span < 0) {
            future = true
            span = -span
        }
        val format: String
        if (span < MINUTE) {
            if (future) {
                format = context.getString(R.string.abbreviated_in_seconds)
            } else {
                format = context.getString(R.string.abbreviated_seconds_ago)
            }
        } else if (span < HOUR) {
            span /= MINUTE
            if (future) {
                format = context.getString(R.string.abbreviated_in_minutes)
            } else {
                format = context.getString(R.string.abbreviated_minutes_ago)
            }
        } else if (span < DAY) {
            span /= HOUR
            if (future) {
                format = context.getString(R.string.abbreviated_in_hours)
            } else {
                format = context.getString(R.string.abbreviated_hours_ago)
            }
        } else if (span < YEAR) {
            span /= DAY
            if (future) {
                format = context.getString(R.string.abbreviated_in_days)
            } else {
                format = context.getString(R.string.abbreviated_days_ago)
            }
        } else {
            span /= YEAR
            if (future) {
                format = context.getString(R.string.abbreviated_in_years)
            } else {
                format = context.getString(R.string.abbreviated_years_ago)
            }
        }
        return String.format(format, span)
    }
}
