package com.perol.asdpl.pixivez.objects

import android.text.Html
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.AIType
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.responses.User
import com.perol.asdpl.pixivez.services.PxEZApp
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

object InteractionUtil {
    private val retrofitRepository: RetrofitRepository = RetrofitRepository.getInstance()
    private val disposables = CompositeDisposable()

    fun Disposable.add() {
        disposables.add(this)
    }

    fun need_restrict(item: Illust) = (PxEZApp.R18Private && item.x_restrict == 1)
    fun x_restrict(item: Illust): String = x_restrict(need_restrict(item))
    fun x_restrict(restrict: Boolean): String {
        return if (restrict) {
            "private"
        }
        else {
            "public"
        }
    }

    fun toDetailString(it: Illust, caption: Boolean = true) =
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
    // "image_urls:" + illust.image_urls.toString()

    fun like(item: Illust, tagList: ArrayList<String>? = null, callback: () -> Unit) {
        retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), tagList).subscribe({
            item.is_bookmarked = true
            callback()
        }, {}, {}).add()
    }

    fun unlike(item: Illust, callback: () -> Unit) {
        retrofitRepository.postUnlikeIllust(item.id).subscribe({
            item.is_bookmarked = false
            callback()
        }, {}, {}).add()
    }

    fun follow(item: Illust, callback: () -> Unit) {
        follow(item.user, need_restrict(item), callback)
    }

    fun follow(user: User, need_restrict: Boolean, callback: () -> Unit) {
        retrofitRepository.postFollowUser(user.id, x_restrict(need_restrict)).subscribe({
            user.is_followed = true
            callback()
        }, {}, {}).add()
    }

    fun unfollow(item: Illust, callback: () -> Unit) {
        unfollow(item.user, callback)
    }

    private fun unfollow(user: User, callback: () -> Unit) {
        retrofitRepository.postUnfollowUser(user.id).subscribe({
            user.is_followed = false
            callback()
        }, {}, {}).add()
    }

    fun onDestory() {
        disposables.clear()
    }
}
