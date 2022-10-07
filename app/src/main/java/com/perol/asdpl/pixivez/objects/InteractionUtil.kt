package com.perol.asdpl.pixivez.objects

import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.repository.RetrofitRepository
import com.perol.asdpl.pixivez.responses.Illust
import com.perol.asdpl.pixivez.services.PxEZApp
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

object InteractionUtil {
    val retrofitRepository: RetrofitRepository = RetrofitRepository.getInstance()
    val disposables = CompositeDisposable()
    fun Disposable.add() {
        disposables.add(this)
    }
    fun x_restrict(item: Illust): String{
        return if (PxEZApp.R18Private && item.x_restrict == 1) {
            "private"
        } else {
            "public"
        }
    }
    fun like(item:Illust, callback:Unit){

        retrofitRepository.postLikeIllustWithTags(item.id, x_restrict(item), null)
            .subscribe({ }, {}, {}).add()
    }


}