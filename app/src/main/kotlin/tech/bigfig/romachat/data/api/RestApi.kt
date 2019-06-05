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

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.Field
import tech.bigfig.romachat.data.entity.*

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

    //--------
    // ACCOUNT

    @GET("api/v1/accounts/{id}/following")
    fun accountFollowing(
        @Path("id") accountId: String,
        @Query("max_id") maxId: String?
    ): Call<List<Account>>

    @FormUrlEncoded
    @POST("api/v1/accounts/{id}/follow")
    fun followAccount(@Path("id") accountId: String, @Field("reblogs") showReblogs: Boolean): Call<Relationship>

    @POST("api/v1/accounts/{id}/unfollow")
    fun unfollowAccount(@Path("id") accountId: String): Call<Relationship>

    @GET("api/v1/accounts/{id}")
    fun account(@Path("id") accountId: String): Call<Account>

    @GET("api/v1/accounts/relationships")
    fun relationships(@Query("id[]") accountIds: List<String>): Call<List<Relationship>>

    //-----
    // CHAT

    @GET("api/v1/timelines/direct")
    fun directTimeline(
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @FormUrlEncoded
    @POST("api/v1/statuses")
    fun createStatus(
        @Header("Authorization") auth: String,
        @Header(DOMAIN_HEADER) domain: String,
        @Field("status") text: String?,
        @Field("in_reply_to_id") inReplyToId: String?,
        @Field("spoiler_text") warningText: String?,
        @Field("visibility") visibility: String,
        @Field("sensitive") sensitive: Boolean?,
        @Field("media_ids[]") mediaIds: List<String>?,
        @Header("Idempotency-Key") idempotencyKey: String
    ): Call<Status>

    @Multipart
    @POST("api/v1/media")
    fun uploadMedia(@Part file: MultipartBody.Part): Call<Attachment>

    @DELETE("api/v1/statuses/{id}")
    fun deleteStatus(@Path("id") statusId: String): Call<ResponseBody>

    //-------
    // SEARCH

    @GET("api/v2/search")
    fun search(@Query("q") query: String, @Query("resolve") resolve: Boolean?): Call<SearchResults>

    //-----
    // FEED

    @GET("api/v1/timelines/home")
    fun homeTimeline(
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @GET("api/v1/timelines/public")
    fun publicTimeline(
        @Query("local") local: Boolean?,
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @GET("api/v1/accounts/{id}/statuses")
    fun accountStatuses(
        @Path("id") accountId: String,
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?,
        @Query("exclude_replies") excludeReplies: Boolean?,
        @Query("only_media") onlyMedia: Boolean?,
        @Query("pinned") pinned: Boolean?
    ): Call<List<Status>>

    @GET("api/v1/timelines/tag/{hashtag}")
    fun hashtagTimeline(
        @Path("hashtag") hashtag: String,
        @Query("local") local: Boolean?,
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @POST("api/v1/statuses/{id}/favourite")
    fun favoriteStatus(@Path("id") statusId: String): Call<Status>

    @POST("api/v1/statuses/{id}/unfavourite")
    fun unfavoriteStatus(@Path("id") statusId: String): Call<Status>
}