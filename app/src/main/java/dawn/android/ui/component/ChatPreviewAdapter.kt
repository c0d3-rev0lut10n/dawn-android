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

package dawn.android.ui.component

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import dawn.android.R
import dawn.android.ui.data.ChatPreviewData


class ChatPreviewAdapter(
    private val context: Context,
    private val resource: Int,
    objects: ArrayList<ChatPreviewData?>): ArrayAdapter<ChatPreviewData?>(context, resource, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var mConvertView = convertView
        val layoutInflater = LayoutInflater.from(context)
        if(mConvertView == null) {
            mConvertView = layoutInflater.inflate(resource, parent, false)
        }

        val tvChatName = mConvertView!!.findViewById<TextView>(R.id.chatName)
        val tvChatPreview = mConvertView.findViewById<TextView>(R.id.chatPreview)
        val tvTime = mConvertView.findViewById<TextView>(R.id.time)
        val ivProfilePicture = mConvertView.findViewById<ImageView>(R.id.profilePicture)
        val ivSentReceived = mConvertView.findViewById<ImageView>(R.id.sentReceived)

        val item = getItem(position) ?: return mConvertView

        tvChatName.text = item.getChatName()
        tvChatPreview.text = item.getMessagePreview()
        tvTime.text = item.getTime()
        if(item.isRead())
            ivSentReceived.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_message_received))
        else if(item.isSent())
            ivSentReceived.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_message_sent))

        return mConvertView
    }
}