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

package dawn.android.data

import dawn.android.data.serialized.SerializedChat
import dawn.android.data.serialized.SerializedMessage
import dawn.android.ui.data.ChatPreviewData
import dawn.android.util.TimestampUtil.toTimestampForChatPreview

class Chat(
    var dataId: String,
    var id: String,
    var idStamp: String,
    var idSalt: String,
    var lastMessageId: UShort,
    var name: String,
    val messages: ArrayList<Message>,
    var type: ChatType,
    var ownKyber: Keypair,
    var ownCurve: Keypair,
    var ownPFS: String,
    var remotePFS: String,
    var pfsSalt: String,
    var mdcSeed: String,
    var associatedProfileId: String?
) {
    companion object {

        fun fromSerialized(ser: SerializedChat): Chat {
            val messages = ArrayList<Message>()
            for(serMsg in ser.messages) {
                val message = Message.fromSerialized(serMsg)
                if(message.isOk())
                    messages.add(message.unwrap())
            }
            return Chat(
                dataId = ser.dataId,
                id = ser.id,
                idStamp = ser.idStamp,
                idSalt = ser.idSalt,
                lastMessageId = ser.lastMessageId,
                name = ser.name,
                type = ser.type,
                messages = messages,
                ownKyber = ser.ownKyber,
                ownCurve = ser.ownCurve,
                ownPFS = ser.ownPFS,
                remotePFS = ser.remotePFS,
                pfsSalt = ser.pfsSalt,
                mdcSeed = ser.mdcSeed,
                associatedProfileId = ser.associatedProfileId
            )
        }
    }

    fun intoSerializable(): SerializedChat {
        val serializedMessages = ArrayList<SerializedMessage>()
        for(msg in this.messages) {
            val serializedMessage = msg.intoSerializable()
            serializedMessages.add(serializedMessage)
        }
        return SerializedChat(
            dataId = dataId,
            id = id,
            idStamp = idStamp,
            idSalt = idSalt,
            lastMessageId = lastMessageId,
            name = name,
            type = type,
            messages = serializedMessages,
            ownKyber = ownKyber,
            ownCurve = ownCurve,
            ownPFS = ownPFS,
            remotePFS = remotePFS,
            pfsSalt = pfsSalt,
            mdcSeed = mdcSeed,
            associatedProfileId = associatedProfileId
        )
    }

    fun toPreview(): ChatPreviewData {
        val messageForPreview: Message
        val userName: String?
        val messagePreview: String
        try {
            messageForPreview = messages.last()
            userName = messageForPreview.sender.name
            messagePreview = if(messageForPreview.text.length > 42)
                messageForPreview.text.slice(IntRange(0,42))
            else messageForPreview.text

        }
        catch(e: Exception) {
            return ChatPreviewData(
                "UNKNOWN", null, "COULD NOT LOAD CHAT", "", false, false, dataId
            )
        }
        val time: String = if(messageForPreview.received != null)
           messageForPreview.received!!.toTimestampForChatPreview()
        else if(messageForPreview.sent != null)
            messageForPreview.sent!!.toTimestampForChatPreview()
        else
            ""
        return ChatPreviewData(
            chatName = name,
            userName = userName,
            messagePreview = messagePreview,
            time = time,
            isSent = messageForPreview.sent != null,
            isRead = messageForPreview.received != null,
            dataId = dataId
        )
    }
}