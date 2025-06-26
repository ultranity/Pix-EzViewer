package com.perol.asdpl.pixivez.objects

import com.perol.asdpl.pixivez.base.KotlinUtil.launchCatching
import com.perol.asdpl.pixivez.data.RetrofitRepository
import com.perol.asdpl.pixivez.data.model.Illust
import com.perol.asdpl.pixivez.data.model.User
import com.perol.asdpl.pixivez.services.PxEZApp
import kotlinx.coroutines.MainScope

object InteractionUtil {
    private val retrofit: RetrofitRepository = RetrofitRepository.getInstance()
    fun visRestrictTag(item: Illust): String = visRestrictTag(PxEZApp.R18Private && item.isR18)
    fun visRestrictTag(restrict: Boolean): String = if (restrict) "private" else "public"
    val failedActions = mutableListOf<Pair<Int, String>>() //TODO: persistence
    fun like(
        item: Illust,
        tagList: List<String>? = null,
        forcePrivate: Boolean = false,
        callback: () -> Unit = { }
    ) =
        MainScope().launchCatching(
            {
                retrofit.api.postLikeIllust(
                    item.id,
                    if (forcePrivate) visRestrictTag(true) else visRestrictTag(item),
                    tagList
                )
            }, {
                item.is_bookmarked = true
                callback()
            }, {
                failedActions.add(Pair(item.id, "like"))
                CrashHandler.instance.e(
                    "interaction",
                    "failed to bookmark ${item.id} ${item.title}",
                    it,
                    true
                )
            }, retryCount = 3
        )

    fun unlike(item: Illust, callback: () -> Unit = { }) = MainScope().launchCatching({
        retrofit.api.postUnlikeIllust(item.id)
    }, {
        item.is_bookmarked = false
        callback()
    }, {
        failedActions.add(Pair(item.id, "unlike"))
        CrashHandler.instance.e(
            "interaction",
            "failed to del bookmark ${item.id} ${item.title}",
            it,
            true
        )
    }, retryCount = 1)

    /*
     * set private flag by check if need_restrict
     */
    inline fun follow(item: Illust, noinline callback: () -> Unit = {}) {
        follow(item.user, item.restricted, callback)
    }

    fun follow(user: User, private: Boolean = false, callback: () -> Unit = {}) =
        MainScope().launchCatching({
            retrofit.api.postFollowUser(user.id, visRestrictTag(private))
        }, {
            user.is_followed = true
            callback()
        }, {
            failedActions.add(Pair(user.id, "follow"))
            CrashHandler.instance.e(
                "interaction",
                "failed to follow ${user.id} ${user.name}",
                it,
                true
            )
        }, retryCount = 3)

    fun unfollow(user: User, callback: () -> Unit = {}) = MainScope().launchCatching({
        retrofit.api.postUnfollowUser(user.id)
    }, {
        user.is_followed = false
        callback()
    }, {
        failedActions.add(Pair(user.id, "unfollow"))
        CrashHandler.instance.e(
            "interaction",
            "failed to unfollow ${user.id} ${user.name}",
            it,
            true
        )
    }, retryCount = 1)
}
