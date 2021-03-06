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

package tech.bigfig.romachat.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import tech.bigfig.romachat.data.db.entity.MessageEntity
import tech.bigfig.romachat.data.entity.ChatInfo

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: MessageEntity)

    @Query("SELECT * FROM MessageEntity WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun loadAll(accountId:String): LiveData<List<MessageEntity>>

    @Query("SELECT COUNT(m.id) as messageCount, * FROM MessageEntity m LEFT JOIN ChatAccountEntity a ON m.accountId = a.id GROUP BY accountId ORDER BY MAX(createdAt) DESC")
    fun loadAllChats(): LiveData<List<ChatInfo>>

    @Query("DELETE FROM MessageEntity WHERE id = :messageId")
    fun delete(messageId: String)

}