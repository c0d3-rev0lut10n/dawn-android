package dawn.android

import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dawn.android.data.Chat
import dawn.android.data.Preferences
import dawn.android.data.Theme
import dawn.android.databinding.ActivityShowChatBinding
import dawn.android.util.ChatManager
import dawn.android.util.ThemeLoader

class ShowChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowChatBinding
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString
    private lateinit var logTag: String
    private lateinit var mThemeLoader: ThemeLoader
    private lateinit var chat: Chat

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

        val extras = intent.extras
        val dataId = extras?.getString("dataId")?: ""
        if(dataId == "") {
            Log.e(logTag, "failed to get dataId for chat from intent extras")
            finish()
            return
        }

        val getChat = ChatManager.getChat(dataId)
        if(getChat.isErr()) {
            Log.e(logTag, "failed to chat from dataId: ${getChat.print()}")
            finish()
            return
        }

        chat = getChat.unwrap()

        binding = ActivityShowChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        window.statusBarColor = mTheme.primaryUIColor
        window.navigationBarColor = mTheme.primaryBackgroundColor

        val actionBarTextColor = mTheme.secondaryTextColor
        val actionBarString = chat.name
        actionBarText = SpannableString(actionBarString)
        actionBarText.setSpan(ForegroundColorSpan(actionBarTextColor), 0, actionBarString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(mTheme.backButtonIcon)
    }
}