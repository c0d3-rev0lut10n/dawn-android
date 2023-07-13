package dawn.android.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

import org.torproject.jni.TorService




object TorReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent == null) return
        val status = intent.getStringExtra(TorService.EXTRA_STATUS)
        val packageName = if(context != null) context.packageName
        else "dawn.android"
        Log.i(packageName, "Tor status: $status")
    }
}