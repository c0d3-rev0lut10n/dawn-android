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

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import dawn.android.R

class ChatListItem(context: Context, attrs: AttributeSet): RelativeLayout(context, attrs) {
    init {
        val attrArray = context.obtainStyledAttributes(attrs, R.styleable.ChatListItem)
        try {
            for (attrNumber in 0..attrArray.indexCount) {
                // TODO
            }
        }
        finally {
            attrArray.recycle()
        }
    }
}