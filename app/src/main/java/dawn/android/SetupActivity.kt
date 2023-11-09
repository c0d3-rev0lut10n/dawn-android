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
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dawn.android.data.Preferences
import dawn.android.data.Theme
import dawn.android.databinding.ActivitySetupBinding
import dawn.android.util.DataManager
import dawn.android.util.PreferenceManager
import dawn.android.util.RequestFactory
import dawn.android.util.ThemeLoader
import java.io.File

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mThemeLoader = ThemeLoader(this)
        val themeSwitch = mThemeLoader.getThemeSetting(this)
        when(themeSwitch) {
            Preferences.THEME_DARK -> {
                setTheme(R.style.Theme_Dawn_Dark)
                theme.applyStyle(R.style.Theme_Dawn_Dark, true)
                mTheme = mThemeLoader.loadDarkTheme()
                androidTheme = R.style.Theme_Dawn_Dark
            }

            Preferences.THEME_EXTRADARK -> {
                setTheme(R.style.Theme_Dawn_ExtraDark)
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

        val dataInitResult = DataManager.initializeStorage(this.applicationContext, password, false)
        if(!dataInitResult) {
            // create a correctly themed dialog notifying about the failed data storage initialization and what to do about it
            val failedDataInitDialogResponse = EditText(this)
            val textColor = MaterialColors.getColor(this, android.R.attr.textColor, ContextCompat.getColor(this, R.color.white))
            failedDataInitDialogResponse.setTextColor(textColor)
            val backgroundColor = MaterialColors.getColor(this, R.attr.textFieldBackgroundColor, ContextCompat.getColor(this, R.color.black))
            failedDataInitDialogResponse.setTextColor(textColor)
            failedDataInitDialogResponse.setBackgroundColor(backgroundColor)
            if(Build.VERSION.SDK_INT >= 29) {
                val cursor = failedDataInitDialogResponse.textCursorDrawable
                cursor?.setTint(textColor)
                failedDataInitDialogResponse.textCursorDrawable = cursor
            }

            val failedDataInitDialog = MaterialAlertDialogBuilder(this, R.style.Theme_Dawn_Dialog)
            failedDataInitDialog.setTitle(R.string.title_failed_data_init_dialog)
            failedDataInitDialog.setMessage(R.string.text_failed_data_init_dialog)
            failedDataInitDialog.setView(failedDataInitDialogResponse)
            failedDataInitDialog.setCancelable(true)
            failedDataInitDialog.setNegativeButton(R.string.cancel, null)
            failedDataInitDialog.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> if(failedDataInitDialogResponse.text.toString() == "OK JUST DELETE ALL THE DATA") tryForcingDataInit(password, serverAddress, profileName, profileBio) }
            failedDataInitDialog.create().show()
            return
        }
        else {
            saveProfileData(password, serverAddress, profileName, profileBio)
        }
    }

    private fun tryForcingDataInit(password: String, serverAddress: String, profileName: String, profileBio: String) {
        Log.w(application.packageName, "FORCING DATA STORAGE INIT")
        DataManager.initializeStorage(this.applicationContext, password, true)
        saveProfileData(password, serverAddress, profileName, profileBio)
    }

    private fun saveProfileData(password: String, serverAddress: String, profileName: String, profileBio: String) {
        DataManager.init(this.applicationContext, password, false)

        val initIdResult = LibraryConnector.mGenId()
        if(initIdResult.isErr()) {
            // TODO: notify about the error
            finish()
            return
        }
        val initId = initIdResult.unwrap().id!!

        val signatureKeypairResult = LibraryConnector.mSignKeygen()
        if(signatureKeypairResult.isErr()) {
            // TODO: notify about the error
            finish()
        }
        val signatureKeypair = signatureKeypairResult.unwrap()

        PreferenceManager.new(filesDir)
        PreferenceManager.set("server", serverAddress)
        PreferenceManager.set("profileName", profileName)
        PreferenceManager.set("profileBio", profileBio)
        PreferenceManager.set("initId", initId)
        PreferenceManager.set("initMdc", LibraryConnector.mMdcGen().unwrap().mdc!!)
        PreferenceManager.set("pubkeySig", signatureKeypair.own_pubkey_sig!!)
        PreferenceManager.set("seckeySig", signatureKeypair.own_seckey_sig!!)

        val chatsDir = File(filesDir, "chats")
        chatsDir.mkdir()
        val receivedInitRequestDir = File(chatsDir, initId)
        receivedInitRequestDir.mkdir()
        PreferenceManager.write().unwrap()

        if(!ReceiveMessagesService.isRunning) {
            val startServiceIntent = Intent(this, ReceiveMessagesService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(startServiceIntent)
            } else {
                startService(startServiceIntent)
            }
        }

        finish()
    }

    override fun onResume() {
        binding.toolbar.title = actionBarText
        super.onResume()
    }
}