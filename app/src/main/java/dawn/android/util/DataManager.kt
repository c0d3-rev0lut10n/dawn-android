package dawn.android.util

import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// since we use the application context, this is NOT a memory leak, so SHUT UP
@SuppressLint("StaticFieldLeak")
object DataManager {

    private lateinit var mContext: Context
    private lateinit var mSecretKeySpec: SecretKeySpec
    private lateinit var salt: ByteArray
    private val mSecureRandom = SecureRandom()
    private lateinit var dataDirectory: File
    private lateinit var messagesDirectory: File
    private var initialized = false
    private var initializing = false

    fun init(context: Context, password: String) {
        if(initializing) return
        else initializing = true
        mContext = context.applicationContext
        dataDirectory = mContext.filesDir
        messagesDirectory = File(dataDirectory, "messages")
        // check if salt exists
        val saltFile = File(dataDirectory, "salt")
        if(!saltFile.isFile) {
            salt = ByteArray(256)
            mSecureRandom.nextBytes(salt)
            val mFileOutputStream = FileOutputStream(saltFile, false)
            mFileOutputStream.write(salt)
            mFileOutputStream.close()
        }
        else {
            val mFileInputStream = FileInputStream(saltFile)
            salt = mFileInputStream.readBytes()
            mFileInputStream.close()
        }
        println(Base64.encodeToString(salt, Base64.NO_WRAP))

        val passwordChars = password.toCharArray()
        println(java.time.LocalTime.now())
        val pbeKeySpec = PBEKeySpec(passwordChars, salt, 100000, 256)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = secretKeyFactory.generateSecret(pbeKeySpec).encoded
        println(java.time.LocalTime.now())
        mSecretKeySpec = SecretKeySpec(key, "AES")

        initialized = true
    }

    fun isInitialized(): Boolean {
        return initialized
    }
}