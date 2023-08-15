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

    fun buildDetailRequest(id: String, messageNumber: UShort, mdc: String): Request {
        val request = Request.Builder().url("$serverBaseAddress/d/$id/$messageNumber?mdc=$mdc")
        return request.build()
    }
}