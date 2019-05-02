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

package tech.bigfig.romachat.app.di

import android.content.Context
import android.text.Spanned
import androidx.room.Room
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.bigfig.romachat.BuildConfig
import tech.bigfig.romachat.data.api.InstanceSwitchAuthInterceptor
import tech.bigfig.romachat.data.api.PLACEHOLDER_DOMAIN
import tech.bigfig.romachat.data.api.RestApi
import tech.bigfig.romachat.data.db.AccountManager
import tech.bigfig.romachat.data.db.AppDatabase
import tech.bigfig.romachat.utils.SpannedTypeAdapter
import java.util.concurrent.TimeUnit

import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    @Singleton
    fun provideDb(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "romachatDB")
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    @Singleton
    fun provideAccountManager(appDatabase: AppDatabase): AccountManager {
        return AccountManager(appDatabase)
    }

    @Provides
    @Singleton
    fun provideRestApi(accountManager: AccountManager): RestApi {
        val builder = Retrofit.Builder()
            .baseUrl("https://$PLACEHOLDER_DOMAIN")
            .addConverterFactory(buildGsonConverterFactory())
            .client(buildOkHttpClient(accountManager))
            .build()

        return builder.create(RestApi::class.java)
    }

    private fun buildGsonConverterFactory(): GsonConverterFactory {
        val gson = GsonBuilder()
            .registerTypeAdapter(Spanned::class.java, SpannedTypeAdapter())
            .create()
        return GsonConverterFactory.create(gson)
    }

    private fun buildOkHttpClient(accountManager: AccountManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(InstanceSwitchAuthInterceptor(accountManager))
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
