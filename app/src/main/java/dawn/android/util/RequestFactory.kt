/*	Copyright (c) 2023 Laurenz Werner

	This file is part of Dawn.

	Dawn is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Dawn is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Dawn.  If not, see <http://www.gnu.org/licenses/>.
*/

package dawn.android.util

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object RequestFactory {

    private lateinit var serverBaseAddress: String

    fun setMessageServerAddress(address: String) {
        serverBaseAddress = address
    }

    fun buildRcvRequest(id: String, messageNumber: UShort): Request {
        val request = Request.Builder().url("$serverBaseAddress/rcv/$id/$messageNumber")
        return request.build()
    }

    fun buildDetailRequest(id: String, messageNumber: UShort, mdc: String): Request {
        val request = Request.Builder().url("$serverBaseAddress/d/$id/$messageNumber?mdc=$mdc")
        return request.build()
    }

    fun buildSndRequest(id: String, messageCiphertext: ByteArray, mdc: String): Request {
        val request = Request
            .Builder()
            .url("$serverBaseAddress/snd/$id?mdc=$mdc")
            .post(
                messageCiphertext.toRequestBody(
                    "application/octet-stream".toMediaTypeOrNull()
                )
            )
        return request.build()
    }

    fun buildWhoRequest(handle: String, initSecret: String?): Request {
        val initSecretParameter = initSecret?: ""
        val request = Request.Builder().url("$serverBaseAddress/who/$handle?init_secret=$initSecretParameter")
        return request.build()
    }
}