package dawn.android.util

import okhttp3.Request

object RequestFactory {

    private lateinit var serverBaseAddress: String

    fun setMessageServerAddress(address: String) {
        serverBaseAddress = address
    }

    fun buildRcvRequest(id: String, messageNumber: UShort): Request {
        val request = Request.Builder().url("$serverBaseAddress/rcv/$id/$messageNumber")
        return request.build()
    }
}