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

package dawn.android

import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup.LayoutParams
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dawn.android.data.Preferences
import dawn.android.data.Theme
import dawn.android.databinding.ActivityMainBinding
import dawn.android.util.DataManager
import dawn.android.util.ThemeLoader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mLibraryConnector: LibraryConnector
    private lateinit var mDataManager: DataManager
    private lateinit var mTheme: Theme
    private lateinit var actionBarText: SpannableString
    private val sizeFactor = 3 // this will be configurable
    private lateinit var chatPreviewLayoutParams: ConstraintLayout.LayoutParams

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

        mLibraryConnector = LibraryConnector

        super.onCreate(savedInstanceState)

        // load theme
        val mThemeLoader = ThemeLoader(this)
        val themeId = mThemeLoader.getThemeSetting(this)
        when(themeId) {
            Preferences.THEME_DARK -> {
                mTheme = mThemeLoader.loadDarkTheme()
                setTheme(R.style.Theme_Dawn_Dark)
            }
            Preferences.THEME_EXTRADARK -> {
                mTheme = mThemeLoader.loadExtraDarkTheme()
                setTheme(R.style.Theme_Dawn_ExtraDark)

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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        window.statusBarColor = mTheme.primaryUIColor
        window.navigationBarColor = mTheme.primaryBackgroundColor
        binding.drawerLayout.setBackgroundColor(mTheme.primaryBackgroundColor)

        val actionBar = binding.appBarMain.toolbar
        val actionBarTextColor = mTheme.secondaryTextColor
        val actionBarString = getString(R.string.app_name)
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        //actionBar.setBackgroundDrawable(ColorDrawable(actionBarTextColor))

        actionBar.setBackgroundColor(mTheme.primaryUIColor)

        binding.navView.setNavigationItemSelectedListener { navigate(it) }

        val navHeader = binding.navView.getHeaderView(0)

        val gradientColors = intArrayOf(
            mTheme.gradientStartColor,
            mTheme.gradientCenterColor,
            mTheme.gradientEndColor
        )

        val navHeaderBackground = GradientDrawable(GradientDrawable.Orientation.TL_BR, gradientColors)
        navHeader.background = navHeaderBackground

        binding.navView.itemTextColor = mTheme.navItemColorStateList
        binding.navView.itemIconTintList = mTheme.navItemColorStateList
        binding.navView.itemBackground = mTheme.navHighlightStateListDrawable

        val navHeaderTitle = navHeader.findViewById<TextView>(R.id.nav_header_title)
        navHeaderTitle.setTextColor(mTheme.primaryTextColor)

        mDataManager = DataManager

        if(!mDataManager.isStorageInitialized(this.applicationContext)) {
            // start setup if necessary
            val setupIntent = Intent(this, SetupActivity::class.java)
            startActivity(setupIntent)
        }

        else {

            if (!mDataManager.isInitialized()) {
                askForPassword()
            }
            else connectReceiveMessagesService()
        }

        chatPreviewLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, sizeFactor * 32)
        chatPreviewLayoutParams.setMargins(30, 30, 30, 0)

        makeChatlist()
    }

    override fun onResume() {
        if(binding.drawerLayout.isDrawerOpen(binding.navView))
            binding.drawerLayout.closeDrawer(binding.navView, false)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.navigationIcon)
        binding.navView.setCheckedItem(R.id.nav_home)

        binding.appBarMain.toolbar.title = actionBarText
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean {
        if(binding.drawerLayout.isDrawerOpen(binding.navView))
            binding.drawerLayout.closeDrawer(binding.navView)
        else
            binding.drawerLayout.openDrawer(binding.navView)

        return super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        try {
            unbindService(connection)
        }
        catch (e: Exception) {
            Log.w(packageName, "Service was not bound on onDestroy()", e)
        }
        super.onDestroy()
    }

    private fun navigate(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_new_chat -> {
                val intent = Intent(this, InitiateChatActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    private fun askForPassword() {
        val passwordField = EditText(this)
        passwordField.inputType = InputType.TYPE_CLASS_TEXT
        passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
        val textColorTypedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColor, textColorTypedValue, true)
        @ColorInt val textColor = textColorTypedValue.data
        passwordField.setTextColor(textColor)
        val backgroundColorTypedValue = TypedValue()
        theme.resolveAttribute(R.attr.textFieldBackgroundColor, backgroundColorTypedValue, true)
        @ColorInt val backgroundColor = backgroundColorTypedValue.data
        passwordField.setTextColor(textColor)
        passwordField.setBackgroundColor(backgroundColor)
        if(Build.VERSION.SDK_INT >= 29) {
            val cursor = passwordField.textCursorDrawable
            cursor?.setTint(textColor)
            passwordField.textCursorDrawable = cursor
        }

        val passwordDialog = MaterialAlertDialogBuilder(this, R.style.Theme_Dawn_Dialog)

        passwordDialog.setMessage(R.string.text_enter_password)
        passwordDialog.setTitle(R.string.title_password_dialog)
        passwordDialog.setCancelable(false)
        passwordDialog.setView(passwordField)
        passwordDialog.setPositiveButton(R.string.ok) {_: DialogInterface, _: Int -> validatePasswordInput(passwordField.text.toString())}
        passwordDialog.setNegativeButton(R.string.cancel) {_: DialogInterface, _: Int -> finish() }
        passwordDialog.create().show()
    }

    private fun validatePasswordInput(password: String) {
        if(password == "") {
            val emptyPasswordDialog = AlertDialog.Builder(this)

            emptyPasswordDialog.setTitle(R.string.title_password_dialog)
            emptyPasswordDialog.setMessage(R.string.text_no_password_provided)
            emptyPasswordDialog.setCancelable(false)
            emptyPasswordDialog.setPositiveButton(R.string.ok) {_: DialogInterface, _: Int -> askForPassword()}
            emptyPasswordDialog.create().show()
        }
        else {
            unlockDataManager(password)
        }
    }

    private fun unlockDataManager(password: String) {
        if(!mDataManager.init(this, password)) {
            val wrongPasswordDialog = MaterialAlertDialogBuilder(this)

            wrongPasswordDialog.setTitle(R.string.title_password_dialog)
            wrongPasswordDialog.setMessage(R.string.text_wrong_password)
            wrongPasswordDialog.setCancelable(false)
            wrongPasswordDialog.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> askForPassword()}
            wrongPasswordDialog.create().show()
        }
        else connectReceiveMessagesService()
    }

    private fun connectReceiveMessagesService() {
        if(!ReceiveMessagesService.isRunning) {
            val startServiceIntent = Intent(this, ReceiveMessagesService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(startServiceIntent)
            } else {
                startService(startServiceIntent)
            }
        }
        bindService(
            Intent(this, ReceiveMessagesService::class.java),
            connection,
            BIND_AUTO_CREATE
        )
    }

    private fun makeChatlist() {
        binding.appBarMain.content.contentLayout.removeAllViews()
        val chats = DataManager.getAllChats()
        if(chats.isEmpty()) {
            // no chats yet, show a notice about that instead
            val noChatsYetTextView = TextView(this)
            val layoutParams = chatPreviewLayoutParams
            layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            noChatsYetTextView.layoutParams = layoutParams
            noChatsYetTextView.text = getString(R.string.main_text_no_chats_yet)
            noChatsYetTextView.textSize = (sizeFactor * 6.5).toFloat()
            noChatsYetTextView.gravity = Gravity.CENTER
            binding.appBarMain.content.contentLayout.addView(noChatsYetTextView)
        }
    }
}