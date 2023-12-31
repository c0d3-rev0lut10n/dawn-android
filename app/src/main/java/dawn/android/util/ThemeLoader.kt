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
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import dawn.android.R
import dawn.android.data.Preferences
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

        val itemStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val itemColors = intArrayOf(
            primaryTextColor,
            secondaryTextColor
        )
        val itemColorStateList = ColorStateList(itemStates, itemColors)

        val navHighlightStateListDrawable = StateListDrawable()
        navHighlightStateListDrawable.addState(itemStates[0], ColorDrawable(ColorUtils.setAlphaComponent(primaryUIColor, 100)))

        return Theme(primaryForegroundColor, primaryUIColor, primaryBackgroundColor, primaryTextColor, secondaryTextColor, gradientStartColor, gradientCenterColor, gradientEndColor, navigationIcon, backButtonIcon, itemColorStateList, navHighlightStateListDrawable)
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

        val itemStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val itemColors = intArrayOf(
            primaryTextColor,
            secondaryTextColor
        )
        val itemColorStateList = ColorStateList(itemStates, itemColors)

        val navHighlightStateListDrawable = StateListDrawable()
        navHighlightStateListDrawable.addState(itemStates[0], ColorDrawable(ColorUtils.setAlphaComponent(primaryUIColor, 100)))

        return Theme(primaryForegroundColor, primaryUIColor, primaryBackgroundColor, primaryTextColor, secondaryTextColor, gradientStartColor, gradientCenterColor, gradientEndColor, navigationIcon, backButtonIcon, itemColorStateList, navHighlightStateListDrawable)
    }

    fun getThemeSetting(context: Context): Int {
        val androidSettings = getDefaultSharedPreferences(context)
        // find out if the user wants the theme to change depending on system night mode
        val themeUseSystem = androidSettings.getBoolean("theme_use_system", Preferences.THEME_MANUAL)
        if(themeUseSystem) {
            // change mode depending on system night mode
            val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if(currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                return androidSettings.getInt("theme_system_dark", Preferences.THEME_DARK)
            }
            else if(currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                // TODO: change default to light theme once the theme is complete
                return androidSettings.getInt("theme_system_light", Preferences.THEME_DARK)
            }
        }
        return androidSettings.getInt("default_theme", Preferences.THEME_DARK)
    }

    fun getThemeMode(context: Context): Boolean {
        val androidSettings = getDefaultSharedPreferences(context)
        // find out if the user wants the theme to change depending on system night mode
        return androidSettings.getBoolean("theme_use_system", Preferences.THEME_MANUAL)
    }

    fun getThemeManualSetting(context: Context): Int {
        val androidSettings = getDefaultSharedPreferences(context)
        return androidSettings.getInt("default_theme", Preferences.THEME_DARK)
    }

    fun getThemeDarkSetting(context: Context): Int {
        val androidSettings = getDefaultSharedPreferences(context)
        return androidSettings.getInt("theme_system_dark", Preferences.THEME_DARK)
    }

    fun getThemeLightSetting(context: Context): Int {
        val androidSettings = getDefaultSharedPreferences(context)
        // TODO: change default to light theme once the theme is complete
        return androidSettings.getInt("theme_system_light", Preferences.THEME_DARK)
    }

    fun setThemeMode(context: Context, themeMode: Boolean) {
        val androidSettings = getDefaultSharedPreferences(context).edit()
        // find out if the user wants the theme to change depending on system night mode
        androidSettings.putBoolean("theme_use_system", themeMode)
        androidSettings.apply()
    }

    fun setThemeManualSetting(context: Context, themeManualSetting: Int) {
        val androidSettings = getDefaultSharedPreferences(context).edit()
        androidSettings.putInt("default_theme", themeManualSetting)
        androidSettings.apply()
    }

    fun setThemeDarkSetting(context: Context, themeDarkSetting: Int) {
        val androidSettings = getDefaultSharedPreferences(context).edit()
        androidSettings.putInt("theme_system_dark", themeDarkSetting)
        androidSettings.apply()
    }

    fun setThemeLightSetting(context: Context, themeLightSetting: Int) {
        val androidSettings = getDefaultSharedPreferences(context).edit()
        androidSettings.putInt("theme_system_light", themeLightSetting)
        androidSettings.apply()
    }

}