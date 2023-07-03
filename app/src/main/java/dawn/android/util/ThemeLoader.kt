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
        val primaryUIColor = ContextCompat.getColor(context, R.color.dark_secondary)
        val primaryBackgroundColor = ContextCompat.getColor(context, R.color.dark_background)
        val primaryTextColor = ContextCompat.getColor(context, R.color.white)
        val secondaryTextColor = ContextCompat.getColor(context, R.color.white)

        val gradientStartColor = ContextCompat.getColor(context, R.color.dark_secondary)
        val gradientCenterColor = ContextCompat.getColor(context, R.color.dark_gradient_center)
        val gradientEndColor = ContextCompat.getColor(context, R.color.dark_gradient_end)

        val navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_menu_24)
        val backButtonIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_arrow_back_24)

        if(navigationIcon != null) DrawableCompat.setTint(navigationIcon, primaryTextColor)
        if(backButtonIcon != null) DrawableCompat.setTint(backButtonIcon, primaryTextColor)

        return Theme(primaryForegroundColor, primaryUIColor, primaryBackgroundColor, primaryTextColor, secondaryTextColor, gradientStartColor, gradientCenterColor, gradientEndColor, navigationIcon, backButtonIcon)
    }

    fun loadExtraDarkTheme(): Theme {
        val primaryForegroundColor = ContextCompat.getColor(context, R.color.x_dark_primary)
        val primaryUIColor = ContextCompat.getColor(context, R.color.x_dark_secondary)
        val primaryBackgroundColor = ContextCompat.getColor(context, R.color.black)
        val primaryTextColor = ContextCompat.getColor(context, R.color.x_dark_primary)
        val secondaryTextColor = ContextCompat.getColor(context, R.color.black)

        val gradientStartColor = ContextCompat.getColor(context, R.color.x_dark_secondary)
        val gradientCenterColor = ContextCompat.getColor(context, R.color.x_dark_gradient_center)
        val gradientEndColor = ContextCompat.getColor(context, R.color.x_dark_gradient_end)

        val navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_menu_24)
        val backButtonIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_arrow_back_24)

        if(navigationIcon != null) DrawableCompat.setTint(navigationIcon, secondaryTextColor)
        if(backButtonIcon != null) DrawableCompat.setTint(backButtonIcon, secondaryTextColor)

        return Theme(primaryForegroundColor, primaryUIColor, primaryBackgroundColor, primaryTextColor, secondaryTextColor, gradientStartColor, gradientCenterColor, gradientEndColor, navigationIcon, backButtonIcon)
    }

}