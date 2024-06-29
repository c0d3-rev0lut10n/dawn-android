/*
 * Copyright (c) 2024  Laurenz Werner
 *
 * This file is part of Dawn.
 *
 * Dawn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dawn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dawn.  If not, see <http://www.gnu.org/licenses/>.
 */

package dawn.android

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dawn.android.annotation.ConcurrentAnnotation
import dawn.android.data.Chat
import dawn.android.data.ChatType
import dawn.android.data.ContentType
import dawn.android.data.Default
import dawn.android.data.Message
import dawn.android.data.Preferences
import dawn.android.data.Theme
import dawn.android.data.numeric
import dawn.android.databinding.ActivityShowChatBinding
import dawn.android.ui.component.ChatMessagesAdapter
import dawn.android.util.ChatManager
import dawn.android.util.PreferenceManager
import dawn.android.util.ThemeLoader

class ShowChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowChatBinding
    private lateinit var mTheme: Theme
    private var androidTheme: Int = 0
    private lateinit var actionBarText: SpannableString
    private lateinit var logTag: String
    private lateinit var mThemeLoader: ThemeLoader
    private lateinit var chat: Chat

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

        bindService(
            Intent(this, ReceiveMessagesService::class.java),
            connection,
            BIND_AUTO_CREATE
        )

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
        binding.contentLayout.adapter = ChatMessagesAdapter(this, R.layout.own_message_view, chat.messages)
        binding.btnSendMessage.setOnClickListener { send() }
    }

    override fun onResume() {
        binding.toolbar.title = actionBarText
        super.onResume()
    }

    @OptIn(ConcurrentAnnotation::class)
    private fun send() {
        val text = binding.etChatMessage.editText?.text.toString()
        if(text == "") return // don't send empty messages
        if(chat.type == ChatType.SENT_INIT) return // TODO: notify the user that they can't send messages to uninitialized chats
        if(chat.type == ChatType.GROUP) return // TODO: remove this after groups are fully supported

        val contentType = ContentType.TEXT // TODO allow other message types
        val ownSeckeySig = PreferenceManager.get(Preferences.sign.ownPrivateKey).unwrap()
        // encrypt message
        // the ID does not have to be up-to-date as the MDC gets derived by ReceiveMessagesService
        val messageResult = LibraryConnector.mSendMsg(
            msg_type = contentType.numeric(),
            msg_string = text,
            msg_bytes = ByteArray(0),
            remote_pubkey_kyber = chat.remoteKyber,
            own_pubkey_sig = ownSeckeySig,
            pfs_key = chat.ownPFS,
            pfs_salt = chat.pfsSalt,
            id = chat.id,
            mdc_seed = chat.mdcSeed
        )
        if(messageResult.isErr()) {
            val toast = Toast.makeText(this, getString(R.string.showchat_error_encrypting, messageResult.print()), Toast.LENGTH_LONG)
            toast.show()
            return
        }

        val message = messageResult.unwrap()
        val ciphertext = Base64.decode(message.ciphertext, Base64.NO_WRAP)
        chat.ownPFS = message.new_pfs_key!!
        ChatManager.updateChat(chat)

        val messageInChat = Message(
            chatDataId = chat.dataId,
            id = (chat.lastMessageId + 1U).toULong(),
            sender = ChatManager.getProfile(Default.ProfileSelfDataId).unwrap(),
            sent = null,
            received = null,
            read = null,
            contentType = ContentType.TEXT,
            text = text,
            media = null
        )
        mService.transmitMessage(chat.dataId, messageInChat, ciphertext)
    }
}