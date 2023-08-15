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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import dawn.android.data.Chat
import dawn.android.util.DataManager
import dawn.android.util.RequestFactory
import dawn.android.util.TorReceiver
import okhttp3.OkHttpClient
import org.torproject.jni.TorService
import org.torproject.jni.TorService.LocalBinder
import java.net.InetSocketAddress
import java.net.Proxy


class ReceiveMessagesService: Service() {

    private val bindInterface : IBinder = BindInterface()
    private val mTorReceiver = TorReceiver
    private lateinit var notificationManager: NotificationManager
    private lateinit var chats: ArrayList<Chat>
    private var activeChat: Chat? = null
    private val useTor = true
    private lateinit var directHttpClient: OkHttpClient
    private lateinit var torProxy: Proxy
    private lateinit var torHttpClient: OkHttpClient

    companion object {
        var isRunning = false
    }

    inner class BindInterface: Binder() {
        fun getService(): ReceiveMessagesService {
            return this@ReceiveMessagesService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(isRunning) {
            Log.w(packageName,"ReceiveMessagesService was requested to start, but is already running")
            stopSelf()
        }
        else isRunning = true

        // we need this to create the okhttp client. Requests are made asynchronously anyway, so whatever
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        directHttpClient = OkHttpClient.Builder().proxy(null).build()
        torProxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("localhost", 19050)) // planned to be configurable
        torHttpClient = OkHttpClient.Builder().proxy(torProxy).build()

        setupForegroundServiceWithNotification()

        startTor()
        chats = DataManager.getAllChats()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return bindInterface
    }

    fun addChat(chat: Chat): Boolean {
        for(chatToCheck in chats) {
            if(chatToCheck.dataId == chat.dataId || chatToCheck.id == chat.id) return false
        }
        chats.add(chat)
        return true
    }

    fun setActiveChat(chat: Chat) {
        activeChat = chat
    }

    private fun pollChats() {
        for(chat in chats) {
            if(chat.dataId == activeChat?.dataId) continue // skip active chat as it gets polled separately
            if(chat.idStamp != LibraryConnector.mGetCurrentTimestamp().timestamp) {
                // this chat was not received with the current tempId, therefore we need to search for old messages
            }
            if(useTor) {

            }
            else {

            }
        }
    }

    private fun pollActiveChat() {

    }

    private fun setupForegroundServiceWithNotification() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationSettingsIntent = Intent()
        notificationSettingsIntent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        notificationSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        notificationSettingsIntent.putExtra("app_package", packageName)
        notificationSettingsIntent.putExtra("app_uid", applicationInfo.uid)
        notificationSettingsIntent.putExtra("android.provider.extra.APP_PACKAGE", packageName)

        val notificationPendingIntent = PendingIntent.getActivity(this, 0, notificationSettingsIntent, PendingIntent.FLAG_IMMUTABLE)

        if(Build.VERSION.SDK_INT >= 26) {
            val notificationChannel = NotificationChannel("BG_KEEPALIVE", getString(R.string.notification_channel_bg), NotificationManager.IMPORTANCE_LOW)
            notificationChannel.enableVibration(false)
            notificationChannel.enableLights(false)
            notificationManager.createNotificationChannel(notificationChannel)
            val notification = Notification.Builder(this, "BG_KEEPALIVE")
                .setOngoing(true)
                .setSmallIcon(R.drawable.notification)
                .setContentText(getString(R.string.notification_bg_content))
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(notificationPendingIntent)
                .build()
            startForeground(1, notification)
        }
        else {
            val notification = Notification.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.notification)
                .setContentText(getString(R.string.notification_bg_content))
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(notificationPendingIntent)
                .build()
            startForeground(1, notification)
        }
    }

    private fun startTor() {
        registerReceiver(mTorReceiver, IntentFilter(TorService.ACTION_STATUS))

        bindService(Intent(this, TorService::class.java), object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val torService = (service as LocalBinder).service

                while (torService.torControlConnection == null) {
                    try {
                        Thread.sleep(500)
                    }
                    catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }

                Log.i(packageName, "Tor control connection created!")
                torService.torControlConnection.setConf("SOCKSPort","19050")
            }

            override fun onServiceDisconnected(name: ComponentName?) {

            }
        }, BIND_AUTO_CREATE)
    }
}