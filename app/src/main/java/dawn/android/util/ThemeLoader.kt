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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import dawn.android.R
import dawn.android.data.Theme

class ThemeLoader(private val context: Context) {

    fun loadDarkTheme(): Theme {

        val primaryForegroundColor = ContextCompat.getColor(context, R.color.dark_primary)
        val primaryBackgroundColor = ContextCompat.getColor(context, R.color.dark_secondary)
        val primaryTextColor = ContextCompat.getColor(context, R.color.white)
        val navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_menu_24)

        if(navigationIcon == null) {
            return Theme(primaryForegroundColor, primaryBackgroundColor, primaryTextColor, null)
        }

        DrawableCompat.setTint(navigationIcon, primaryTextColor)

        return Theme(primaryForegroundColor, primaryBackgroundColor, primaryTextColor, navigationIcon)
    }

}