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
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dawn.android.data.Preferences
import dawn.android.data.Theme
import dawn.android.databinding.ActivitySettingsBinding
import dawn.android.util.DataManager
import dawn.android.util.ThemeLoader

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString
    private lateinit var currentProfileName: String
    private lateinit var currentProfileBio: String
    private lateinit var currentProfileHandle: String
    private lateinit var currentManualThemeName: String
    private lateinit var currentSystemDarkThemeName: String
    private lateinit var currentSystemLightThemeName: String
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

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        window.statusBarColor = mTheme.primaryUIColor
        window.navigationBarColor = mTheme.primaryBackgroundColor

        val actionBarTextColor = mTheme.primaryTextColor
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

        binding.etProfileName.setText(currentProfileName)
        binding.etProfileBio.setText(currentProfileBio)
        binding.etProfileHandle.setText(currentProfileHandle)

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
                profileHandleChanges = binding.etProfileHandle.text.toString() != currentProfileHandle
                binding.etProfileHandleWrapper.error = null
            }
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

        currentManualThemeName = getThemeName(mThemeLoader.getThemeManualSetting(this))
        binding.etThemeManual.setText(currentManualThemeName)
        binding.etThemeManual.setAdapter(themeSelectorAdapter)
        binding.etThemeManual.addTextChangedListener {
            manualThemeChanges = it.toString() != currentManualThemeName
        }

        currentSystemLightThemeName = getThemeName(mThemeLoader.getThemeLightSetting(this))
        binding.etThemeSystemLight.setText(currentSystemLightThemeName)
        binding.etThemeSystemLight.setAdapter(themeSelectorAdapter)
        binding.etThemeSystemLight.addTextChangedListener {
            systemLightThemeChanges = it.toString() != currentSystemLightThemeName
        }

        currentSystemDarkThemeName = getThemeName(mThemeLoader.getThemeDarkSetting(this))
        binding.etThemeSystemDark.setText(currentSystemDarkThemeName)
        binding.etThemeSystemDark.setAdapter(themeSelectorAdapter)
        binding.etThemeSystemDark.addTextChangedListener {
            systemDarkThemeChanges = it.toString() != currentSystemDarkThemeName
        }
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
            // TODO: set a handle on the server
            val profileHandleStringPrePadding = DataManager.generateStringPadding()
            val profileHandleStringPostPadding = DataManager.generateStringPadding()
            val profileHandleString = profileHandleStringPrePadding.concatToString() + "\n" + binding.etProfileHandle.text.toString() + "\n" + profileHandleStringPostPadding.concatToString()
            DataManager.writeFile("profileHandle", filesDir, profileHandleString.toByteArray(Charsets.UTF_8), true)
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

        finish()
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
}