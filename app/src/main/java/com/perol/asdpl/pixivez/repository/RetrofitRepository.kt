/*
 * MIT License
 *
 * Copyright (c) 2020 ultranity
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

package com.perol.asdpl.pixivez.repository

import android.util.Log
import com.google.gson.Gson
import com.perol.asdpl.pixivez.networks.ReFreshFunction
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.responses.BookMarkDetailResponse
import com.perol.asdpl.pixivez.responses.BookMarkTagsResponse
import com.perol.asdpl.pixivez.responses.IllustCommentsResponse
import com.perol.asdpl.pixivez.responses.IllustDetailResponse
import com.perol.asdpl.pixivez.responses.IllustNext
import com.perol.asdpl.pixivez.responses.ListUserResponse
import com.perol.asdpl.pixivez.responses.PixivResponse
import com.perol.asdpl.pixivez.responses.RecommendResponse
import com.perol.asdpl.pixivez.responses.SearchIllustResponse
import com.perol.asdpl.pixivez.responses.SearchUserResponse
import com.perol.asdpl.pixivez.responses.SpotlightResponse
import com.perol.asdpl.pixivez.responses.TrendingtagResponse
import com.perol.asdpl.pixivez.responses.UgoiraMetadataResponse
import com.perol.asdpl.pixivez.responses.UserDetailResponse
import com.perol.asdpl.pixivez.services.AppApiPixivService
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.HttpException

class RetrofitRepository {

    var appApiPixivService: AppApiPixivService = RestClient.retrofitAppApi.create(AppApiPixivService::class.java)
    private var gifApiPixivService: AppApiPixivService = RestClient.gifAppApi.create(AppApiPixivService::class.java)
    var reFreshFunction: ReFreshFunction = ReFreshFunction.getInstance()

    init {
        if (System.currentTimeMillis() - AppDataRepository.pre.getLong("lastRefresh", 0)
        > 59 * 60 * 1000
        ) {
            val init = Observable.just(1).flatMap {
                Log.d("init", "Observable init")
                reFreshFunction.reFreshToken()
            }.subscribe({
                Log.d("Retrofit", "Observable inited")
            }, {}, {})
        }
        Log.d("Retrofit", "RetrofitRepository inited")
    }

    fun getLikeIllust(userid: Long, pub: String, tag: String?): Observable<IllustNext> = Request(appApiPixivService.getLikeIllust(userid, pub, tag))

    fun getIllustBookmarkTags(userid: Long, pub: String): Observable<BookMarkTagsResponse> = Request(appApiPixivService.getIllustBookmarkTags(userid, pub))

    fun getUserIllusts(id: Long, type: String): Observable<IllustNext> = Request(appApiPixivService.getUserIllusts(id, type))

    fun getIllustRelated(id: Long): Observable<RecommendResponse> = Request(appApiPixivService.getIllustRecommended(id))

    fun getIllustRanking(mode: String, date: String?): Observable<IllustNext> = Request(appApiPixivService.getIllustRanking(mode, date))

    fun getSearchAutoCompleteKeywords(newText: String): Observable<PixivResponse> = Request(appApiPixivService.getSearchAutoCompleteKeywords(newText))

    fun getPixivison(category: String): Observable<SpotlightResponse> = Request(appApiPixivService.getPixivisionArticles(category))

    fun getRecommend(): Observable<RecommendResponse> = Request(appApiPixivService.getRecommend())

    fun getSearchIllustPreview(
        word: String,
        sort: String,
        search_target: String?,
        bookmark_num: Int?,
        duration: String?
    ): Observable<SearchIllustResponse> {
        return Request(appApiPixivService.getSearchIllustPreview(word, sort, search_target, bookmark_num, duration))
    }

    fun getSearchIllust(
        word: String,
        sort: String,
        search_target: String?,
        start_date: String?,
        end_date: String?,
        bookmark_num: Int?
    ): Observable<SearchIllustResponse> = Request(appApiPixivService.getSearchIllust(word, sort, search_target, start_date, end_date, bookmark_num))

    fun postUserProfileEdit(part: MultipartBody.Part): Observable<ResponseBody> = Request(appApiPixivService.postUserProfileEdit(part))

    fun getUserFollower(user_id: Long): Observable<SearchUserResponse> = Request(appApiPixivService.getUserFollower(user_id))

    fun getUserFollowing(user_id: Long, restrict: String): Observable<SearchUserResponse> = Request(appApiPixivService.getUserFollowing(user_id, restrict))

    fun getFollowIllusts(restrict: String): Observable<IllustNext> = Request(appApiPixivService.getFollowIllusts(restrict))

    fun getIllustBookmarkUsers(illust_id: Long, offset: Int = 0): Observable<ListUserResponse> = Request(appApiPixivService.getIllustBookmarkUsers(illust_id, offset))

    fun getSearchUser(string: String): Observable<SearchUserResponse> =
        Request(appApiPixivService.getSearchUser(string))

    fun getIllustComments(
        illust_id: Long,
        offset: Int = 0,
        include_total_comments: Boolean = false
    ): Observable<IllustCommentsResponse> =
        Request(appApiPixivService.getIllustComments(illust_id, offset, include_total_comments))

    fun postIllustComment(
        illust_id: Long,
        comment: String,
        parent_comment_id: Int?
    ): Observable<ResponseBody> =
        Request(appApiPixivService.postIllustComment(illust_id, comment, parent_comment_id))

    fun getIllustTrendTags(): Observable<TrendingtagResponse> =
        Request(appApiPixivService.getIllustTrendTags())

    fun postLikeIllust(illust_id: Long): Observable<ResponseBody> =
        Request(appApiPixivService.postLikeIllust(illust_id, "public", null))

    fun postLikeIllustWithTags(
        illust_id: Long,
        string: String = "public",
        tagList: ArrayList<String>? = null
    ): Observable<ResponseBody> = Request(appApiPixivService.postLikeIllust(illust_id, string, tagList))

    fun getIllust(illust_id: Long): Observable<IllustDetailResponse> = Request(appApiPixivService.getIllust(illust_id)).also { Log.d("getIllust", illust_id.toString()) }

    private suspend fun getIllustCor(long: Long): IllustDetailResponse? {
        var illustDetailResponse: IllustDetailResponse? = null
        try {
            illustDetailResponse = appApiPixivService.getIllustCor(long)
        } catch (e: Exception) {
            if (e is HttpException) {
                getIllustCor(long)
            }
        } finally {
            if (illustDetailResponse == null) {
                getIllustCor(long)
            }
        }
        return illustDetailResponse
    }

    fun postUnlikeIllust(long: Long): Observable<ResponseBody> = Request(appApiPixivService.postUnlikeIllust(long))

    fun postFollowUser(long: Long, restrict: String): Observable<ResponseBody> = Request(appApiPixivService.postFollowUser(long, restrict))

    fun postUnfollowUser(long: Long): Observable<ResponseBody> = Request(appApiPixivService.postUnfollowUser(long))

    fun getUgoiraMetadata(long: Long): Observable<UgoiraMetadataResponse> = Request(appApiPixivService.getUgoiraMetadata(long))

    fun getGIFFile(string: String): Observable<ResponseBody> = Request(gifApiPixivService.getGIFFile(string))

    fun getBookmarkDetail(long: Long): Observable<BookMarkDetailResponse> = Request(appApiPixivService.getLikeIllustDetail(long))

    fun getUserDetail(userid: Long): Observable<UserDetailResponse> = Request(appApiPixivService.getUserDetail(userid))

    fun getUserRecommanded() = Request(appApiPixivService.getUserRecommended())

    private inline fun <reified T> Request(observable: Observable<T>): Observable<T> {
        return observable.map {
            // resetToken()
            Log.d("Retrofit", "Request ${T::class.java.canonicalName}")
            it
        }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
        .retryWhen {
            Log.d("Retrofit", "Request ${T::class.java.canonicalName} failed, call reFreshFunction")
            reFreshFunction.apply(it)
        }
    }

    fun <T> create(observable: Observable<T>): Observable<T> {
        return observable.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
            .retryWhen(reFreshFunction)
    }

    inline fun <reified T> getNext(url: String): Observable<T> = appApiPixivService.getUrl(url).flatMap {
            Log.d("Retrofit", "getNext ${T::class.java.simpleName} from $url")
            Observable.just(Gson().fromJson(it.string(), T::class.java))
        }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
        .retryWhen {
            Log.d("Retrofit", "Request ${T::class.java.canonicalName} failed, call reFreshFunction")
            reFreshFunction.apply(it)
        }

    fun getNextUser(url: String): Observable<SearchUserResponse> = getNext(url)

    fun getNextTags(url: String): Observable<BookMarkTagsResponse> = getNext(url)

    fun getNextUserIllusts(url: String): Observable<IllustNext> = getNext(url)

    fun getNextIllustRecommended(url: String): Observable<RecommendResponse> = getNext(url)

    fun getNextIllustComments(url: String): Observable<IllustCommentsResponse> = getNext(url)

    fun getNextPixivisionArticles(url: String): Observable<SpotlightResponse> = getNext(url)

    companion object {
        private var instance: RetrofitRepository? = null
        fun getInstance(): RetrofitRepository {
            if (instance == null) {
                synchronized(RetrofitRepository::class.java) {
                    if (instance == null) {
                        instance = RetrofitRepository()
                    }
                }
            }
            return instance!!
        }
    }
}
