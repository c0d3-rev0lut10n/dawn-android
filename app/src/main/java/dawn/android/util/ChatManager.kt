/*
 * Copyright (c) 2023-2024  Laurenz Werner
 *
 * This file is part of Dawn.
 *
 * Dawn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dawn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dawn.  If not, see <http://www.gnu.org/licenses/>.
 */

package dawn.android.util

import dawn.android.GenId
import dawn.android.LibraryConnector
import dawn.android.annotation.ConcurrentAnnotation
import dawn.android.data.Chat
import dawn.android.data.ChatType
import dawn.android.data.Default
import dawn.android.data.Keypair
import dawn.android.data.Location
import dawn.android.data.Ok
import dawn.android.data.Profile
import dawn.android.data.Regex
import dawn.android.data.Result
import dawn.android.data.Result.Companion.err
import dawn.android.data.Result.Companion.ok
import dawn.android.data.serialized.SerializedChat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ChatManager {
    private var chatCache: HashMap<String, Chat> = HashMap()
    private var profileCache: HashMap<String, Profile> = HashMap()
    private var chatsPath: File = DataManager.getLocation(Location.CHATS)
    private var profilePath: File = DataManager.getLocation(Location.PROFILES)

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
        return ok(profileCache[id]?: return err("getProfile: Profile $id disappeared from cache"))
    }

    fun newProfile(profilePrototype: Profile): Result<Profile, String> {
        // validate profile data
        if(profilePrototype.dataId != Default.ToBeDeterminedDataId) return err("dataId must be Default.ToBeDeterminedDataId, got ${profilePrototype.dataId}")
        if(profilePrototype.name.contains("\n", true) || profilePrototype.name.isEmpty()) return err("invalid name ${profilePrototype.name}")
        if(!Regex.handle.matches(profilePrototype.handle)) return err("handle invalid, got ${profilePrototype.handle}")

        // generate data ID
        var dataId: GenId? = null
        val profileDirs = profilePath.listFiles() // is not null since at least the own profile exists
        val profileDirNames = ArrayList<String>()
        for (profile in profileDirs!!) {
            profileDirNames.add(profile.name)
        }
        for (i in 1..100) {
            // choose a random ID that is not used
            val dataIdResult = LibraryConnector.mGenId()
            if (dataIdResult.isErr()) return err("could not generate data ID")
            dataId = dataIdResult.unwrap()
            if (dataId.id!! !in profileDirNames) break
            if (i == 100) return err("could not generate data ID")
        }
        profilePrototype.dataId = dataId!!.id!!

        try {
            profileCache[dataId.id!!] = profilePrototype
            DataManager.writeFile(dataId.id!!, profilePath, Json.encodeToString(profilePrototype).toByteArray(Charsets.UTF_8), false)
        }
        catch (e: Exception) {
            return err("newProfile: Error saving chat ${dataId.id}: $e")
        }

        return ok(profilePrototype)
    }

    @ConcurrentAnnotation
    fun updateProfile(profile: Profile): Result<Ok, String> {
        profileCache[profile.dataId] = profile
        try {
            DataManager.writeFile(profile.dataId, profilePath, Json.encodeToString(profile).toByteArray(Charsets.UTF_8), true)
        }
        catch (e: Exception) {
            return err("updateProfile: Error saving profile ${profile.dataId}: ${e.printStackTrace()}")
        }
        return ok(Ok)
    }

    fun getChat(id: String): Result<Chat, String> {
        if(!chatCache.contains(id)) {
            try {
                val serializedChatString = String(DataManager.readFile(id, chatsPath)!!, Charsets.UTF_8)
                val serializedChat: SerializedChat = Json.decodeFromString(serializedChatString)
                val chat = Chat.fromSerialized(serializedChat)
                chatCache[id] = chat
            }
            catch (e: Exception) {
                return err("getChat: Error getting chat $id: $e")
            }
        }
        return ok(chatCache[id]?: return err("getChat: Chat $id disappeared from cache"))
    }

    fun getAllChats(): HashMap<String, Chat> {
        val chatDirs = chatsPath.listFiles() ?: return HashMap()
        for(dir in chatDirs) {
            if(!chatCache.contains(dir.name))
                getChat(dir.name)
        }
        return chatCache
    }

    fun getChatsToPoll(timestamp: String): HashMap<String, Chat> {
        val chatsToPoll = HashMap<String, Chat>()
        getAllChats()
        for(chat in chatCache) {
            if(chat.value.idStamp == timestamp) {
                chatsToPoll[chat.key] = chat.value
            }
        }
        return chatsToPoll
    }

    fun getOldestIdStampToPoll(): String {
        getAllChats()
        var oldestStamp = LibraryConnector.mGetCurrentTimestamp().unwrap().timestamp!!
        for(chat in chatCache) {
            if(chat.value.idStamp.toUInt() < oldestStamp.toUInt()) {
                oldestStamp = chat.value.idStamp
            }
        }
        return oldestStamp
    }

    @ConcurrentAnnotation
    fun updateChat(chat: Chat): Result<Ok, String> {
        chatCache[chat.dataId] = chat
        try {
            val serializedChat = chat.intoSerializable()
            DataManager.writeFile(chat.dataId, chatsPath, Json.encodeToString(serializedChat).toByteArray(Charsets.UTF_8), true)
        }
        catch (e: Exception) {
            return err("updateChat: Error saving chat ${chat.dataId}: ${e.printStackTrace()}")
        }
        return ok(Ok)
    }

    fun newChat(chatPrototype: Chat): Result<Chat, String> {
        if(!chatPrototype.id.matches(Regex.ID)) return err("invalid ID ${chatPrototype.id}")
        if(!chatPrototype.idSalt.matches(Regex.IdSalt)) return err("invalid ID salt ${chatPrototype.idSalt}")
        if(chatPrototype.name.contains("\n", true) || chatPrototype.name.isEmpty()) return err("invalid name ${chatPrototype.name}")
        if(chatPrototype.dataId != Default.ToBeDeterminedDataId) return err("dataId must be Default.ToBeDeterminedDataId, got ${chatPrototype.dataId}")
        var dataId: GenId? = null // we have to initialize with null because the compiler will complain otherwise (even though dataId will be always initialized when the chatDir File gets constructed

        val chatDirs = chatsPath.listFiles()
        if(chatDirs == null) {
            // there are no chats, we can freely choose an ID
            val dataIdResult = LibraryConnector.mGenId()
            if(dataIdResult.isErr()) return err("could not generate data ID")
            dataId = dataIdResult.unwrap()
        }
        else {
            val chatDirNames = ArrayList<String>()
            for(chat in chatDirs) {
                chatDirNames.add(chat.name)
            }
            for (i in 1..100) {
                // choose a random ID that is not used
                val dataIdResult = LibraryConnector.mGenId()
                if (dataIdResult.isErr()) return err("could not generate data ID")
                dataId = dataIdResult.unwrap()
                if (dataId.id!! !in chatDirNames) break
                if(i == 100) return err("could not generate data ID")
            }
        }
        chatPrototype.dataId = dataId!!.id!!
        try {
            val serializedChat = chatPrototype.intoSerializable()
            DataManager.writeFile(dataId.id!!, chatsPath, Json.encodeToString(serializedChat).toByteArray(Charsets.UTF_8), false)
            chatCache[dataId.id!!] = chatPrototype
        }
        catch (e: Exception) {
            return err("newChat: Error saving chat ${chatPrototype.id}: $e")
        }
        return ok(chatPrototype)
    }

    fun newChat(id: String, idStamp: String, idSalt: String, name: String, type: ChatType, ownKyber: Keypair, ownCurve: Keypair, ownPFS: String, remotePFS: String, pfsSalt: String, mdcSeed: String, associatedProfileId: String?): Result<Chat, String> {
        if(!id.matches(Regex.ID)) return err("invalid ID $id")
        if(!idSalt.matches(Regex.IdSalt)) return err("invalid ID salt $idSalt")
        if(name.contains("\n", true) || name.isEmpty()) return err("invalid name $name")
        var dataId: GenId? = null // we have to initialize with null because the compiler will complain otherwise (even though dataId will be always initialized when the chatDir File gets constructed

        val chatDirs = chatsPath.listFiles()
        if(chatDirs == null) {
            // there are no chats, we can freely choose an ID
            val dataIdResult = LibraryConnector.mGenId()
            if(dataIdResult.isErr()) return err("could not generate data ID")
            dataId = dataIdResult.unwrap()
        }
        else {
            val chatDirNames = ArrayList<String>()
            for(chat in chatDirs) {
                chatDirNames.add(chat.name)
            }
            for (i in 1..100) {
                // choose a random ID that is not used
                val dataIdResult = LibraryConnector.mGenId()
                if (dataIdResult.isErr()) return err("could not generate data ID")
                dataId = dataIdResult.unwrap()
                if (dataId.id!! !in chatDirNames) break
                if(i == 100) return err("could not generate data ID")
            }
        }
        val chat = Chat(
            dataId = dataId!!.id!!,
            id = id,
            idStamp = idStamp,
            idSalt = idSalt,
            lastMessageId = 0U,
            name = name,
            messages = ArrayList(),
            type = type,
            ownKyber = ownKyber,
            ownCurve = ownCurve,
            ownPFS = ownPFS,
            remotePFS = remotePFS,
            pfsSalt = pfsSalt,
            mdcSeed = mdcSeed,
            associatedProfileId = associatedProfileId
        )
        try {
            val serializedChat = chat.intoSerializable()
            DataManager.writeFile(dataId.id!!, chatsPath, Json.encodeToString(serializedChat).toByteArray(Charsets.UTF_8), false)
            chatCache[dataId.id!!] = chat
        }
        catch (e: Exception) {
            return err("newChat: Error saving chat $id: $e")
        }
        return ok(chat)
    }
}