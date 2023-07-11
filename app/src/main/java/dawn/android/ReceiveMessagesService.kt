package dawn.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import dawn.android.util.DataManager

class ReceiveMessagesService : Service() {

    private val bindInterface : IBinder = BindInterface()
    private val mLibraryConnector = LibraryConnector
    private val mDataManager = DataManager
    private lateinit var notificationManager: NotificationManager

    companion object {
        var isRunning = false
    }

    inner class BindInterface: Binder() {
        fun getService(): ReceiveMessagesService {
            return this@ReceiveMessagesService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= 26) {
            val notificationChannel = NotificationChannel("BG_KEEPALIVE", getString(R.string.notification_channel_bg), NotificationManager.IMPORTANCE_LOW)
            notificationChannel.enableVibration(false)
            notificationChannel.enableLights(false)
            notificationManager.createNotificationChannel(notificationChannel)
            val notification = Notification.Builder(this, "BG_KEEPALIVE")
                .setOngoing(true)
                .build()
            startForeground(1, notification)
        }
        else {
            val notification = Notification.Builder(this)
                .setOngoing(true)
                .build()
            startForeground(1, notification)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return bindInterface
    }
}