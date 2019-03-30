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

package tech.bigfig.romachat.data.db.entity

import android.text.Spanned
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import tech.bigfig.romachat.data.db.Converters
import tech.bigfig.romachat.data.entity.Attachment
import tech.bigfig.romachat.data.entity.Status
import java.util.*

@Entity(
    foreignKeys = [ForeignKey(
        entity = ChatAccountEntity::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = CASCADE
    )]
)
@TypeConverters(Converters::class)
data class MessageEntity(

    @PrimaryKey
    val id: String,
    var content: Spanned,

    var accountId: String,
    var accountName: String,

    var fromMe: Boolean,

    var createdAt: Date,

    var mentions: Array<Status.Mention>,
    var attachments: List<Attachment>
)