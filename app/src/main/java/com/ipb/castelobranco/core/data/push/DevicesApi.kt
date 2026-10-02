package com.ipb.castelobranco.core.data.push

import com.ipb.castelobranco.core.network.ApiConstants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

private object DevicesEndpoints {
    const val DEVICES_PATH = "${ApiConstants.BASE_PATH}me/devices/"
    const val UNREGISTER_PATH = "${DEVICES_PATH}unregister/"
}

/** The token always travels in the body: in a URL, access logs would keep it. */
interface DevicesApi {

    @POST(DevicesEndpoints.DEVICES_PATH)
    suspend fun register(@Body body: DeviceTokenBody): Response<Unit>

    @POST(DevicesEndpoints.UNREGISTER_PATH)
    suspend fun unregister(@Body body: DeviceTokenBody): Response<Unit>
}

@Serializable
data class DeviceTokenBody(
    @SerialName("token") val token: String,
)
