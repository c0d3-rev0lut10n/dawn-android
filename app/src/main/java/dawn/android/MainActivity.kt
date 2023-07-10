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

import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {

        mLibraryConnector = LibraryConnector

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // load theme
        val themeId = Preferences.THEME_DARK
        val mThemeLoader = ThemeLoader(this)
        when(themeId) {
            Preferences.THEME_DARK -> {
                mTheme = mThemeLoader.loadDarkTheme()
            }
            Preferences.THEME_EXTRADARK -> {
                mTheme = mThemeLoader.loadExtraDarkTheme()

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

        if(!mDataManager.isInitialized()) {
            askForPassword()
        }
    }

    override fun onResume() {
        if(binding.drawerLayout.isDrawerOpen(binding.navView))
            binding.drawerLayout.closeDrawer(binding.navView)

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

    private fun navigate(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        return true
    }

    private fun askForPassword() {
        val passwordField = EditText(this)
        passwordField.inputType = InputType.TYPE_CLASS_TEXT
        passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
        val passwordDialog = AlertDialog.Builder(this)

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
        mDataManager.init(this, password)
    }
}