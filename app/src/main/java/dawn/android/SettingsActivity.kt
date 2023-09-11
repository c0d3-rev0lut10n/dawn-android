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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dawn.android.data.Preferences
import dawn.android.data.Theme
import dawn.android.databinding.ActivitySettingsBinding
import dawn.android.util.DataManager
import dawn.android.util.RequestFactory
import dawn.android.util.ThemeLoader
import java.security.SecureRandom

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString
    private lateinit var currentProfileName: String
    private lateinit var currentProfileBio: String
    private lateinit var currentProfileHandle: String
    private lateinit var currentHandlePassword: String
    private var currentHandleAllowPublicInit = false
    private lateinit var currentManualThemeName: String
    private lateinit var currentSystemDarkThemeName: String
    private lateinit var currentSystemLightThemeName: String
    private lateinit var initSecret: String
    private var profileNameChanges = false
    private var profileBioChanges = false
    private var profileHandleChanges = false
    private var themeModeChanges = false
    private var manualThemeChanges = false
    private var systemDarkThemeChanges = false
    private var systemLightThemeChanges = false
    private var themeUseSystem = false
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

        logTag = "$packageName.SettingsActivity"

        bindService(
            Intent(this, ReceiveMessagesService::class.java),
            connection,
            BIND_AUTO_CREATE
        )

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        window.statusBarColor = mTheme.primaryUIColor
        window.navigationBarColor = mTheme.primaryBackgroundColor

        val actionBarTextColor = mTheme.secondaryTextColor
        val actionBarString = getString(R.string.settings)
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.backButtonIcon)

        val paddedProfileName = String(DataManager.readFile("profileName", filesDir)!!, Charsets.UTF_8)
        currentProfileName = paddedProfileName.substringAfter("\n").substringBefore("\n")

        val paddedProfileBio = String(DataManager.readFile("profileBio", filesDir)!!, Charsets.UTF_8)
        currentProfileBio = paddedProfileBio.substringAfter("\n").substringBefore("\n")

        val handleFileContent = DataManager.readFile("profileHandle", filesDir)
        currentProfileHandle = if(handleFileContent != null) {
            val paddedProfileHandle = String(handleFileContent, Charsets.UTF_8)
            paddedProfileHandle.substringAfter("\n").substringBefore("\n")
        } else ""

        val handlePasswordFileContent = DataManager.readFile("profileHandlePassword", filesDir)
        currentHandlePassword = if(handlePasswordFileContent != null)
            String(handlePasswordFileContent, Charsets.UTF_8).substringAfter("\n").substringBefore("\n")
        else
            ""

        val handlePublicInitFileContent = DataManager.readFile("profileHandlePublicInit", filesDir)
        if(handlePublicInitFileContent != null) {
            when (String(handlePublicInitFileContent, Charsets.UTF_8)) {
                "true" -> currentHandleAllowPublicInit = true
                "false" -> currentHandleAllowPublicInit = false
                else -> {
                    Log.w(logTag, "Could not parse profileHandlePublicInit")
                }
            }
        }

        val initSecretFileContent = DataManager.readFile("initSecret", filesDir)
        if(initSecretFileContent == null) {
            // there doesn't exist an init secret yet, therefore create one
            val availableSecretCharacters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val secretLength = 16
            val secret = CharArray(secretLength)
            val stringGenerationRng = SecureRandom()
            for(i in 0 until secretLength) {
                secret[i] = availableSecretCharacters[stringGenerationRng.nextInt(availableSecretCharacters.length)]
            }
            initSecret = secret.concatToString()
            val initSecretFileString = DataManager.generateStringPadding().concatToString() + "\n" + initSecret + "\n" + DataManager.generateStringPadding().concatToString()
            DataManager.writeFile("initSecret", filesDir, initSecretFileString.toByteArray(Charsets.UTF_8), false)
        }
        else {
            initSecret = String(initSecretFileContent, Charsets.UTF_8).substringAfter("\n").substringBefore("\n")
        }

        binding.etProfileName.setText(currentProfileName)
        binding.etProfileBio.setText(currentProfileBio)
        binding.etProfileHandle.setText(currentProfileHandle)
        binding.etProfileHandlePassword.setText(currentHandlePassword)
        binding.cbAllowPublicInit.isChecked = currentHandleAllowPublicInit
        binding.tvInitSecret.text = getString(R.string.settings_text_init_secret, initSecret)
        binding.tvInitSecret.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipboardContent = ClipData.newPlainText("dawn init secret", initSecret)
            clipboardManager.setPrimaryClip(clipboardContent)
            val toast = Toast.makeText(this, getString(R.string.settings_toast_code_copied), Toast.LENGTH_SHORT)
            toast.show()
        }

        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // check for changes and ask the user if they want to save them
                checkForChanges()
            }
        })

        binding.etProfileName.addTextChangedListener {
            profileNameChanges = binding.etProfileName.text.toString() != currentProfileName
        }
        binding.etProfileBio.addTextChangedListener {
            profileBioChanges = binding.etProfileBio.text.toString() != currentProfileBio
        }
        binding.etProfileHandle.addTextChangedListener {
            if(!checkHandle(binding.etProfileHandle.text.toString()))
                binding.etProfileHandleWrapper.error = getString(R.string.settings_error_handle_invalid)
            else {
                profileHandleChanges = checkHandleChanges()
                binding.etProfileHandleWrapper.error = null
            }
        }

        binding.etProfileHandlePassword.addTextChangedListener {
            profileHandleChanges = checkHandleChanges()
        }

        binding.cbAllowPublicInit.setOnCheckedChangeListener {
                _, _ -> profileHandleChanges = checkHandleChanges()
        }

        val themeSelectorItems = listOf(getString(R.string.theme_dark), getString(R.string.theme_extradark), getString(R.string.theme_light))
        val themeSelectorAdapter = ArrayAdapter(this, R.layout.dropdown_menu_item, themeSelectorItems)
        themeUseSystem = mThemeLoader.getThemeMode(this)

        if(themeUseSystem == Preferences.THEME_USE_SYSTEM) {
            binding.cbThemeUseSystem.isChecked = true
            binding.etThemeManual.isEnabled = false
        }
        else {
            binding.etThemeSystemLight.isEnabled = false
            binding.etThemeSystemDark.isEnabled = false
        }
        binding.cbThemeUseSystem.setOnCheckedChangeListener {
                _, _ -> toggleThemeEditTexts()
            themeModeChanges = binding.cbThemeUseSystem.isChecked xor themeUseSystem
        }

        val editTextColor = MaterialColors.getColor(this, android.R.attr.textColor, ContextCompat.getColor(this, R.color.white))
        val endIconTintList = ColorStateList(arrayOf(intArrayOf()), intArrayOf(editTextColor))

        currentManualThemeName = getThemeName(mThemeLoader.getThemeManualSetting(this))
        binding.etThemeManual.setText(currentManualThemeName)
        binding.etThemeManual.setAdapter(themeSelectorAdapter)
        binding.etThemeManual.addTextChangedListener {
            manualThemeChanges = it.toString() != currentManualThemeName
        }
        binding.etThemeManual.setTextColor(editTextColor)
        binding.etThemeManualWrapper.setEndIconTintList(endIconTintList)

        currentSystemLightThemeName = getThemeName(mThemeLoader.getThemeLightSetting(this))
        binding.etThemeSystemLight.setText(currentSystemLightThemeName)
        binding.etThemeSystemLight.setAdapter(themeSelectorAdapter)
        binding.etThemeSystemLight.addTextChangedListener {
            systemLightThemeChanges = it.toString() != currentSystemLightThemeName
        }
        binding.etThemeSystemLight.setTextColor(editTextColor)
        binding.etThemeSystemLightWrapper.setEndIconTintList(endIconTintList)

        currentSystemDarkThemeName = getThemeName(mThemeLoader.getThemeDarkSetting(this))
        binding.etThemeSystemDark.setText(currentSystemDarkThemeName)
        binding.etThemeSystemDark.setAdapter(themeSelectorAdapter)
        binding.etThemeSystemDark.addTextChangedListener {
            systemDarkThemeChanges = it.toString() != currentSystemDarkThemeName
        }
        binding.etThemeSystemDark.setTextColor(editTextColor)
        binding.etThemeSystemDarkWrapper.setEndIconTintList(endIconTintList)
    }

    override fun onResume() {
        binding.toolbar.title = actionBarText
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean {
        checkForChanges()
        return true
    }

    private fun checkForChanges() {
        Log.i(logTag, "Checking for changed settings...")
        if(!(profileNameChanges || profileBioChanges || profileHandleChanges || themeModeChanges || manualThemeChanges || systemDarkThemeChanges || systemLightThemeChanges)) {
            Log.i(logTag, "No changed settings found. Closing Settings.")
            finish()
            return
        }
        // ask if the changes should be saved
        Log.i(logTag, "Changed settings detected. Asking if the user wants to save them.")
        val saveSettingsDialog = MaterialAlertDialogBuilder(this, R.style.Theme_Dawn_Dialog)
        saveSettingsDialog.setTitle(R.string.settings_title_save_changes_dialog)
        saveSettingsDialog.setMessage(R.string.settings_text_save_changes_dialog)
        saveSettingsDialog.setCancelable(true)
        saveSettingsDialog.setNegativeButton(R.string.discard) {_: DialogInterface, _:Int -> finish() }
        saveSettingsDialog.setNeutralButton(R.string.cancel, null)
        saveSettingsDialog.setPositiveButton(R.string.save) { _: DialogInterface, _: Int -> saveChanges() }
        saveSettingsDialog.create().show()
    }

    private fun saveChanges() {
        // save the changes
        Log.i(logTag, "Saving changes")
        var error = false

        if(profileNameChanges) {
            val profileNameStringPrePadding = DataManager.generateStringPadding()
            val profileNameStringPostPadding = DataManager.generateStringPadding()
            val profileNameString = profileNameStringPrePadding.concatToString() + "\n" + binding.etProfileName.text.toString() + "\n" + profileNameStringPostPadding.concatToString()
            DataManager.writeFile("profileName", filesDir, profileNameString.toByteArray(Charsets.UTF_8), true)
        }

        if(profileBioChanges) {
            val profileBioStringPrePadding = DataManager.generateStringPadding()
            val profileBioStringPostPadding = DataManager.generateStringPadding()
            val profileBioString = profileBioStringPrePadding.concatToString() + "\n" + binding.etProfileBio.text.toString() + "\n" + profileBioStringPostPadding.concatToString()
            DataManager.writeFile("profileBio", filesDir, profileBioString.toByteArray(Charsets.UTF_8), true)
        }

        if(profileHandleChanges && checkHandle(binding.etProfileHandle.text.toString())) {
            val initIdBytes = DataManager.readFile("initId", filesDir)
            if(initIdBytes == null) {
                error = true
                toastError(getString(R.string.settings_error_no_init_id))
            }
            else {
                val initId = String(initIdBytes, Charsets.UTF_8).substringAfter("\n").substringBefore("\n")
                if(initId == "") {
                    error = true
                    toastError(getString(R.string.settings_error_no_init_id))
                }
                else {
                    val handlePassword = binding.etProfileHandlePassword.text.toString()
                    val setHandleRequest = RequestFactory.buildSetHandleRequest(
                        id = initId,
                        handle = binding.etProfileHandle.text.toString(),
                        password = handlePassword,
                        initSecret = initSecret,
                        allowPublicInit =  binding.cbAllowPublicInit.isActivated)
                    val response = mService.makeRequest(setHandleRequest)

                    if(response.code == 204) {
                        val profileHandleStringPrePadding = DataManager.generateStringPadding()
                        val profileHandleStringPostPadding = DataManager.generateStringPadding()
                        val profileHandleString =
                            profileHandleStringPrePadding.concatToString() + "\n" + binding.etProfileHandle.text.toString() + "\n" + profileHandleStringPostPadding.concatToString()
                        error = !DataManager.writeFile(
                            "profileHandle",
                            filesDir,
                            profileHandleString.toByteArray(Charsets.UTF_8),
                            true
                        )

                        val handlePasswordString = DataManager.generateStringPadding().concatToString() + "\n" + handlePassword + "\n" + DataManager.generateStringPadding()
                        error = error || !DataManager.writeFile("profileHandlePassword", filesDir, handlePasswordString.toByteArray(Charsets.UTF_8), true)

                        val handlePublicInit = binding.cbAllowPublicInit.isChecked.toString()
                        error = error || !DataManager.writeFile("profileHandlePublicInit", filesDir, handlePublicInit.toByteArray(Charsets.UTF_8), true)

                        if(!error) Log.i(logTag, "Changed handle successfully!")
                    }
                    else {
                        error = true
                        Log.w(logTag, "Could not edit handle: $response, ${response.body}")
                    }
                }
            }
        }

        if(themeModeChanges) {
            mThemeLoader.setThemeMode(this, binding.cbThemeUseSystem.isChecked)
        }

        if(manualThemeChanges) {
            when(binding.etThemeManual.text.toString()) {
                getString(R.string.theme_dark) -> {
                    mThemeLoader.setThemeManualSetting(this, Preferences.THEME_DARK)
                }
                getString(R.string.theme_extradark) -> {
                    mThemeLoader.setThemeManualSetting(this, Preferences.THEME_EXTRADARK)
                }
                getString(R.string.theme_light) -> {
                    mThemeLoader.setThemeManualSetting(this, Preferences.THEME_LIGHT)
                }
            }
        }

        if(systemDarkThemeChanges) {
            when(binding.etThemeSystemDark.text.toString()) {
                getString(R.string.theme_dark) -> {
                    mThemeLoader.setThemeDarkSetting(this, Preferences.THEME_DARK)
                }
                getString(R.string.theme_extradark) -> {
                    mThemeLoader.setThemeDarkSetting(this, Preferences.THEME_EXTRADARK)
                }
                getString(R.string.theme_light) -> {
                    mThemeLoader.setThemeDarkSetting(this, Preferences.THEME_LIGHT)
                }
            }
        }

        if(systemLightThemeChanges) {
            when(binding.etThemeSystemLight.text.toString()) {
                getString(R.string.theme_dark) -> {
                    mThemeLoader.setThemeLightSetting(this, Preferences.THEME_DARK)
                }
                getString(R.string.theme_extradark) -> {
                    mThemeLoader.setThemeLightSetting(this, Preferences.THEME_EXTRADARK)
                }
                getString(R.string.theme_light) -> {
                    mThemeLoader.setThemeLightSetting(this, Preferences.THEME_LIGHT)
                }
            }
        }
        if(!error) finish()
        else {
            val errorSavingChangesDialog = MaterialAlertDialogBuilder(this)

            errorSavingChangesDialog.setTitle(R.string.settings_title_error_saving_changes)
            errorSavingChangesDialog.setMessage(R.string.settings_text_error_saving_changes)
            errorSavingChangesDialog.setCancelable(false)
            errorSavingChangesDialog.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            errorSavingChangesDialog.setNegativeButton(R.string.discard) { _: DialogInterface, _: Int -> finish() }
            errorSavingChangesDialog.create().show()
        }
    }

    private fun checkHandle(handle: String): Boolean {
        if (!handle.all { it.isLetterOrDigit() || it.toString() == "-" || it.toString() == "_" }) {
            return false
        }
        return true
    }

    private fun getThemeName(themeId: Int): String {
        return when(themeId) {
            Preferences.THEME_EXTRADARK -> {
                getString(R.string.theme_extradark)
            }
            Preferences.THEME_LIGHT -> {
                getString(R.string.theme_light)
            }
            else -> {
                getString(R.string.theme_dark)
            }
        }
    }

    private fun toggleThemeEditTexts() {
        binding.etThemeManual.isEnabled = !binding.etThemeManual.isEnabled
        binding.etThemeSystemLight.isEnabled = !binding.etThemeSystemLight.isEnabled
        binding.etThemeSystemDark.isEnabled = !binding.etThemeSystemDark.isEnabled
    }

    private fun checkHandleChanges(): Boolean {
        return binding.etProfileHandle.text.toString() != currentProfileHandle || binding.etProfileHandlePassword.text.toString() != currentHandlePassword || binding.cbAllowPublicInit.isChecked != currentHandleAllowPublicInit
    }

    private fun toastError(error: String) {
        val toast = Toast.makeText(this, error, Toast.LENGTH_LONG)
        toast.show()
    }
}