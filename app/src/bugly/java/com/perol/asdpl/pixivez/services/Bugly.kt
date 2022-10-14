package com.perol.asdpl.pixivez.services

import android.os.Environment
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.R
import com.tencent.bugly.Bugly
import com.tencent.bugly.beta.Beta
import com.tencent.bugly.crashreport.BuglyLog

inline fun checkUpdate(){
    Beta.checkUpgrade()
}

inline fun initBugly(context:PxEZApp) {
    Beta.upgradeDialogLayoutId = R.layout.upgrade_dialog
    Beta.enableHotfix = false
    Beta.initDelay = 1 * 1000
    //Beta.autoCheckUpgrade = pre.getBoolean("autocheck",true)
    Beta.storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    Bugly.init(context, "5f21ff45b7", BuildConfig.DEBUG)
    if(BuildConfig.DEBUG)
        Bugly.setAppChannel(context,"DeBug")
    else
        Bugly.setAppChannel(context,"InApp")
    BuglyLog.d("settings", context.pre.all.toString())
}