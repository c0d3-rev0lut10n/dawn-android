/*
 * Copyright (c) 2023-2024  Laurenz Werner
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

import android.annotation.SuppressLint
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
import dawn.android.annotation.ConcurrentAnnotation
import dawn.android.data.ChatType
import dawn.android.data.ContentType
import dawn.android.data.Default
import dawn.android.data.HandlePrivateInfo
import dawn.android.data.Keypair
import dawn.android.data.Message
import dawn.android.data.Ok
import dawn.android.data.Preferences
import dawn.android.data.Profile
import dawn.android.data.Result
import dawn.android.data.Result.Companion.err
import dawn.android.data.Result.Companion.ok
import dawn.android.messagereception.PollingId
import dawn.android.messagereception.Subscription
import dawn.android.messagereception.SubscriptionUpdates
import dawn.android.messagereception.SubscriptionUtil
import dawn.android.util.ChatManager
import dawn.android.util.DataManager
import dawn.android.util.PreferenceManager
import dawn.android.util.RequestFactory
import dawn.android.util.TorReceiver
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.torproject.jni.TorService
import org.torproject.jni.TorService.LocalBinder
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.Timer
import kotlin.concurrent.timer


class ReceiveMessagesService: Service() {

    private val bindInterface : IBinder = BindInterface()
    private val mTorReceiver = TorReceiver
    private lateinit var notificationManager: NotificationManager
    private lateinit var idRelations: HashMap<String, String>
    private lateinit var subscriptions: ArrayList<Subscription>
    private lateinit var initKeyDirectory: File
    private val useTor = true
    private lateinit var torProxy: Proxy
    private lateinit var client: OkHttpClient
    private lateinit var logTag: String

    private var tickTimer: Timer? = null
    private var tickInProgress = false

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

        logTag = this.javaClass.name

        subscriptions = ArrayList()
        idRelations = HashMap()

        torProxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("localhost", 19050)) // planned to be configurable
        client = if(useTor)
            OkHttpClient.Builder().proxy(torProxy).build()
        else
            OkHttpClient.Builder().proxy(null).build()

        setupForegroundServiceWithNotification()

        if(useTor) startTor()

        val serverAddress = PreferenceManager.get(Preferences.server).unwrap()
        RequestFactory.setMessageServerAddress(serverAddress)

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
        tickTimer = timer(null, false, 5000, 2000) { tick() }

        isReady = true

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return bindInterface
    }

    private fun tick() {
        if(tickInProgress) return
        tickInProgress = true
        // here happens everything that gets executed during a tick
        Log.i(logTag, "Starting tick")
        val pollChatResults = pollChats()
        if(pollChatResults.isErr()) Log.e(logTag, pollChatResults.print())
        val result = pollInitID() // commented out because it is not ready yet
        if(result.isErr()) Log.e(logTag, result.unwrapErr())
        tickInProgress = false
    }

    private fun pollChats(): Result<Ok, String> {
        val timestamp = ChatManager.getOldestIdStampToPoll()
        val chatsToPoll = ChatManager.getChatsToPoll(timestamp)
        val missingChats: ArrayList<PollingId> = ArrayList()

        // get all chats that are not part of a subscription yet
        for(chat in chatsToPoll.values) {
            if(chat.id !in idRelations.keys) {
                val mdc = LibraryConnector.mPredictableMdcGen(chat.mdcSeed, chat.id).unwrap().mdc!!
                missingChats.add(
                    PollingId(
                        chatDataId = chat.dataId,
                        id = chat.id,
                        mdc = mdc,
                        startId = chat.lastMessageId
                    )
                )
            }
        }

        // prepare subscriptions
        // TODO: let the user choose how many subscriptions should be created
        val generatedSubscriptions = SubscriptionUtil.createSubscriptions(missingChats, 10U)

        // actually create the subscriptions
        for(generatedSubscription in generatedSubscriptions) {
            if(generatedSubscription.associatedChats.isEmpty()) continue
            val request = RequestFactory.buildCreateSubscriptionRequest(generatedSubscription)
            val response = makeRequest(request)
            if(response.isErr()) return err("Creating a subscription failed: ${response.print()}\nSTOPPING FURTHER TICK WORK")
            val createSubscriptionResponse = response.unwrap()
            val body = createSubscriptionResponse.body
            val responseString = body?.string()
            body?.close()
            if(createSubscriptionResponse.code != 200) {
                return err("Creating a subscription failed: $responseString\nSTOPPING FURTHER TICK WORK")
            }
            generatedSubscription.id = responseString!!
            for(chat in generatedSubscription.associatedChats) {
                idRelations[chat.id] = chat.chatDataId
            }
            subscriptions.add(generatedSubscription)
        }

        // poll all subscriptions
        for(subscription in subscriptions) {
            val request = RequestFactory.buildSubRequest(subscription)
            val response = makeRequest(request)
            if(response.isErr()) {
                Log.w(logTag, "Polling a subscription failed: ${response.print()}\nSkipping this subscription.")
                continue
            }
            val subscriptionResponse = response.unwrap()
            val body = subscriptionResponse.body
            val responseString = body?.string()
            body?.close()
            when(subscriptionResponse.code) {
                400 -> {
                    // the subscription does not exist on the server anymore. Remove it and all ID relations and recreate it next tick
                    Log.i(logTag, "Subscription expired, removing it")
                    for(chat in subscription.associatedChats) {
                        idRelations.remove(chat.id)
                    }
                    subscriptions.remove(subscription)
                    continue
                }
                500 -> {
                    Log.w(logTag, "Polling a subscription failed: $responseString\nSkipping this subscription")
                    continue
                }
            }
            val update = Json.decodeFromString<SubscriptionUpdates>(responseString!!)
            for(messageInfo in update.messages) {
                if(messageInfo.status != "ok") continue
                val message = messageInfo.message!!
                val dataId = idRelations[message.id]?: continue
                val getChat = ChatManager.getChat(dataId)
                if(getChat.isErr()) continue
                val chat = getChat.unwrap()

                // skip groups for now
                // TODO: remove this once groups are supported
                if(chat.type == ChatType.GROUP) continue

                val profileResult = ChatManager.getProfile(chat.associatedProfileId!!)
                if(profileResult.isErr()) continue

                val profile = profileResult.unwrap()

                val messageContent = Base64.decode(message.content, Base64.NO_WRAP)
                val messageResult = LibraryConnector.mParseMsg(messageContent, chat.ownKyber.privateKey, profile.pubkeySig, chat.remotePFS, chat.pfsSalt)
            }
        }
        return err("not implemented")
    }

    @OptIn(ConcurrentAnnotation::class)
    private fun pollChatsLegacy() {
        val chats = ChatManager.getAllChats()
        for(entry in chats) {
            val chat = entry.value
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
                    ChatManager.updateChat(chat)
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
                chat.lastMessageId = 0U
                ChatManager.updateChat(chat)
            }
        }
    }

    private fun pollInitID(): Result<Ok, String> {
        val initIdResult = PreferenceManager.get("initId")
        if(initIdResult.isErr() || initIdResult.unwrap() == "") return err("Could not get init ID")
        val initId = initIdResult.unwrap()
        var initMessageNumber: UShort
        val initMessageNumberResult = PreferenceManager.get("initMessageNumber")
        if(initMessageNumberResult.isErr()) {
            initMessageNumber = 0U
            PreferenceManager.set("initMessageNumber", "0")
            PreferenceManager.write()
        }
        else initMessageNumber = initMessageNumberResult.unwrap().toUShort()
        val initIdRequest = RequestFactory.buildRcvRequest(initId, initMessageNumber)
        val initIdResponseResult = makeRequest(initIdRequest)
        if(initIdResponseResult.isErr()) return err("Could not poll init ID: ${initIdResponseResult.unwrapErr()}")
        val initIdResponse = initIdResponseResult.unwrap()
        when(initIdResponse.code) {
            200 -> {
                // there is a new init request
                val initMdcResult = PreferenceManager.get("initMdc")
                if(initMdcResult.isErr()) {
                    return err("Could not get init MDC: ${initMdcResult.unwrapErr()}")
                }
                val initMdc = initMdcResult.unwrap()

                val getInitRequestResult = makeRequest(RequestFactory.buildRcvRequest(initId, initMessageNumber, initMdc))
                if(getInitRequestResult.isErr()) return err("Could not download new init request: ${getInitRequestResult.unwrapErr()}")
                val getInitRequestResponse = getInitRequestResult.unwrap()
                if(getInitRequestResponse.code != 200) {
                    val responseBody = getInitRequestResponse.body
                    val responseString = responseBody?.string()
                    responseBody?.close()
                    return err("Could not download new init request: ${getInitRequestResponse.code}\n$responseString")
                }
                val initRequestBytesStream = getInitRequestResponse.body?: return err("Could not download new init request: could not get response body")
                val initRequestBytes = initRequestBytesStream.bytes()
                initRequestBytesStream.close()

                initMessageNumber = (initMessageNumber+1U).toUShort()
                PreferenceManager.set("initMessageNumber", initMessageNumber.toString())
                PreferenceManager.write()

                val referrer = getInitRequestResponse.header("X-Referrer")
                if(referrer.isNullOrEmpty()) return err("Init request did not come with a referrer")

                val handleDir = File(filesDir, "handles")
                val handlePrivateInfoFile = DataManager.readFile(referrer, handleDir)
                    ?: return err("Init request referrer does not point to any saved handle on this device: $referrer")
                val handlePrivateInfoResult = HandlePrivateInfo.fromString(String(handlePrivateInfoFile, Charsets.UTF_8))
                if(handlePrivateInfoResult.isErr()) return err("Could not read handle private info: ${handlePrivateInfoResult.unwrapErr()}")
                val handlePrivateInfo = handlePrivateInfoResult.unwrap()
                val initRequest = LibraryConnector.mParseInitRequest(
                    initRequestBytes,
                    handlePrivateInfo.initKeypairKyber.own_seckey_kyber!!,
                    handlePrivateInfo.initKeypairCurve.own_seckey_curve!!,
                    handlePrivateInfo.initKeypairCurvePfs2.own_seckey_curve!!,
                    handlePrivateInfo.initKeypairKyberSalt.own_seckey_kyber!!,
                    handlePrivateInfo.initKeypairCurveSalt.own_seckey_curve!!
                    )
                if(initRequest.isErr()) return err("Could not parse init request: ${initRequest.unwrapErr()}")
                val receivedRequestsDirectory = File(filesDir, "receivedRequests")
                if(!receivedRequestsDirectory.isDirectory) receivedRequestsDirectory.mkdir()

                val requestBytes = Json.encodeToString(initRequest.unwrap()).toByteArray(Charsets.UTF_8)
                if(!DataManager.writeFile(initRequest.unwrap().id!!, receivedRequestsDirectory, requestBytes, false)) return err("could not write file")
            }
            204 -> {
                // no new init request
                return ok(Ok)
            }
            else -> {
                // either an error or an unrecognized status
                val responseBody = initIdResponse.body
                val responseString = responseBody?.string()
                responseBody?.close()
                return err("Could not poll init ID: invalid response ${initIdResponse.code}\n$responseString")
            }
        }
        return ok(Ok)
    }

    private fun pollHandleAddKey(): Result<Ok, String> {
        val handleNameResult = PreferenceManager.get(Preferences.profileHandle)
        if(handleNameResult.isErr() || handleNameResult.unwrap() == "") return ok(Ok)
        val handle = handleNameResult.unwrap()
        val initMdcResult = PreferenceManager.get("initMdc")
        if(initMdcResult.isErr()) {
            return err("Could not get init MDC: ${initMdcResult.unwrapErr()}")
        }
        val initMdc = initMdcResult.unwrap()
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
                val nameResult = PreferenceManager.get(Preferences.profileName)
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
                    nameResult.unwrap(),
                    initMdc
                )
                if(handleResult.isErr()) return err(handleResult.unwrapErr())
                val handleContent = Base64.decode(handleResult.unwrap().handle!!, Base64.NO_WRAP)
                val referrer = deriveReferrer(handleContent)
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
                        referrer,
                        handleDir,
                        handlePrivateInfo.toString().toByteArray(Charsets.UTF_8),
                        false
                    )
                ) return err("Could not write to handle private info file")
                val handlePasswordResult = PreferenceManager.get("profileHandlePassword")
                if(handlePasswordResult.isErr())
                    return err("Could not get handle password")
                val handlePassword = handlePasswordResult.unwrap()
                val request =
                    RequestFactory.buildAddKeyRequest(handle, handlePassword, handleContent)
                val response = makeRequest(request)
                if(response.isOk() && response.unwrap().isSuccessful) {
                    File(handleDir, "$i.uploaded").createNewFile()
                }
                else {
                    if(response.isOk()) {
                        val responseBody = response.unwrap().body
                        val responseString = responseBody?.string()
                        responseBody?.close()
                        Log.w(logTag, "Could not upload new handle key: ${request.url} returned response: $responseString")
                    }
                    else
                        Log.w(logTag, "Could not upload new handle key: ${request.url} call failed: ${response.unwrapErr()}")
                }
            }
            else if(!File(handleDir, "$i.uploaded").isFile) {
                // try reupload
                val handleContent = DataManager.readFile(i.toString(), handleDir)
                if(handleContent != null) {
                    val handlePasswordResult = PreferenceManager.get("profileHandlePassword")
                    if(handlePasswordResult.isErr())
                        return err("Could not get handle password")
                    val handlePassword = handlePasswordResult.unwrap()
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
                        if(response.isOk()) {
                            val responseBody = response.unwrap().body
                            val responseString = responseBody?.string()
                            responseBody?.close()
                            Log.w(logTag, "Could not upload new handle key: ${request.url} returned response: $responseString")
                        }
                        else
                            Log.w(logTag, "Could not upload new handle key: ${request.url} call failed: ${response.unwrapErr()}")
                    }
                }
            }
        }
        return ok(Ok)
    }

    @OptIn(ConcurrentAnnotation::class)
    fun searchHandleAndInit(handle: String, initSecret: String, comment: String): Result<Ok, String> {
        val request = RequestFactory.buildWhoRequest(handle, initSecret)
        val response = client.newCall(request).execute()
        if(!response.isSuccessful) {
            Log.w(logTag, "Request $request failed, response: ${response.code}")
            val body = response.body
            val bodyString = body?.string()
            body?.close()
            when (bodyString) {
                "init not allowed" -> {
                    Toast.makeText(this, R.string.receive_text_wrong_init_secret, Toast.LENGTH_LONG)
                        .show()
                }

                "all key slots empty" -> {
                    Toast.makeText(this, R.string.receive_text_key_slots_empty, Toast.LENGTH_LONG)
                        .show()
                }
            }
            return err("Request $request failed, response: ${response.code}; $bodyString")
        }
        if(response.code == 204) return err("handle not found")

        if(response.code == 200) {
            // parse received keys and ID
            val id = response.headers["X-ID"]?: return err("no ID associated with handle")
            val responseBodyStream = response.body?: return err("Response $response to request $request did not have a response body")
            val responseBody = responseBodyStream.bytes()
            responseBodyStream.close()
            val handleInfoResult = LibraryConnector.mParseHandle(responseBody)
            if(handleInfoResult.isErr()) {
                return err("Could not parse handle: ${handleInfoResult.unwrapErr()}")
            }
            val handleInfo = handleInfoResult.unwrap()

            val ownSignPublicKey = PreferenceManager.get(Preferences.sign.ownPublicKey)
            val ownSignSecretKey = PreferenceManager.get(Preferences.sign.ownPrivateKey)
            if(ownSignPublicKey.isErr()) return err("searchHandleAndInit: could not get own keys: public key: ${ownSignPublicKey.print()}; private key: ${ownSignSecretKey.print()}")

            val profileNameResult = PreferenceManager.get(Preferences.profileName)
            if(profileNameResult.isErr()) return err("could not get profile name: ${profileNameResult.unwrapErr()}")

            val initRequestResult = LibraryConnector.mGenInitRequest(
                handleInfo.init_pk_kyber!!,
                handleInfo.init_pk_kyber_for_salt!!,
                handleInfo.init_pk_curve!!,
                handleInfo.init_pk_curve_pfs_2!!,
                handleInfo.init_pk_curve_for_salt!!,
                ownSignPublicKey.unwrap(),
                ownSignSecretKey.unwrap(),
                profileNameResult.unwrap(),
                comment,
                handleInfo.mdc!!
            )

            if(initRequestResult.isErr()) return err("could not generate init request: ${initRequestResult.unwrapErr()}")
            val initRequest = initRequestResult.unwrap()

            val referrer = deriveReferrer(responseBody)
            val initRequestToSend = RequestFactory.buildSndRequest(id, Base64.decode(initRequest.ciphertext, Base64.NO_WRAP), initRequest.mdc!!, referrer)

            val sentInitResponse: Response
            try {
                sentInitResponse = client.newCall(initRequestToSend).execute()
            }
            catch(e: Exception) {
                return err("could not send init request: ${e.printStackTrace()}")
            }

            if(!sentInitResponse.isSuccessful) {
                val sentInitResponseBody = sentInitResponse.body
                val responseString = sentInitResponseBody?.string()
                sentInitResponseBody?.close()
                return err("could not send init request: ${sentInitResponse.code}; $responseString")
            }

            val profilePrototype = Profile(
                dataId = Default.ToBeDeterminedDataId,
                name = handleInfo.name!!,
                handle = handle,
                bio = "",
                pictureBase64 = null,
                pubkeySig = ""
            )
            val profileResult = ChatManager.newProfile(profilePrototype)
            if(profileResult.isErr())
                return err(profileResult.unwrapErr())
            val profile = profileResult.unwrap()

            val chatResult = ChatManager.newChat(
                id = initRequest.id!!,
                idStamp = LibraryConnector.mGetCurrentTimestamp().unwrap().timestamp!!,
                idSalt = initRequest.id_salt!!,
                name = handleInfo.name,
                type = ChatType.SENT_INIT,
                ownKyber = Keypair(publicKey = initRequest.own_pubkey_kyber!!, privateKey = initRequest.own_seckey_kyber!!),
                ownCurve = Keypair(publicKey = initRequest.own_pubkey_curve!!, privateKey = initRequest.own_seckey_curve!!),
                ownPFS = initRequest.own_pfs_key!!,
                remotePFS = initRequest.remote_pfs_key!!,
                pfsSalt = initRequest.pfs_salt!!,
                mdcSeed = initRequest.mdc_seed!!,
                associatedProfileId = profile.dataId
            )
            if(chatResult.isErr())
                return err(chatResult.unwrapErr())
            val chat = chatResult.unwrap()

            val initMessage = Message(
                id = 0U,
                chatDataId = chat.dataId,
                sender = ChatManager.getProfile(Default.ProfileSelfDataId).unwrap(),
                sent = System.currentTimeMillis() / 1000,
                received = null,
                contentType = ContentType.SENT_INIT,
                text = comment,
                media = Base64.decode(initRequest.ciphertext!!, Base64.NO_WRAP),
            )

            chat.messages.add(initMessage)
            ChatManager.updateChat(chat)
        }

        return ok(Ok)
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun startTor() {
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(mTorReceiver, IntentFilter(TorService.ACTION_STATUS),
                RECEIVER_NOT_EXPORTED
            )
        }
        else {
            registerReceiver(mTorReceiver, IntentFilter(TorService.ACTION_STATUS))
        }

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

    private fun deriveReferrer(handleCiphertext: ByteArray): String {
        return LibraryConnector.mHash(handleCiphertext).unwrap().hash!!.slice(0..15)
    }
}