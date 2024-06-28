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
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dawn.android.data.Location
import dawn.android.data.Preferences
import dawn.android.data.ReceivedInitRequest
import dawn.android.data.Theme
import dawn.android.databinding.ActivityInitRequestsBinding
import dawn.android.ui.component.AdapterScope
import dawn.android.ui.component.ChatPreviewAdapter
import dawn.android.ui.data.ChatPreviewData
import dawn.android.util.DataManager
import dawn.android.util.ThemeLoader
import dawn.android.util.TimestampUtil.toTimestampForChatPreview
import kotlinx.serialization.json.Json

class InitRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInitRequestsBinding
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

        binding = ActivityInitRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        window.statusBarColor = mTheme.primaryUIColor
        window.navigationBarColor = mTheme.primaryBackgroundColor

        val actionBarTextColor = mTheme.secondaryTextColor
        val actionBarString = getString(R.string.initrequests_app_bar)
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.backButtonIcon)
    }

    private fun getInitRequests(): ArrayList<ChatPreviewData> {
        val list = ArrayList<ChatPreviewData>()
        val initRequestDirectory = DataManager.getLocation(Location.RECEIVED_REQUESTS)
        val files = initRequestDirectory.listFiles()
        if(files.isNullOrEmpty()) return list
        for(file in files) {
            val content = DataManager.readFile(file.name, initRequestDirectory)?: continue
            val request = Json.decodeFromString<ReceivedInitRequest>(String(content, Charsets.UTF_8))
            val time = if(request.sent == 0L) "" else request.sent.toTimestampForChatPreview()
            val preview = ChatPreviewData(
                chatName = request.name,
                userName = null,
                messagePreview = request.comment,
                time = time,
                isSent = true,
                isRead = false,
                isOwn = false,
                dataId = request.id
            )
            list.add(preview)
        }
        binding.contentLayout.adapter = ChatPreviewAdapter(this, R.layout.chat_list_item, list, AdapterScope.INIT_REQUEST_LIST)
        return list
    }

    override fun onResume() {
        binding.contentLayout.adapter = null
        getInitRequests()
        binding.toolbar.title = actionBarText
        super.onResume()
    }
}