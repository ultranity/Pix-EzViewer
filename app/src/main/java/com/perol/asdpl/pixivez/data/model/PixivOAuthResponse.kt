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
package com.perol.asdpl.pixivez.data.model

import com.perol.asdpl.pixivez.data.entity.UserEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * access_token : SrsL1Z7tGhM6yArRKeGkfaZ-ID3TTiTdFWtLmJtvWBA
 * expires_in : 3600
 * token_type : bearer
 * scope :
 * refresh_token : YeysYu5dgbu0tV1yckzLhJmUCMOyDmqTeriZfFh-UTw
 * user : {"profile_image_urls":{"px_16x16":"https://i.pximg.net/user-profile/img/2018/06/11/22/00/29/14348260_c1f2b130248005062b7c6c358812160a_16.jpg","px_50x50":"https://i.pximg.net/user-profile/img/2018/06/11/22/00/29/14348260_c1f2b130248005062b7c6c358812160a_50.jpg","px_170x170":"https://i.pximg.net/user-profile/img/2018/06/11/22/00/29/14348260_c1f2b130248005062b7c6c358812160a_170.jpg"},"id":"14713395","name":"Notsfsssf","account":"912756674","mail_address":"912756674@qq.com","is_premium":false,"x_restrict":2,"is_mail_authorized":true}
 * device_token : DEPRECATED
 */
@Serializable
class PixivOAuthResponse(
    val access_token: String,
    val expires_in: Int = 0,
    val token_type: String,
    val scope: String?,
    val refresh_token: String,
    val user: UserBean,
    //private val device_token: String?
    //ignore val respose: PixivOAuthResponse?
)

/**
 * profile_image_urls : {"px_16x16":"https://i.pximg.net/user-profile/img/2018/06/11/22/00/29/14348260_c1f2b130248005062b7c6c358812160a_16.jpg","px_50x50":"https://i.pximg.net/user-profile/img/2018/06/11/22/00/29/14348260_c1f2b130248005062b7c6c358812160a_50.jpg","px_170x170":"https://i.pximg.net/user-profile/img/2018/06/11/22/00/29/14348260_c1f2b130248005062b7c6c358812160a_170.jpg"}
 * id : 14713395
 * name : Notsfsssf
 * account : 912756674
 * mail_address : 912756674@qq.com
 * is_premium : false
 * x_restrict : 2
 * is_mail_authorized : true
 */
@Serializable
class UserBean(
    val id: Int,
    val name: String,
    val account: String,
    val mail_address: String,
    val is_premium: Boolean = false,
    val x_restrict: Int = 0,
    @SerialName("is_mail_authorized")
    val isMailAuthorized: Boolean,
    val profile_image_urls: ProfileImageUrlsBean,
    @SerialName("require_policy_agreement")
    val requirePolicyAgreement: Boolean = false,
){
    fun toUserEntity(refreshToken: String, accessToken: String) = UserEntity(
        id, name, mail_address,
        is_premium, x_restrict, profile_image_urls.px_170x170,
        "OAuth2", refreshToken, "Bearer $accessToken"
    )
}

/**
 * px_16x16 : https://s.pximg.net/common/images/no_profile_s.png
 * px_50x50 : https://i.pximg.net/user-profile/img/2018/06/11/22/00/29/14348260_c1f2b130248005062b7c6c358812160a_50.jpg
 * px_170x170 : https://i.pximg.net/user-profile/img/2018/06/11/22/00/29/14348260_c1f2b130248005062b7c6c358812160a_170.jpg
 */
@Serializable
class ProfileImageUrlsBean(
    val px_16x16: String?,
    val px_50x50: String?,
    val px_170x170: String
)
