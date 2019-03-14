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

import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    @Singleton
    fun provideRestApi(): RestApi {
        val builder = Retrofit.Builder()
            .baseUrl("https://$PLACEHOLDER_DOMAIN")
            .addConverterFactory(buildGsonConverterFactory())
            .client(buildOkHttpClient())
            .build()

        return builder.create(RestApi::class.java)
    }

    private fun buildGsonConverterFactory(): GsonConverterFactory {
        val gsonBuilder = GsonBuilder()

        val gson = gsonBuilder.create()

        return GsonConverterFactory.create(gson)
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(InstanceSwitchAuthInterceptor())//TODO pass accountManager
            .build()
    }

}
