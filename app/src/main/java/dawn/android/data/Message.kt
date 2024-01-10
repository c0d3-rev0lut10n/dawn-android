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

package dawn.android.data

import dawn.android.data.Result.Companion.err
import dawn.android.data.Result.Companion.ok
import dawn.android.data.serialized.SerializedMessage
import dawn.android.util.ChatManager
import dawn.android.util.DataManager
import java.io.File

class Message(
    val chatDataId: String,
    val id: String,
    val sender: Profile,
    var sent: Long?,
    var received: Long?,
    val contentType: ContentType,
    val text: String,
    private var media: ByteArray?
) {
    companion object {
        fun fromSerialized(ser: SerializedMessage): Result<Message, String> {
            val profile = ChatManager.getProfile(ser.sender)
            if (profile.isErr())
                return err("Message.fromSerialized: Unknown profile")
            return ok(
                Message(
                    chatDataId = ser.chatDataId,
                    id = ser.id,
                    sender = profile.unwrap(),
                    sent = ser.sent,
                    received = ser.received,
                    contentType = ser.contentType,
                    text = ser.text,
                    media = null // media is lazy-loaded for applicable message types to save memory,
                )
            )
        }
    }

    fun intoSerializable(): SerializedMessage {
        return SerializedMessage(
            chatDataId = chatDataId,
            id = id,
            sender = sender.dataId,
            sent!!,
            received!!,
            contentType,
            text
        )
    }

    fun media(): ByteArray? {
        if(media != null) return media
        if(contentType == ContentType.LINKED_MEDIA || contentType == ContentType.PICTURE || contentType == ContentType.VOICE) {
            val chatsDir = DataManager.getLocation(Location.CHATS)
            val chatDir = File(chatsDir, chatDataId)
            val content = DataManager.readFile(id, chatDir)
            if(content != null) media = content
            return content
        }
        return null
    }
}