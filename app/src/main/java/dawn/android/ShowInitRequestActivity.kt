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

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dawn.android.data.Chat
import dawn.android.data.ChatType
import dawn.android.data.ContentType
import dawn.android.data.Default
import dawn.android.data.Keypair
import dawn.android.data.Location
import dawn.android.data.Message
import dawn.android.data.Preferences
import dawn.android.data.Profile
import dawn.android.data.ReceivedInitRequest
import dawn.android.data.Theme
import dawn.android.databinding.ActivityShowInitRequestBinding
import dawn.android.util.ChatManager
import dawn.android.util.DataManager
import dawn.android.util.PreferenceManager
import dawn.android.util.ThemeLoader
import kotlinx.serialization.json.Json
import java.io.File

class ShowInitRequestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowInitRequestBinding
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString
    private lateinit var logTag: String
    private lateinit var mThemeLoader: ThemeLoader
    private lateinit var dataId: String
    private lateinit var request: ReceivedInitRequest

    private lateinit var mService: ReceiveMessagesService
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ReceiveMessagesService.BindInterface
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mBound = false
        }
    }

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
        dataId = extras?.getString("dataId")?: ""
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
        try {
            request =
                Json.decodeFromString<ReceivedInitRequest>(String(fileContent, Charsets.UTF_8))
        }
        catch (e: Exception) {
            Log.e(logTag, "failed to deserialize init request: ${e.printStackTrace()}")
            finish()
            return
        }

        bindService(
            Intent(this, ReceiveMessagesService::class.java),
            connection,
            BIND_AUTO_CREATE
        )

        binding = ActivityShowInitRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        window.statusBarColor = mTheme.primaryUIColor
        window.navigationBarColor = mTheme.primaryBackgroundColor

        val actionBarTextColor = mTheme.secondaryTextColor
        val actionBarString = request.name
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.backButtonIcon)

        binding.tvInitRequestMessage.text = request.comment
        binding.btnAcceptRequest.setOnClickListener { accept() }
        binding.btnRejectRequest.setOnClickListener { reject() }
    }

    private fun accept() {

    }

    private fun reject() {
        val initRequestDir = DataManager.getLocation(Location.RECEIVED_REQUESTS)
        val file = File(initRequestDir, dataId)
        file.delete()
        finish()
    }

    override fun onResume() {
        binding.toolbar.title = actionBarText
        super.onResume()
    }
}