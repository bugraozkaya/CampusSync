package com.bugra.campussync.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

object SessionManager {
    private val _logoutEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvent: SharedFlow<Unit> = _logoutEvent

    fun triggerLogout() {
        CoroutineScope(Dispatchers.Default).launch {
            _logoutEvent.emit(Unit)
        }
    }
}
