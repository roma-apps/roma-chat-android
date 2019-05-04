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

package tech.bigfig.romachat.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tech.bigfig.romachat.data.db.dao.AccountDao
import tech.bigfig.romachat.data.db.dao.ChatAccountDao
import tech.bigfig.romachat.data.db.dao.MessageDao
import tech.bigfig.romachat.data.db.entity.AccountEntity
import tech.bigfig.romachat.data.db.entity.ChatAccountEntity
import tech.bigfig.romachat.data.db.entity.MessageEntity

/**
 * DB version & declare DAO
 */

@Database(
    entities = [AccountEntity::class, MessageEntity::class, ChatAccountEntity::class], version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun messageDao(): MessageDao
    abstract fun chatAccountDao(): ChatAccountDao

    companion object {
        val MIGRATION_1_2: Migration  = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ChatAccountEntity ADD COLUMN localUsername TEXT NOT NULL default ''");
            }
        }
    }
}