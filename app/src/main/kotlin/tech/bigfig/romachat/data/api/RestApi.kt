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

package tech.bigfig.romachat.data.api

import retrofit2.Call
import retrofit2.http.*
import tech.bigfig.romachat.data.entity.AccessToken
import tech.bigfig.romachat.data.entity.Account
import tech.bigfig.romachat.data.entity.AppCredentials
import tech.bigfig.romachat.data.entity.Status

const val PLACEHOLDER_DOMAIN = "dummy.placeholder"
const val DOMAIN_HEADER = "domain"

interface RestApi {

    //------
    // LOGIN

    @FormUrlEncoded
    @POST("api/v1/apps")
    fun authenticateApp(
        @Header(DOMAIN_HEADER) domain: String,
        @Field("client_name") clientName: String,
        @Field("redirect_uris") redirectUris: String,
        @Field("scopes") scopes: String,
        @Field("website") website: String
    ): Call<AppCredentials>

    @FormUrlEncoded
    @POST("oauth/token")
    fun fetchOAuthToken(
        @Header(DOMAIN_HEADER) domain: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String
    ): Call<AccessToken>

    @GET("api/v1/accounts/verify_credentials")
    fun accountVerifyCredentials(): Call<Account>

    //-----
    // CHAT

    @GET("api/v1/accounts/{id}/following")
    fun accountFollowing(
        @Path("id") accountId: String,
        @Query("max_id") maxId: String
    ): Call<List<Account>>

    @GET("api/v1/timelines/direct")
    fun directTimeline(
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>
}