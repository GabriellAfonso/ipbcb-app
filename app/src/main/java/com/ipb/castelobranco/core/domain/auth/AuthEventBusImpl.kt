package com.ipb.castelobranco.core.domain.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthEventBusImpl @Inject constructor() : AuthEventBus {

    private val _events = MutableSharedFlow<AuthEventBus.Event>(extraBufferCapacity = 1)
    override val events: SharedFlow<AuthEventBus.Event> = _events.asSharedFlow()

    override fun emit(event: AuthEventBus.Event) {
        _events.tryEmit(event)
    }
}