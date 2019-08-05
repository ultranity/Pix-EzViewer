package com.perol.asdpl.pixivez.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.perol.asdpl.pixivez.networks.SharedPreferencesServices
import com.perol.asdpl.pixivez.repository.AppDataRepository
import com.perol.asdpl.pixivez.repository.RetrofitRespository
import com.perol.asdpl.pixivez.responses.UserDetailResponse
import com.perol.asdpl.pixivez.services.PxEZApp
import com.perol.asdpl.pixivez.sql.AppDatabase
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class UserMViewModel : BaseViewModel() {
    var retrofitRespository = RetrofitRespository.getInstance()
    var userDetail = MutableLiveData<UserDetailResponse>()
    var isfollow = MutableLiveData<Boolean>()
    val prefer = SharedPreferencesServices.getInstance()

    init {

    }

    fun getdata(userid: Long) {
        retrofitRespository.getUserDetail(userid).subscribe({
            userDetail.value = it
            isfollow.value = it.user.isIs_followed
        }, {

        }, {})
    }

    fun onFabclick(userid: Long) {
        if (isfollow.value!!) {
            retrofitRespository.postunfollowUser(userid).subscribe({
                isfollow.value = false
            }, {}, {})
        } else {
            retrofitRespository.postfollowUser(userid, "public").subscribe({
                isfollow.value = true
            }, {}, {})
        }
    }

    fun onFabLongClick(userid: Long) {
        if (isfollow.value!!)
            retrofitRespository.postunfollowUser(userid).subscribe({
                isfollow.value = false
            }, {}, {})
        else retrofitRespository.postfollowUser(userid, "private").subscribe({
            isfollow.value = true
        }, {}, {})
    }

    fun isuser(id: Long) = Single.create<Boolean> { it ->
        launchUI {
            val pt = AppDataRepository.getUser()
            val isuser = id == pt.userid
            it.onSuccess(isuser)
        }

    }
}