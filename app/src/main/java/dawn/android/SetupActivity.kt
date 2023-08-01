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

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import dawn.android.data.Preferences
import dawn.android.data.Theme
import dawn.android.databinding.ActivitySetupBinding
import dawn.android.util.ThemeLoader

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mThemeLoader = ThemeLoader(this)
        val themeSwitch = Preferences.THEME_EXTRADARK
        when(themeSwitch) {
            Preferences.THEME_DARK -> {
                theme.applyStyle(R.style.Theme_Dawn_Dark, true)
                mTheme = mThemeLoader.loadDarkTheme()
                androidTheme = R.style.Theme_Dawn_Dark
            }

            Preferences.THEME_EXTRADARK -> {
                theme.applyStyle(R.style.Theme_Dawn_ExtraDark, true)
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

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val actionBarTextColor = mTheme.primaryTextColor
        val actionBarString = getString(R.string.setup)
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // disable the back button action to prevent the user from exiting setup preliminary
            }
        })

        binding.etPassword.editText?.addTextChangedListener {
            checkPasswordsMatching()
        }
        binding.etPasswordConfirm.editText?.addTextChangedListener {
            checkPasswordsMatching()
        }
        binding.btnFinishSetup.setOnClickListener {
            completeSetup()
        }
    }

    private fun checkPasswordsMatching(): Boolean {
        if(binding.etPassword.editText?.text.toString() != binding.etPasswordConfirm.editText?.text.toString()) {
            binding.etPasswordConfirm.error = getString(R.string.error_passwords_not_matching)
            return false
        }
        else {
            binding.etPasswordConfirm.error = null
            return true
        }
    }

    private fun completeSetup() {
        val password = binding.etPassword.editText?.text.toString()

        if(password == "") {
            val emptyPasswordDialog = AlertDialog.Builder(this, R.style.Theme_Dawn_Dialog)

            emptyPasswordDialog.setTitle(R.string.title_password_dialog)
            emptyPasswordDialog.setMessage(R.string.text_no_password_provided)
            emptyPasswordDialog.setCancelable(false)
            emptyPasswordDialog.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            emptyPasswordDialog.create().show()
            return
        }

        if(!checkPasswordsMatching()) {
            val emptyPasswordDialog = AlertDialog.Builder(this, R.style.Theme_Dawn_Dialog)

            emptyPasswordDialog.setTitle(R.string.title_passwords_not_matching_dialog)
            emptyPasswordDialog.setMessage(R.string.text_passwords_not_matching_dialog)
            emptyPasswordDialog.setCancelable(false)
            emptyPasswordDialog.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            emptyPasswordDialog.create().show()
            return
        }

        val profileName = binding.etProfileName.editText?.text.toString()
        if(profileName == "") {
            val emptyProfileNameDialog = AlertDialog.Builder(this, R.style.Theme_Dawn_Dialog)

            emptyProfileNameDialog.setTitle(R.string.title_no_profile_name_dialog)
            emptyProfileNameDialog.setMessage(R.string.text_no_profile_name_dialog)
            emptyProfileNameDialog.setCancelable(false)
            emptyProfileNameDialog.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            emptyProfileNameDialog.create().show()
            return
        }

        val profileBio = binding.etProfileBio.editText?.text.toString()

        val serverAddressInput = binding.etServerAddress.editText?.text.toString()
        val serverAddress = if(serverAddressInput == "") getString(R.string.default_server_address) else serverAddressInput
    }

    override fun onResume() {
        binding.toolbar.title = actionBarText
        super.onResume()
    }
}