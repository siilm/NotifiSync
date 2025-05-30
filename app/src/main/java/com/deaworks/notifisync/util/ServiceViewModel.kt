package com.deaworks.notifisync.util

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ServiceViewModel : ViewModel() {
    private val _isServiceRunning = mutableStateOf(false)
    val isServiceRunning: State<Boolean> get() = _isServiceRunning

    // 更新服务状态（通过监听或手动调用）
    fun updateServiceState(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
    }
}