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

import dawn.android.data.Chat
import dawn.android.data.Profile
import dawn.android.data.ReceivedInitRequest
import dawn.android.data.Result
import dawn.android.data.Result.Companion.err
import dawn.android.data.Result.Companion.ok
import dawn.android.data.SentInitRequest
import kotlinx.serialization.json.Json
import java.io.File

object ChatManager {
    private var chatCache: HashMap<String, Chat> = HashMap()
    private var sentRequestsCache: HashMap<String, SentInitRequest> = HashMap()
    private var receivedRequestsCache: HashMap<String, ReceivedInitRequest> = HashMap()
    private var profileCache: HashMap<String, Profile> = HashMap()
    private lateinit var chatsPath: File
    private lateinit var sentRequestsPath: File
    private lateinit var receivedRequestsPath: File
    private lateinit var profilePath: File

    fun init(storageRoot: File) {
        chatsPath = File(storageRoot, "chats")
        sentRequestsPath = File(storageRoot, "sentRequests")
        receivedRequestsPath = File(storageRoot, "receivedRequests")
        profilePath = File(storageRoot, "profiles")
    }

    fun getProfile(id: String): Result<Profile, String> {
        if(!profileCache.contains(id)) {
            try {
                val serializedProfile = String(DataManager.readFile(id, profilePath)!!, Charsets.UTF_8)
                val profile: Profile = Json.decodeFromString(serializedProfile)
                profileCache[id] = profile
            }
            catch(e: Exception) {
                return err("getProfile: Error getting profile $id: $e")
            }
        }
        return ok(profileCache[id]?: return err("getProfile: Profile disappeared from cache"))
    }
}