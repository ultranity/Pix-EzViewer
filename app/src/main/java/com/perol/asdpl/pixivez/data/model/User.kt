/*
 * MIT License
 *
 * Copyright (c) 2022 ultranity
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

import androidx.lifecycle.MutableLiveData
import com.perol.asdpl.pixivez.base.EmptyAsNullJsonTransformingSerializer
import com.perol.asdpl.pixivez.objects.CopyFrom
import com.perol.asdpl.pixivez.objects.UserCacheRepo
import com.perol.asdpl.pixivez.ui.settings.BlockViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encoding.Decoder
import java.util.WeakHashMap

/**
 * id : 19887389
 * name : kkrin1013
 * account : kkrin1013
 * profile_image_urls : {"medium":"https://i.pximg.net/user-profile/img/2017/12/04/10/46/10/13525660_fc11c3a777f794271125c1f7ab043168_170.png"}
 * comment:"Aliterと申します。\r\nいつもお気に入り、ブクマ、フォロー等々ありがとうございます！\r\n皆さんと一绪に交流していきたいと思います。\r\n\r\nThis is Aliter.\r\nThank you all for your comments, favorites and bookmarks.\r\nI also hope to comunicate with everyone.\r\n\r\n这里是Aliter；\r\n感谢各位的点赞、收藏与关注；\r\n同时也希望能和大家多多交流。\r\n\r\n這裡是Aliter；\r\n感謝各位的點贊、收藏與關注；\r\n同時也希望能和大家多多交流。\r\n\r\nTwitter: @aliter_c\r\n微博链接（weibo link）：http://weibo.com/aliter08\r\n半次元链接（bcy link）：https://bcy.net/u/1561764\r\n\r\n欢迎勾搭\r\n\r\n暂不接受约稿",
 * is_followed : false
 */
@Serializable
data class UserX(
    var id: Int,
    var name: String,
    var account: String,
    var profile_image_urls: ProfileImageUrls,
    var comment: String = "",
    var is_access_blocking_user: Boolean = false,
) : CopyFrom<User> {
    @Transient
    private val binders = WeakHashMap<MutableLiveData<Boolean>, String>()
    fun addBinder(key: String, binder: MutableLiveData<Boolean>) {
        binders[binder] = key
    }
    fun removeBinder(binder: MutableLiveData<Boolean>) {
        binders.remove(binder)
    }
    var is_followed: Boolean = false
        set(value) {
            val updated = field != value
            if (updated) {
                field = value
                CoroutineScope(Dispatchers.Main).launch {
                    binders.forEach { it.key.value = value }
                }
            }
        }

    var is_blocked: Boolean? = null
        get() {
            if (field == null) {
                field = BlockViewModel.getBlockUIDs().contains(id)
            }
            return field
        }

    override fun copyFrom(src: User) {
        //TODO: id = src.id
        name = src.name
        account = src.account
        profile_image_urls = src.profile_image_urls
        comment = src.comment
        is_followed = src.is_followed
    }
}
// workaround until https://github.com/Kotlin/kotlinx.serialization/issues/1169 fixed
typealias User = @Serializable(with = UserSerializer::class) UserX

object UserSerializer : EmptyAsNullJsonTransformingSerializer<User>(User.serializer()) {
    override fun deserialize(decoder: Decoder): User {
        var user = super.deserialize(decoder)
        user = UserCacheRepo.update(user.id, user)
        return user
    }
}

/**
 * {"medium":"https://i.pximg.net/user-profile/img/2017/12/04/10/46/10/13525660_fc11c3a777f794271125c1f7ab043168_170.png"}
 */
@Serializable
data class ProfileImageUrls(
    val medium: String
)
