/*
 * MIT License
 *
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */
package com.perol.asdpl.pixivez.services

import com.perol.asdpl.pixivez.data.model.PixivOAuthResponse
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

const val client_id: String = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
const val client_secret: String = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"

interface OAuthSecureService {
    @FormUrlEncoded
    @POST("/auth/token")
    suspend fun postAuthToken(@FieldMap map: HashMap<String, Any>): PixivOAuthResponse

    @FormUrlEncoded
    @POST("/auth/token")
    suspend fun postAuthTokenX(
        @Field("code") code: String, // BB5_yxZvE1n3ECFH9KmPQV3Tu3pfaJqUp-5fuWP-msg
        @Field("code_verifier") code_verifier: String, // cwnuOPjfkM1f65Cqaf94Pu4EqFNZJcAzfDGKmrAr0vQ
        @Field("client_id") id: String = client_id,
        @Field("client_secret") secret: String = client_secret,
        @Field("grant_type") grant_type: String = "authorization_code", // authorization_code
        @Field("redirect_uri") redirect_uri: String = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback",
        @Field("include_policy") include_policy: Boolean = true
    ): PixivOAuthResponse

    @FormUrlEncoded
    @POST("/auth/token")
    suspend fun postRefreshAuthTokenX(
        @Field("refresh_token") refresh_token: String,
        @Field("client_id") id: String = client_id,
        @Field("client_secret") secret: String = client_secret,
        @Field("grant_type") grant_type: String = "refresh_token",
        @Field("include_policy") get_secure_url: Boolean = true
    ): PixivOAuthResponse
}
