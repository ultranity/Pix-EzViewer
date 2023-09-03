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

import kotlinx.serialization.Serializable

//     "user" : {"id":6900078,"name":"にゃんこ茶（Aliter）","account":"aliter","profile_image_urls":{"medium":"https://i.pximg.net/user-profile/img/2018/08/18/13/33/23/14652189_7fc9018d975d494657755b007d178a3f_170.jpg"},"comment":"Aliterと申します。\r\nいつもお気に入り、ブクマ、フォロー等々ありがとうございます！\r\n皆さんと一绪に交流していきたいと思います。\r\n\r\nThis is Aliter.\r\nThank you all for your comments, favorites and bookmarks.\r\nI also hope to comunicate with everyone.\r\n\r\n这里是Aliter；\r\n感谢各位的点赞、收藏与关注；\r\n同时也希望能和大家多多交流。\r\n\r\n這裡是Aliter；\r\n感謝各位的點贊、收藏與關注；\r\n同時也希望能和大家多多交流。\r\n\r\nTwitter: @aliter_c\r\n微博链接（weibo link）：http://weibo.com/aliter08\r\n半次元链接（bcy link）：https://bcy.net/u/1561764\r\n\r\n欢迎勾搭\r\n\r\n暂不接受约稿","is_followed":true}
//      "profile" : {"webpage":null,"gender":"male","birth":"","birth_day":"11-22","birth_year":0,"region":"中華人民共和国 (中国)","address_id":48,"country_code":"CN","job":"クリエーター系","job_id":5,"total_follow_users":397,"total_mypixiv_users":37,"total_illusts":80,"total_manga":0,"total_novels":0,"total_illust_bookmarks_public":81,"total_illust_series":0,"background_image_url":"https://s.pximg.net/common/images/bg/star02.png","twitter_account":"","twitter_url":null,"pawoo_url":"https://pawoo.net/oauth_authentications/6900078?provider=pixiv","is_premium":false,"is_using_custom_profile_image":true}
//     "profile_publicity": {"gender":"public","region":"public","birth_day":"public","birth_year":"public","job":"public","pawoo":true}
//     "workspace" : {"pc":"台式机","monitor":"Samsung SyncMaster2333","tool":"SAI、PhotshopCS6","scanner":"WIA CanoScan Lide 110","tablet":"Wacom Cintip 13HD","mouse":"","printer":"","desktop":"","music":"ACGのこと","desk":"","chair":"","comment":"","workspace_image_url":null}
@Serializable
class UserDetail(
    val user: User,
    val profile: ProfileBean,
    val profile_publicity: ProfilePublicityBean,
    val workspace: WorkspaceBean,
)

/**
 * webpage : null
 * gender : male
 * birth :
 * birth_day : 11-22
 * birth_year : 0
 * region : 中華人民共和国 (中国)
 * address_id : 48
 * country_code : CN
 * job : クリエーター系
 * job_id : 5
 * total_follow_users : 397
 * total_mypixiv_users : 37
 * total_illusts : 80
 * total_manga : 0
 * total_novels : 0
 * total_novel_series :0
 * total_illust_bookmarks_public : 81
 * total_illust_series : 0
 * background_image_url : https://s.pximg.net/common/images/bg/star02.png
 * twitter_account :
 * twitter_url : null
 * pawoo_url : https://pawoo.net/oauth_authentications/6900078?provider=pixiv
 * is_premium : false
 * is_using_custom_profile_image : true
 */
@Serializable
class ProfileBean(
    val webpage: String = "",
    val gender: String?,
    val birth: String?,
    val birth_day: String?,
    val birth_year: Int = 0,
    val region: String?,
    val address_id: Int = 0,
    val country_code: String?,
    val job: String?,
    val job_id: Int = 0,
    val total_follow_users: Int = 0,
    val total_mypixiv_users: Int = 0,
    val total_illusts: Int = 0,
    val total_manga: Int = 0,
    val total_novels: Int = 0,
    val total_illust_bookmarks_public: Int = 0,
    val total_illust_series: Int = 0,
    val total_novel_series: Int = 0,
    val background_image_url: String?,
    val twitter_account: String?,
    val twitter_url: String?,
    val pawoo_url: String?,
    val is_premium: Boolean = false,
    val is_using_custom_profile_image: Boolean = false
)

/**
 * gender : public
 * region : public
 * birth_day : public
 * birth_year : public
 * job : public
 * pawoo : true
 */
@Serializable
class ProfilePublicityBean(
    val gender: String,
    val region: String,
    val birth_day: String,
    val birth_year: String,
    val job: String,
    val pawoo: Boolean = false
)

/**
 * pc : 台式机
 * monitor : Samsung SyncMaster2333
 * tool : SAI、PhotshopCS6
 * scanner : WIA CanoScan Lide 110
 * tablet : Wacom Cintip 13HD
 * mouse :
 * printer :
 * desktop :
 * music : ACGのこと
 * desk :
 * chair :
 * comment :
 * workspace_image_url : null
 */
@Serializable
class WorkspaceBean(
    val pc: String,
    val monitor: String,
    val tool: String,
    val scanner: String,
    val tablet: String,
    val mouse: String,
    val printer: String,
    val desktop: String,
    val music: String,
    val desk: String,
    val chair: String,
    val comment: String,
    val workspace_image_url: String?
)
