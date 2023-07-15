package dawn.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// since we use the application context, this is NOT a memory leak, so SHUT UP
@SuppressLint("StaticFieldLeak")
object DataManager {

    private lateinit var mContext: Context
    private lateinit var mSecretKeySpec: SecretKeySpec
    private lateinit var salt: ByteArray
    private lateinit var dataDirectory: File
    private lateinit var messagesDirectory: File
    private var initialized = false
    private var initializing = false

    fun init(context: Context, password: String): Boolean {
        if(initializing) return false
        else initializing = true
        mContext = context.applicationContext
        dataDirectory = mContext.filesDir
        messagesDirectory = File(dataDirectory, "messages")

        val saltFile = File(dataDirectory, "salt")
        val testFile = File(dataDirectory, "check")

        // check if files exist
        if(!saltFile.isFile || !testFile.isFile) return false
        val mFileInputStream = FileInputStream(saltFile)
        salt = mFileInputStream.readBytes()
        mFileInputStream.close()

        //println(Base64.encodeToString(salt, Base64.NO_WRAP))

        val passwordChars = password.toCharArray()
        println(java.time.LocalTime.now())
        val pbeKeySpec = PBEKeySpec(passwordChars, salt, 100000, 256)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = secretKeyFactory.generateSecret(pbeKeySpec).encoded
        println(java.time.LocalTime.now())
        mSecretKeySpec = SecretKeySpec(key, "AES")

        // test whether the key actually works


        initialized = true
        return true
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    fun isStorageInitialized(context: Context): Boolean {
        dataDirectory = context.filesDir
        // check if salt and test file exist
        val saltFile = File(dataDirectory, "salt")
        val testFile = File(dataDirectory, "check")
        return saltFile.isFile && testFile.isFile
    }

    fun initializeStorage(context: Context, password: String, force: Boolean): Boolean {
        dataDirectory = context.filesDir
        val saltFile = File(dataDirectory, "salt")
        val testFile = File(dataDirectory, "check")

        // if files exist and force option is not present, return, if force is set to true, cleanly initialize the directory once again
        if(saltFile.isFile || testFile.isFile) {
            if(force) {
                deleteRecursive(dataDirectory)
            }
            else {
                return false
            }
        }

        salt = ByteArray(256)
        SecureRandom().nextBytes(salt)
        val mFileOutputStream = FileOutputStream(saltFile, false)
        mFileOutputStream.write(salt)
        mFileOutputStream.close()



        return true
    }

    private fun deleteRecursive(directory: File) {
        val files = directory.listFiles()
        if(files == null) return
        for (file in files) {
            if (file.isDirectory) deleteRecursive(file)
            file.delete()
        }
    }
}