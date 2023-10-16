package com.perol.asdpl.pixivez.ui.home.trend

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Calendar

class CalendarViewModel:ViewModel() {
    //val pool = RecyclerView.RecycledViewPool()
    var picDateShare = MutableLiveData<String?>()

    class YMD(
        var year: Int = -1,
        var month: Int = -1,
        var day: Int = -1
    ) {
        fun setDate(
            year: Int = -1,
            month: Int = -1,
            day: Int = -1
        ) {
            this.year = year
            this.month = month
            this.day = day
        }

        fun fromCalendar(calendar: Calendar): YMD {
            year = calendar.get(Calendar.YEAR)
            month = calendar.get(Calendar.MONTH)
            day = calendar.get(Calendar.DAY_OF_MONTH)
            return this
        }

        fun toStr(): String = "$year-${month+1}-$day"

        companion object {
            fun now(): YMD {
                return YMD().fromCalendar(Calendar.getInstance())
            }
        }
    }
    val ymd = YMD().fromCalendar(Calendar.getInstance())
    fun getDateStr() = YMD.now().toStr()
}