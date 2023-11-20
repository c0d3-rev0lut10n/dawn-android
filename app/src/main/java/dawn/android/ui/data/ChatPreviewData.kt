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

package dawn.android.ui.data

class ChatPreviewData(
    private var chatName: String,
    private var userName: String?,
    private var messagePreview: String,
    private var time: String,
    private var isSent: Boolean,
    private var isRead: Boolean
) {
    fun getChatName(): String {
        return chatName
    }

    fun setChatName(name: String) {
        chatName = name
    }

    fun getUserName(): String {
        return if(userName == null) chatName
        else userName!!
    }

    fun setUserName(name: String) {
        userName = name
    }

    fun getMessagePreview(): String {
        return messagePreview
    }

    fun setMessagePreview(preview: String) {
        messagePreview = preview
    }

    fun getTime(): String {
        return time
    }

    fun setTime(newTime: String) {
        time = newTime
    }

    fun isSent(): Boolean {
        return isSent
    }

    fun setIsSent(sent: Boolean) {
        isSent = sent
    }

    fun isRead(): Boolean {
        return isRead
    }

    fun setIsRead(read: Boolean) {
        isRead = read
    }
}