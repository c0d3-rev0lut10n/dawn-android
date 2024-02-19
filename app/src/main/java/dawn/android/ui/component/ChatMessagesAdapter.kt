/*
 * Copyright (c) 2024  Laurenz Werner
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


package dawn.android.ui.component

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.TextView
import dawn.android.R
import dawn.android.data.ContentType
import dawn.android.data.Message

class ChatMessagesAdapter(
    private val context: Context,
    private val resource: Int,
    objects: ArrayList<Message>
): ArrayAdapter<Message>(context, resource, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var mConvertView = convertView
        val layoutInflater = LayoutInflater.from(context)
        if(mConvertView == null) {
            mConvertView = layoutInflater.inflate(resource, parent, false)
        }

        val layout = mConvertView!!.findViewById<RelativeLayout>(R.id.messageView)

        val item = getItem(position)?: return mConvertView
        when(item.contentType) {
            ContentType.TEXT -> {
                val contentView = TextView(context)
                contentView.text = item.text
                layout.addView(contentView)
            }
            ContentType.SENT_INIT -> {
                val contentView = TextView(context)
                contentView.text = item.text
                layout.addView(contentView)
            }
            else -> {
                val contentView = TextView(context)
            }
        }

        return mConvertView
    }
}