package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.domain.push.DevicesRepository
import com.ipb.castelobranco.core.domain.push.PushRegistrationScheduler
import com.ipb.castelobranco.core.domain.push.RegisteredTokenStore

class FakePushRegistrationScheduler : PushRegistrationScheduler {
    var scheduleCalls = 0
        private set
    var cancelCalls = 0
        private set

    override fun schedule() {
        scheduleCalls++
    }

    override fun cancel() {
        cancelCalls++
    }
}

class FakeDevicesRepository : DevicesRepository {
    val registered = mutableListOf<String>()
    val unregistered = mutableListOf<String>()
    var registerResult: Result<Unit> = Result.success(Unit)
    var unregisterResult: Result<Unit> = Result.success(Unit)

    /** Runs before answering; lets a test make the call slow. */
    var beforeUnregister: suspend () -> Unit = {}

    override suspend fun register(token: String): Result<Unit> {
        registered += token
        return registerResult
    }

    override suspend fun unregister(token: String): Result<Unit> {
        beforeUnregister()
        unregistered += token
        return unregisterResult
    }
}

class FakeRegisteredTokenStore(var token: String? = null) : RegisteredTokenStore {
    override suspend fun get(): String? = token

    override suspend fun set(token: String) {
        this.token = token
    }

    override suspend fun clear() {
        token = null
    }
}
