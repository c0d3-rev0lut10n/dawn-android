package dawn.android

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dawn.android.util.DataManager

class ReceiveMessagesService : Service() {

    private val bindInterface : IBinder = BindInterface()
    private val mLibraryConnector = LibraryConnector
    private val mDataManager = DataManager

    companion object {
        var isRunning = false
    }

    inner class BindInterface: Binder() {
        fun getService(): ReceiveMessagesService {
            return this@ReceiveMessagesService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return bindInterface
    }
}