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
import android.util.Base64
import android.util.Log
import android.widget.Toast
import dawn.android.data.Chat
import dawn.android.data.HandlePrivateInfo
import dawn.android.data.Ok
import dawn.android.data.Result
import dawn.android.data.Result.Companion.ok
import dawn.android.data.Result.Companion.err
import dawn.android.data.SentInitRequest
import dawn.android.util.DataManager
import dawn.android.util.PreferenceManager
import dawn.android.util.RequestFactory
import dawn.android.util.TorReceiver
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.torproject.jni.TorService
import org.torproject.jni.TorService.LocalBinder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Timer
import kotlin.concurrent.timer


class ReceiveMessagesService: Service() {

    private val bindInterface : IBinder = BindInterface()
    private val mTorReceiver = TorReceiver
    private lateinit var notificationManager: NotificationManager
    private lateinit var chats: ArrayList<Chat>
    private lateinit var handle: String
    private lateinit var initId: String
    private lateinit var initKeyDirectory: File
    private val useTor = true
    private lateinit var torProxy: Proxy
    private lateinit var client: OkHttpClient
    private lateinit var logTag: String

    private var pollHandleAddKeyTimer: Timer? = null
    private var handleAddKeyActive = false

    companion object {
        var isRunning = false
        var isReady = false
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

        initKeyDirectory = File(filesDir, "initKeys")

        logTag = "$packageName.ReceiveMessagesService"

        torProxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("localhost", 19050)) // planned to be configurable
        client = if(useTor)
            OkHttpClient.Builder().proxy(torProxy).build()
        else
            OkHttpClient.Builder().proxy(null).build()

        setupForegroundServiceWithNotification()

        if(useTor) startTor()

        loadHandleInfo()
        pollHandleAddKeyTimer = timer(null, false, 5000, 30000) {
            if (!handleAddKeyActive) {
                handleAddKeyActive = true
                Log.d(logTag, "Polling handles...")
                val result = pollHandleAddKey()
                if (result.isErr()) {
                    Log.w(logTag, "pollHandleAddKey returned error: ${result.unwrapErr()}")
                }
                Log.d(logTag, "Finished polling handles")
                handleAddKeyActive = false
            }
        }

        val serverAddress = PreferenceManager.get("server").unwrap()

        RequestFactory.setMessageServerAddress(serverAddress)
        chats = DataManager.getAllChats()

        isReady = true

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

    private fun pollChats() {
        for(chat in chats) {
            val timestampsToCheckResult = LibraryConnector.mGetAllTimestampsSince(chat.idStamp)
            if(timestampsToCheckResult.isErr()) {
                Log.e(logTag, "Deriving timestamps failed: ${timestampsToCheckResult.unwrapErr()}")
                return
            }
            val timestampsToCheck = timestampsToCheckResult.unwrap()
            for(timestamp in timestampsToCheck.timestamps!!) {
                val temporaryIdResult = LibraryConnector.mGetCustomTempId(chat.id, timestamp)
                if(temporaryIdResult.isErr()) {
                    Log.e(logTag, "Getting temporary ID failed: ${temporaryIdResult.unwrapErr()}")
                    return
                }
                val temporaryId = temporaryIdResult.unwrap()
                while(chat.lastMessageId < 60000U) {
                    val request = RequestFactory.buildRcvRequest(temporaryId.id!!, chat.lastMessageId)
                    val response = client.newCall(request).execute()
                    if(!response.isSuccessful) {
                        Log.w(logTag, "Request $request failed, response: ${response.code}")
                        return
                    }
                    if (response.code == 204) break // there are no messages left to receive for this ID. Use the next one!
                    // save the message and increment messageId
                    // TODO: save the message and build notification
                    chat.lastMessageId++
                    DataManager.saveChatMessageId(chat.dataId, chat.lastMessageId)
                }
                val currentTimestamp = LibraryConnector.mGetCurrentTimestamp()
                if(currentTimestamp.isErr()) return
                if (chat.idStamp == currentTimestamp.unwrap().timestamp) continue// if the checked ID is the currently used one, we do not need to derive a new one. Therefore, we continue
                // we are done with this ID, derive and save the new one
                val nextId = LibraryConnector.mGetNextId(chat.id, chat.idSalt)
                if(nextId.isErr()) {
                    Log.e(logTag, "Deriving next ID for chat $chat failed: ${nextId.unwrapErr()}")
                }
                chat.id = nextId.unwrap().id!!
                DataManager.saveChatMessageId(chat.dataId, 0U)
                DataManager.saveChatId(chat.dataId, chat.id, timestamp)
            }
        }
    }

    private fun pollInitID() {

    }

    private fun pollHandleAddKey(): Result<Ok, String> {
        if(handle == "") return ok(Ok)
        val handleDir = File(filesDir, "handles")
        if(!handleDir.isDirectory) handleDir.mkdir()

        for(i in 0..15) {
            val keyFile = File(handleDir, i.toString())
            if (!keyFile.isFile) {
                Log.i(logTag, "Generating new handle with number: $i")
                val initKeysKyberResult = LibraryConnector.mKyberKeygen()
                if(initKeysKyberResult.isErr()) return err(initKeysKyberResult.unwrapErr())
                val initKeysCurveResult = LibraryConnector.mCurveKeygen()
                if(initKeysCurveResult.isErr()) return err(initKeysCurveResult.unwrapErr())
                val initKeysCurvePfs2Result = LibraryConnector.mCurveKeygen()
                if(initKeysCurvePfs2Result.isErr()) return err(initKeysCurvePfs2Result.unwrapErr())
                val initKeysKyberSaltResult = LibraryConnector.mKyberKeygen()
                if(initKeysKyberSaltResult.isErr()) return err(initKeysKyberSaltResult.unwrapErr())
                val initKeysCurveSaltResult = LibraryConnector.mCurveKeygen()
                if(initKeysCurveSaltResult.isErr()) return err(initKeysCurveSaltResult.unwrapErr())
                val nameResult = DataManager.getOwnProfileName()
                if(nameResult.isErr()) return err(nameResult.unwrapErr())

                val kyberKeys = initKeysKyberResult.unwrap()
                val curveKeys = initKeysCurveResult.unwrap()
                val curvePfs2Keys = initKeysCurvePfs2Result.unwrap()
                val kyberSaltKeys = initKeysKyberSaltResult.unwrap()
                val curveSaltKeys = initKeysCurveSaltResult.unwrap()

                val handleResult = LibraryConnector.mGenHandle(
                    kyberKeys.own_pubkey_kyber!!,
                    curveKeys.own_pubkey_curve!!,
                    curvePfs2Keys.own_pubkey_curve!!,
                    kyberSaltKeys.own_pubkey_kyber!!,
                    curveSaltKeys.own_pubkey_curve!!,
                    nameResult.unwrap()
                )
                if(handleResult.isErr()) return err(handleResult.unwrapErr())
                val handleContent = Base64.decode(handleResult.unwrap().handle!!, Base64.NO_WRAP)
                if(!DataManager.writeFile(
                        i.toString(),
                        handleDir,
                        handleContent,
                        false
                    )
                ) return err("Could not write to handle file")
                val handlePrivateInfo = HandlePrivateInfo(
                    initKeypairKyber = kyberKeys,
                    initKeypairCurve = curveKeys,
                    initKeypairCurvePfs2 = curvePfs2Keys,
                    initKeypairKyberSalt = kyberSaltKeys,
                    initKeypairCurveSalt = curveSaltKeys
                )
                if(!DataManager.writeFile(
                        "$i.private",
                        handleDir,
                        handlePrivateInfo.toString().toByteArray(Charsets.UTF_8),
                        false
                    )
                ) return err("Could not write to handle private info file")
                val handlePasswordFileContent =
                    DataManager.readFile("profileHandlePassword", filesDir)
                val handlePassword = if(handlePasswordFileContent != null)
                    String(handlePasswordFileContent, Charsets.UTF_8).substringAfter("\n")
                        .substringBefore("\n")
                else
                    return err("Could not get handle password")
                val request =
                    RequestFactory.buildAddKeyRequest(handle, handlePassword, handleContent)
                val response = makeRequest(request)
                if(response.isOk() && response.unwrap().isSuccessful) {
                    File(handleDir, "$i.uploaded").createNewFile()
                }
                else {
                    if(response.isOk())
                        Log.w(logTag, "Could not upload new handle key: ${request.url} returned response: ${response.unwrap().body?.string()}")
                    else
                        Log.w(logTag, "Could not upload new handle key: ${request.url} call failed: ${response.unwrapErr()}")
                }
            }
            else if(!File(handleDir, "$i.uploaded").isFile) {
                // try reupload
                val handleContent = DataManager.readFile(i.toString(), handleDir)
                if(handleContent != null) {
                    val handlePasswordFileContent =
                        DataManager.readFile("profileHandlePassword", filesDir)
                    val handlePassword = if(handlePasswordFileContent != null)
                        String(handlePasswordFileContent, Charsets.UTF_8).substringAfter("\n")
                            .substringBefore("\n")
                    else
                        return err("Could not get handle password")
                    val request = RequestFactory.buildAddKeyRequest(
                        handle,
                        handlePassword,
                        handleContent
                    )
                    val response = makeRequest(request)
                    if(response.isOk() && response.unwrap().isSuccessful) {
                        File(handleDir, "$i.uploaded").createNewFile()
                    }
                    else {
                        if(response.isOk())
                            Log.w(logTag, "Could not upload new handle key: ${request.url} returned response: ${response.unwrap().body?.string()}")
                        else
                            Log.w(logTag, "Could not upload new handle key: ${request.url} call failed: ${response.unwrapErr()}")
                    }
                }
            }
        }
        return ok(Ok)
    }

    fun searchHandleAndInit(handle: String, initSecret: String, comment: String): Result<String, String> {
        val request = RequestFactory.buildWhoRequest(handle, initSecret)
        val response = client.newCall(request).execute()
        if(!response.isSuccessful) {
            Log.w(logTag, "Request $request failed, response: ${response.code}")
            val body = response.body
            if(body != null) {
                val bodyString = body.string()
                println(bodyString)
                when(bodyString) {
                    "init not allowed" -> {
                    Toast.makeText(this, R.string.receive_text_wrong_init_secret, Toast.LENGTH_LONG)
                        .show()
                    }
                    "all key slots empty" -> {
                    Toast.makeText(this, R.string.receive_text_key_slots_empty, Toast.LENGTH_LONG)
                        .show()
                    }
                }
            }
            return err("Request $request failed, response: ${response.code}; ${response.body}")
        }
        if(response.code == 204) return err("handle not found")

        if(response.code == 200) {
            // parse received keys and ID
            val id = response.headers["X-ID"]?: return err("no ID associated with handle")
            val responseBody = response.body?: return err("Response $response to request $request did not have a request body")
            val handleInfoResult = LibraryConnector.mParseHandle(responseBody.bytes())
            if(handleInfoResult.isErr()) {
                return err("Could not parse handle: ${handleInfoResult.unwrapErr()}")
            }
            val handleInfo = handleInfoResult.unwrap()

            val ownSignKeypairResult = DataManager.getOwnProfileSignKeys()
            if(ownSignKeypairResult.isErr()) return err("could not get signature keypair: ${ownSignKeypairResult.unwrapErr()}")

            val ownSignKeypair = ownSignKeypairResult.unwrap()

            val profileNameResult = DataManager.getOwnProfileName()
            if(profileNameResult.isErr()) return err("could not get profile name: ${profileNameResult.unwrapErr()}")

            val initRequestResult = LibraryConnector.mGenInitRequest(
                handleInfo.init_pk_kyber!!,
                handleInfo.init_pk_kyber_for_salt!!,
                handleInfo.init_pk_curve!!,
                handleInfo.init_pk_curve_pfs_2!!,
                handleInfo.init_pk_curve_for_salt!!,
                ownSignKeypair.publicKey,
                ownSignKeypair.privateKey,
                profileNameResult.unwrap(),
                comment
            )

            if(initRequestResult.isErr()) return err("could not generate init request: ${initRequestResult.unwrapErr()}")
            val initRequest = initRequestResult.unwrap()

            val initRequestToSend = RequestFactory.buildSndRequest(id, Base64.decode(initRequest.ciphertext, Base64.NO_WRAP), initRequest.mdc!!)

            val sentInitResponse = client.newCall(initRequestToSend).execute()

            if(!sentInitResponse.isSuccessful) return err("could not send init request: ${sentInitResponse.code}; ${sentInitResponse.body}")

            val sentInitRequest = SentInitRequest(
                ownPubkeyKyber = initRequest.own_pubkey_kyber!!,
                ownSeckeyKyber = initRequest.own_seckey_kyber!!,
                ownPubkeyCurve = initRequest.own_pubkey_curve!!,
                ownSeckeyCurve = initRequest.own_seckey_curve!!,
                ownPFSKey = initRequest.own_pfs_key!!,
                remotePFSKey = initRequest.remote_pfs_key!!,
                pfsSalt = initRequest.pfs_salt!!,
                id = initRequest.id!!,
                idSalt = initRequest.id_salt!!,
                mdc = initRequest.mdc,
                ciphertext = initRequest.ciphertext!!
            )

            val initRequestDirectory = File(filesDir, "sentRequests")
            if(!initRequestDirectory.isDirectory) initRequestDirectory.mkdir()

            val sentInitRequestByteOutputStream = ByteArrayOutputStream()
            val sentInitRequestObjectOutputStream = ObjectOutputStream(sentInitRequestByteOutputStream)
            sentInitRequestObjectOutputStream.writeObject(sentInitRequest)
            val requestBytes = sentInitRequestByteOutputStream.toByteArray()
            if(!DataManager.writeFile(sentInitRequest.id, initRequestDirectory, requestBytes, false)) return err("could not write file")
        }

        return ok("success")
    }

    fun makeRequest(request: Request): Result<Response, String> {
        return try {
            ok(client.newCall(request).execute())
        } catch (e: Exception) {
            err(e.stackTraceToString())
        }
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

    private fun loadHandleInfo() {
        // get the handle
        val handleFileContent = DataManager.readFile("profileHandle", filesDir)
        handle = if(handleFileContent != null) {
            val paddedProfileHandle = String(handleFileContent, Charsets.UTF_8)
            paddedProfileHandle.substringAfter("\n").substringBefore("\n")
        } else ""

        // get the init ID
        val initIdResult = PreferenceManager.get("initId")
        initId = if(initIdResult.isErr()) ""
        else initIdResult.unwrap()
    }
}