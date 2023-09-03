package com.perol.asdpl.pixivez.objects

import android.text.Html
import com.perol.asdpl.pixivez.base.KotlinUtil.launchCatching
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.data.model.AIType
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.data.model.User
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.MainScope

object InteractionUtil {
    private val retrofit: RetrofitRepository = RetrofitRepository.getInstance()

    fun need_restrict(item: Illust) = (PxEZApp.R18Private && item.x_restrict == 1)
    fun visRestrictTag(item: Illust): String = visRestrictTag(need_restrict(item))
    fun visRestrictTag(restrict: Boolean): String {
        return if (restrict) {
            "private"
        } else {
            "public"
        }
    }

    fun toDetailString(it: Illust, caption: Boolean = true) =
        "${it.title} \n" +
                "id:${it.id} " + (if (caption) "caption:${Html.fromHtml(it.caption)}" else "") +
                "\nuser:${it.user.name} account:${it.user.account}\n" +
                "create_date:${it.create_date}\n" +
                "width:${it.width} height:${it.height}\n" +
                "tags:${it.tags}\n" +
                "total_bookmarks:${it.total_bookmarks} total_view:${it.total_view}\n" +
                "AI: ${AIType.values()[it.illust_ai_type]} book_style:${it.illust_book_style} tools:${it.tools}\n" +
                "type:${it.type} page_count:${it.page_count}\n" +
                "visible:${it.visible} is_muted:${it.is_muted} CAC:${it.comment_access_control}\n" +
                "sanity_level:${it.sanity_level} restrict:${it.restrict} x_restrict:${it.x_restrict}"
    // "meta_pages:" + illust.meta_pages.toString() + "\n" +
    // "meta_single_page:" + illust.meta_single_page.toString() + "\n" +
    // "image_urls:" + illust.meta_pages[0].toString()

    fun like(item: Illust, tagList: List<String>? = null, callback: () -> Unit = { }) =
        MainScope().launchCatching(
            { retrofit.api.postLikeIllust(item.id, visRestrictTag(item), tagList) }, {
                item.is_bookmarked = true
                callback()
            }, {
                CrashHandler.instance.e(
                    "interaction",
                    "failed to bookmark ${item.id} ${item.title}",
                    it,
                    true
                )
            })

    fun unlike(item: Illust, callback: () -> Unit = { }) = MainScope().launchCatching({
        retrofit.api.postUnlikeIllust(item.id)
    }, {
        item.is_bookmarked = false
        callback()
    }, {
        CrashHandler.instance.e(
            "interaction",
            "failed to del bookmark ${item.id} ${item.title}",
            it,
            true
        )
    })

    inline fun follow(item: Illust, noinline callback: () -> Unit) {
        follow(item.user, need_restrict(item), callback)
    }

    fun follow(user: User, private: Boolean = false, callback: () -> Unit) =
        MainScope().launchCatching({
            retrofit.api.postFollowUser(user.id, visRestrictTag(private))
        }, {
            user.is_followed = true
            callback()
        }, {
            CrashHandler.instance.e(
                "interaction",
                "failed to follow ${user.id} ${user.name}",
                it,
                true
            )
        })

    inline fun unfollow(item: Illust, noinline callback: () -> Unit) {
        unfollow(item.user, callback)
    }

    fun unfollow(user: User, callback: () -> Unit) = MainScope().launchCatching({
        retrofit.api.postUnfollowUser(user.id)
    }, {
        user.is_followed = false
        callback()
    }, {
        it.printStackTrace()
        CrashHandler.instance.e(
            "interaction",
            "failed to unfollow ${user.id} ${user.name}",
            it,
            true
        )
    })
}
