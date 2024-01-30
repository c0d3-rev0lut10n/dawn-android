/*
 * Copyright (c) 2023-2024 Laurenz Werner
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
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dawn.android.data.Preferences
import dawn.android.data.Theme
import dawn.android.databinding.ActivityInitiateChatBinding
import dawn.android.util.ThemeLoader

class InitiateChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInitiateChatBinding
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString
    private lateinit var logTag: String
    private lateinit var mThemeLoader: ThemeLoader

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

        bindService(
            Intent(this, ReceiveMessagesService::class.java),
            connection,
            BIND_AUTO_CREATE
        )

        binding = ActivityInitiateChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        window.statusBarColor = mTheme.primaryUIColor
        window.navigationBarColor = mTheme.primaryBackgroundColor

        val actionBarTextColor = mTheme.secondaryTextColor
        val actionBarString = getString(R.string.initiate_app_bar)
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.backButtonIcon)

        binding.btnSearchHandle.setOnClickListener {
            searchHandleAndInit()
        }

        val extras = intent.extras
        if(extras != null) {
            binding.etHandleName.setText(extras.getString("handle"))
            binding.etHandleSecret.setText(extras.getString("initSecret"))
        }
    }

    override fun onResume() {
        binding.toolbar.title = actionBarText
        super.onResume()
    }

    private fun searchHandleAndInit() {
        if(!ReceiveMessagesService.isReady) {
            val toast = Toast.makeText(this, getString(R.string.initiate_not_ready), Toast.LENGTH_LONG)
            toast.show()
        }
        val handleToSearch = binding.etHandleName.text.toString()
        val initSecret = binding.etHandleSecret.text.toString()
        val comment = binding.etInitComment.text.toString()
        val initResult = mService.searchHandleAndInit(handleToSearch, initSecret, comment)
        if(initResult.isOk()) {
            val toast = Toast.makeText(this, getString(R.string.initiate_init_request_sent), Toast.LENGTH_LONG)
            toast.show()
            finish()
        }
        else {
            Log.e(logTag, "Error during init: ${initResult.unwrapErr()}")
            val toast = Toast.makeText(this, getString(R.string.initiate_error) + initResult.unwrapErr(), Toast.LENGTH_LONG)
            toast.show()
        }
    }
}