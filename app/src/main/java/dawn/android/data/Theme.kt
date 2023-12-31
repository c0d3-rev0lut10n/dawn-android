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

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable

data class Theme(
    val primaryForegroundColor: Int,
    val primaryUIColor: Int,
    val primaryBackgroundColor: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int,

    val gradientStartColor: Int,
    val gradientCenterColor: Int,
    val gradientEndColor: Int,

    val navigationIcon: Drawable?,
    val backButtonIcon: Drawable?,

    val navItemColorStateList: ColorStateList,
    val navHighlightStateListDrawable: Drawable
)
