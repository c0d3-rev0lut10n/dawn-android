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
import android.widget.Toast
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
    private var profileNameChanges = false
    private var profileBioChanges = false
    private lateinit var logTag: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logTag = "$packageName.SettingsActivity"

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val mThemeLoader = ThemeLoader(this)
        val themeSwitch = mThemeLoader.getThemeSetting(this)
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

        binding.etProfileName.setText(currentProfileName)
        binding.etProfileBio.setText(currentProfileBio)

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
        if(!(profileNameChanges || profileBioChanges)) {
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
        finish()
    }

    private fun checkHandle(handleInput: String): Boolean {
        var handle = handleInput
        if (handle.startsWith("@",false)) {
            handle = handle.drop(1)
        }
        if (!handle.all { it.isLetterOrDigit() || it.toString() == "-" || it.toString() == "_" }) {
            return false
        }
        return true
    }
}