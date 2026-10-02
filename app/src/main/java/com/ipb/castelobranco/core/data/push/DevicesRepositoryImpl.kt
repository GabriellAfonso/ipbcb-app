package com.ipb.castelobranco.core.data.push

import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.domain.push.DevicesRepository
import com.ipb.castelobranco.core.network.error.toAppError
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevicesRepositoryImpl @Inject constructor(
    private val api: DevicesApi,
) : DevicesRepository {

    override suspend fun register(token: String): Result<Unit> = call { api.register(DeviceTokenBody(token)) }

    override suspend fun unregister(token: String): Result<Unit> = call { api.unregister(DeviceTokenBody(token)) }

    private suspend fun call(request: suspend () -> Response<Unit>): Result<Unit> = runCatching {
        val response = request()
        if (!response.isSuccessful) throw response.toAppError()
    }.mapError()
}
