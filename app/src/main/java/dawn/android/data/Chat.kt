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

import android.content.Context
import dawn.android.GenId
import dawn.android.LibraryConnector
import dawn.android.data.Result.Companion.ok
import dawn.android.data.Result.Companion.err
import java.io.File

class Chat(
    val dataId: String,
    var id: String,
    var idStamp: String,
    var idSalt: String,
    var lastMessageId: UShort,
    var name: String,
    val filesDir: File
) {
    companion object {
        fun new(id: String, idStamp: String, idSalt: String, name: String, context: Context): Result<Chat, String> {
            // validate input
            if(id.matches(Regex.ID)) return err("invalid ID")
            if(idStamp.matches(Regex.timestamp)) return err("invalid ID stamp")
            if(idSalt.matches(Regex.IdSalt)) return err("invalid ID salt")
            if(name.contains("\n", true) || name.isEmpty()) return err("invalid name")
            val filesDir = context.filesDir
            var dataId: GenId? = null // we have to initialize with null because the compiler will complain otherwise (even though dataId will be always initialized when the chatDir File gets constructed
            val chatsDir = File(filesDir, "chats")
            if(!chatsDir.isDirectory) {
                // this is the first chat, create the directory
                chatsDir.mkdir()
            }
            val chatDirs = chatsDir.listFiles()
            if(chatDirs == null) {
                // there are no chats, we can freely choose an ID
                dataId = LibraryConnector.mGenId()
                if(dataId.id == null) return err("could not generate data ID")
            }
            else {
                val chatDirNames = ArrayList<String>()
                for(chat in chatDirs) {
                    chatDirNames.add(chat.name)
                }
                for (i in 1..1000) {
                    // choose a random ID that is not used
                    dataId = LibraryConnector.mGenId()
                    if (dataId.id == null) return err("could not generate data ID")
                    if (dataId.id!! !in chatDirNames) break
                    if(i == 1000) return err("could not generate data ID")
                }
            }
            return ok(Chat(dataId!!.id!!, id, idStamp, idSalt, 0U, name, filesDir))
        }

        fun load(dataId: String): Result<Chat, String> {
            return err("not implemented")
        }
    }
    fun save(): Result<Any?, String> {

        return err("not implemented")
    }
}