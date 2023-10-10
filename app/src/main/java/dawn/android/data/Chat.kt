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

import dawn.android.data.Result.Companion.ok
import dawn.android.data.Result.Companion.err

class Chat(
    val dataId: String,
    var id: String,
    var idStamp: String,
    var idSalt: String,
    var lastMessageId: UShort,
    var name: String
) {
    companion object {
        fun new(id: String, idStamp: String, idSalt: String, name: String): Result<Chat, String> {
            // validate input here
            if(id.matches(Regex.ID)) return err("invalid ID")
            if(idStamp.matches(Regex.timestamp)) return err("invalid ID stamp")
            if(idSalt.matches(Regex.IdSalt)) return err("invalid ID salt")
            if(name.contains("\n", true) || name.isEmpty()) return err("invalid name")
            return err("not implemented")
        }

        fun load(dataId: String): Result<Chat, String> {
            return err("not implemented")
        }
    }
    fun save(): Result<Any?, String> {

        return err("not implemented")
    }
}