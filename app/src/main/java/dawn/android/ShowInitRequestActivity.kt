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

package dawn.android

import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dawn.android.data.Location
import dawn.android.data.Preferences
import dawn.android.data.ReceivedInitRequest
import dawn.android.data.Theme
import dawn.android.databinding.ActivityShowChatBinding
import dawn.android.util.DataManager
import dawn.android.util.ThemeLoader
import kotlinx.serialization.json.Json

class ShowInitRequestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowChatBinding
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString
    private lateinit var logTag: String
    private lateinit var mThemeLoader: ThemeLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mThemeLoader = ThemeLoader(this)
        val themeSwitch = mThemeLoader.getThemeSetting(this)
        when(themeSwitch) {
            Preferences.THEME_DARK -> {
                setTheme(R.style.Theme_Dawn_Dark)
                mTheme = mThemeLoader.loadDarkTheme()
                androidTheme = R.style.Theme_Dawn_Dark
            }

            Preferences.THEME_EXTRADARK -> {
                setTheme(R.style.Theme_Dawn_ExtraDark)
                mTheme = mThemeLoader.loadExtraDarkTheme()
                androidTheme = R.style.Theme_Dawn_ExtraDark
                // hide status bar and navigation bar
                if(Build.VERSION.SDK_INT < 30) {
                    // effect may not work on even older API levels
                    WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                    WindowInsetsControllerCompat(window, window.decorView).systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                else {
                    window.decorView.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    window.decorView.windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }

        logTag = this.javaClass.name

        val extras = intent.extras
        val dataId = extras?.getString("dataId")?: ""
        if(dataId == "") {
            Log.e(logTag, "failed to get dataId for init request from intent extras")
            finish()
            return
        }

        val initRequestDir = DataManager.getLocation(Location.RECEIVED_REQUESTS)
        val fileContent = DataManager.readFile(dataId, initRequestDir)
        if (fileContent == null) {
            finish()
            return
        }
        val request: ReceivedInitRequest
        try {
            request =
                Json.decodeFromString<ReceivedInitRequest>(String(fileContent, Charsets.UTF_8))
        }
        catch (e: Exception) {
            Log.e(logTag, "failed to deserialize init request: ${e.printStackTrace()}")
        }
    }
}