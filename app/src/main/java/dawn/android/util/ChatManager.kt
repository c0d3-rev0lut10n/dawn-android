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

import java.io.File

object ChatManager {
    private var cache: HashMap<String, String> = HashMap()
    private lateinit var chatsPath: File
    private lateinit var sentRequestsPath: File
    private lateinit var receivedRequestsPath: File

    fun init(storageRoot: File) {
        chatsPath = File(storageRoot, "chats")
        sentRequestsPath = File(storageRoot, "sentRequests")
        receivedRequestsPath = File(storageRoot, "receivedRequests")
    }
}