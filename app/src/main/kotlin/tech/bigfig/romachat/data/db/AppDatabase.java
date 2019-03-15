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

package tech.bigfig.romachat.data.db;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import tech.bigfig.romachat.data.db.entity.AccountEntity;

/**
 * DB version & declare DAO
 */

@Database(entities = {/*TootEntity.class, */
        AccountEntity.class, /*InstanceEntity.class, TimelineStatusEntity.class,
                TimelineAccountEntity.class,  ConversationEntity.class*/
        }, version = 1)
public abstract class AppDatabase extends RoomDatabase {

//    public abstract TootDao tootDao();
    public abstract AccountDao accountDao();
//    public abstract InstanceDao instanceDao();
//    public abstract ConversationsDao conversationDao();
//    public abstract TimelineDao timelineDao();


}