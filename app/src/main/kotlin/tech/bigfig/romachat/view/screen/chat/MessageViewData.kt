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

package tech.bigfig.romachat.view.screen.chat

import android.os.Parcelable
import android.text.Spanned
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.WriteWith
import tech.bigfig.romachat.data.entity.Attachment
import tech.bigfig.romachat.data.entity.Emoji
import tech.bigfig.romachat.data.entity.Status
import tech.bigfig.romachat.utils.SpannedNullParceler

@Parcelize
data class MessageViewData(
    val id: String,

    var showDate: Boolean,
    val date: String?,

    var showAccount: Boolean,
    val account: String?,
    val fromMe: Boolean,

    val isMedia: Boolean,

    val content: @WriteWith<SpannedNullParceler>() Spanned?,
    val mentions: Array<Status.Mention>?,
    val emojis: List<Emoji>?,

    val attachment: Attachment?
) : Parcelable