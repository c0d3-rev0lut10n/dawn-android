package dawn.android

import android.content.DialogInterface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dawn.android.data.Preferences
import dawn.android.data.Theme
import dawn.android.databinding.ActivityDebugBinding
import dawn.android.util.DataManager
import dawn.android.util.PreferenceManager
import dawn.android.util.ThemeLoader
import java.io.File

class DebugActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugBinding
    private lateinit var mThemeLoader: ThemeLoader
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString
    private lateinit var logTag: String

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

        logTag = "$packageName.DebugActivity"
        binding = ActivityDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        window.statusBarColor = mTheme.primaryUIColor
        window.navigationBarColor = mTheme.primaryBackgroundColor

        val actionBarTextColor = mTheme.secondaryTextColor
        val actionBarString = getString(R.string.debug_app_bar)
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.backButtonIcon)

        binding.btnGetPreference.setOnClickListener { getPreference() }
        binding.btnSetPreference.setOnClickListener { setPreference() }
        binding.btnListFiles.setOnClickListener { listFiles() }
        binding.btnShowFileContent.setOnClickListener { showContent() }
    }

    override fun onResume() {
        binding.toolbar.title = actionBarText
        super.onResume()
    }

    private fun getPreference() {
        val key = binding.etPreference.text.toString()
        val result = PreferenceManager.get(key)

        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(R.string.debug_dialog_preference_get_heading)
        if(result.isOk())
            dialog.setMessage(getString(R.string.debug_dialog_preference_get_text_success, key, result.unwrap()))
        else
            dialog.setMessage(getString(R.string.debug_dialog_preference_get_text_not_found, key))
        dialog.setCancelable(true)
        dialog.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
        dialog.create().show()
    }

    private fun setPreference() {
        val key = binding.etPreference.text.toString()
        val value = binding.etPreferenceValue.text.toString()
        PreferenceManager.set(key, value)
        PreferenceManager.write()
    }

    private fun listFiles() {
        val path = binding.etFile.text.toString()
        val fileAtPath = File(filesDir.path + path)
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(R.string.debug_dialog_list_files_heading)
        if(fileAtPath.isDirectory) {
            val filesInDirectory = fileAtPath.listFiles()
            if(filesInDirectory.isNullOrEmpty()) dialog.setMessage(R.string.debug_dialog_list_files_empty)
            else {
                var directoryContent = ""
                for(file in filesInDirectory) {
                    val type =
                        if(file.isDirectory) getString(R.string.debug_dialog_list_files_directory)
                    else getString(R.string.debug_dialog_list_files_file)
                    directoryContent += file.name + " " + type + "\n"
                }
                dialog.setMessage(directoryContent)
            }
        }
        else {
            dialog.setMessage(R.string.debug_dialog_list_files_not_directory)
        }
        dialog.setCancelable(true)
        dialog.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
        dialog.create().show()
    }

    private fun showContent() {
        val path = binding.etFile.text.toString()
        val fileAtPath = File(filesDir.path + path)
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(R.string.debug_dialog_show_content_heading)
        if(fileAtPath.isFile) {
            val fileContent = DataManager.readFile(fileAtPath.name, fileAtPath.parentFile!!)
            if(fileContent == null) dialog.setMessage(R.string.debug_dialog_show_content_read_fail)
            else dialog.setMessage(String(fileContent, Charsets.UTF_8))
        }
        dialog.setCancelable(true)
        dialog.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
        dialog.create().show()
    }
}