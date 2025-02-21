package com.example.postureguard.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _postureStatus = MutableLiveData<String>()
    val postureStatus: LiveData<String> = _postureStatus

    fun updatePostureStatus(status: String) {
        _postureStatus.value = status
    }

    // Other data handling logic can go here
}
