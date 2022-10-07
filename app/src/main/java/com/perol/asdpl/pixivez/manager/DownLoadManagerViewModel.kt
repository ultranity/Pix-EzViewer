package com.perol.asdpl.pixivez.manager

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DownLoadManagerViewModel : ViewModel() {
    val progress = MutableLiveData<String>()
}
