package dawn.android

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
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
        if(isRunning) {
            Log.w(packageName,"ReceiveMessagesService was requested to start, but is already running")
            stopSelf()
        }
        else isRunning = true

        setupForegroundServiceWithNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return bindInterface
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
}