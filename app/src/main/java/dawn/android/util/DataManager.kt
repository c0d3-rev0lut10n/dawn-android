package dawn.android.util

import android.annotation.SuppressLint
import android.content.Context
import java.io.*
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
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
        val keyFile = File(dataDirectory, "key")
        val saltFile = File(dataDirectory, "salt")
        val testFile = File(dataDirectory, "check")
        return keyFile.isFile && saltFile.isFile && testFile.isFile
    }

    fun initializeStorage(context: Context, password: String, force: Boolean): Boolean {
        dataDirectory = context.filesDir
        val keyFile = File(dataDirectory, "key")
        val saltFile = File(dataDirectory, "salt")
        val testFile = File(dataDirectory, "check")

        // if files exist and force option is not present, return, if force is set to true, cleanly initialize the directory once again
        if(keyFile.isFile || saltFile.isFile || testFile.isFile) {
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

        val passwordChars = password.toCharArray()
        val pbeKeySpec = PBEKeySpec(passwordChars, salt, 100000, 256)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val passwordDerivedKey = secretKeyFactory.generateSecret(pbeKeySpec).encoded
        val passwordDerivedKeySpec = SecretKeySpec(passwordDerivedKey, "AES")

        val testFileIv = ByteArray(16)
        SecureRandom().nextBytes(testFileIv)
        val testFileIvSpec = IvParameterSpec(testFileIv)

        // generate a random AES encryption key
        val encryptionKey = ByteArray(64)
        SecureRandom().nextBytes(encryptionKey)
        val encryptionKeySpec = SecretKeySpec(encryptionKey, "AES")

        // generate an IV for the key file
        val keyFileIv = ByteArray(16)
        SecureRandom().nextBytes(keyFileIv)
        val keyFileIvSpec = IvParameterSpec(keyFileIv)

        // encrypt the key using the password-derived key and save it
        val keyCipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        keyCipher.init(Cipher.ENCRYPT_MODE, passwordDerivedKeySpec, keyFileIvSpec)
        val encryptedKey = keyCipher.doFinal(encryptionKey)
        val keyHashMap = HashMap<String, ByteArray>()
        keyHashMap["iv"] = keyFileIv
        keyHashMap["encryptedKey"] = encryptedKey
        val keyFileOutputStream = FileOutputStream(keyFile, false)
        val keyObjectOutputStream = ObjectOutputStream(keyFileOutputStream)
        keyObjectOutputStream.writeObject(keyHashMap)
        keyObjectOutputStream.close()
        keyFileOutputStream.close()

        val testData = ByteArray(64)
        SecureRandom().nextBytes(testData)

        val digest = MessageDigest.getInstance("SHA-256")
        val encodedHash = digest.digest(testData)

        val testDataHashMap = HashMap<String, ByteArray>()
        testDataHashMap["data"] = testData
        testDataHashMap["hash"] = encodedHash

        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(testDataHashMap)
        val dataToEncrypt = byteArrayOutputStream.toByteArray()
        objectOutputStream.close()
        byteArrayOutputStream.close()

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKeySpec, testFileIvSpec)

        val encrypted = cipher.doFinal(dataToEncrypt)

        val encryptedDataHashMap = HashMap<String, ByteArray>()
        encryptedDataHashMap["iv"] = testFileIv
        encryptedDataHashMap["encrypted"] = encrypted

        val mEncryptedFileOutputStream = FileOutputStream(testFile, false)
        val mObjectOutputStream = ObjectOutputStream(mEncryptedFileOutputStream)
        mObjectOutputStream.writeObject(encryptedDataHashMap)
        mObjectOutputStream.close()
        mEncryptedFileOutputStream.close()

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