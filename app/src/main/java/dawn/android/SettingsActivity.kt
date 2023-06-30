package dawn.android

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import dawn.android.data.Theme
import dawn.android.databinding.ActivitySettingsBinding
import dawn.android.util.ThemeLoader

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var mTheme: Theme
    private lateinit var actionBarText: SpannableString

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val mThemeLoader = ThemeLoader(this)
        mTheme = mThemeLoader.loadDarkTheme()

        window.statusBarColor = mTheme.primaryBackgroundColor

        val actionBar = binding.toolbar
        val actionBarTextColor = mTheme.primaryTextColor
        val actionBarString = "Settings"
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        actionBar.setBackgroundColor(mTheme.primaryBackgroundColor)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.backButtonIcon)
    }

    override fun onResume() {
        binding.toolbar.title = actionBarText
        super.onResume()
    }
}