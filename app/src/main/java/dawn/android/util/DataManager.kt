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

package dawn.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import dawn.android.data.Location
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
    private lateinit var encryptionKeySpec: SecretKeySpec
    private lateinit var salt: ByteArray
    private lateinit var dataDirectory: File
    private lateinit var messagesDirectory: File
    private var initialized = false
    private var initializing = false
    private lateinit var logTag: String

    fun init(context: Context, password: String): Boolean {
        return init(context, password, true)
    }
    fun init(context: Context, password: String, initPreferences: Boolean): Boolean {
        if (initializing) return false
        else initializing = true
        if (initialized) return true
        mContext = context.applicationContext
        logTag = this.javaClass.name
        dataDirectory = mContext.filesDir
        messagesDirectory = File(dataDirectory, "messages")

        val keyFile = File(dataDirectory, "key")
        val saltFile = File(dataDirectory, "salt")
        val testFile = File(dataDirectory, "check")

        try {
            // check if files exist
            if (!isStorageInitialized(context)) return false
            val mFileInputStream = FileInputStream(saltFile)
            salt = mFileInputStream.readBytes()
            mFileInputStream.close()

            val passwordChars = password.toCharArray()
            Log.i(logTag, "Starting decryption of app data")
            val pbeKeySpec = PBEKeySpec(passwordChars, salt, 100000, 256)
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val passwordDerivedKey = secretKeyFactory.generateSecret(pbeKeySpec).encoded
            Log.i(logTag, "Finished decryption of app data")
            val passwordDerivedKeySpec = SecretKeySpec(passwordDerivedKey, "AES")

            val keyFileInputStream = FileInputStream(keyFile)
            val keyBytesInputStream = BufferedInputStream(keyFileInputStream)
            val keyFileBytes = keyBytesInputStream.readBytes()
            keyBytesInputStream.close()
            keyFileInputStream.close()

            if (keyFileBytes.size <= 16) return false

            val keyFileIv = keyFileBytes.copyOfRange(0, 16)
            val encryptedKey = keyFileBytes.copyOfRange(16, keyFileBytes.size)

            val keyDecryptionCipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val keyFileIvSpec = IvParameterSpec(keyFileIv)
            keyDecryptionCipher.init(Cipher.DECRYPT_MODE, passwordDerivedKeySpec, keyFileIvSpec)
            val key = keyDecryptionCipher.doFinal(encryptedKey)
            encryptionKeySpec = SecretKeySpec(key, "AES")

            // check the key using the test file
            val testFileInputStream = FileInputStream(testFile)
            val testBytesInputStream = BufferedInputStream(testFileInputStream)
            val testFileBytes = testBytesInputStream.readBytes()
            testBytesInputStream.close()
            testFileInputStream.close()

            if (testFileBytes.size <= 16) return false

            val testFileIv = testFileBytes.copyOfRange(0, 16)
            val testFileEncryptedContent = testFileBytes.copyOfRange(16, testFileBytes.size)

            val testFileDecryptionCipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val testFileIvSpec = IvParameterSpec(testFileIv)
            testFileDecryptionCipher.init(Cipher.DECRYPT_MODE, encryptionKeySpec, testFileIvSpec)
            val testFileContent = testFileDecryptionCipher.doFinal(testFileEncryptedContent)

            if (testFileContent.size <= 64) return false

            val testData = testFileContent.copyOfRange(0, 64)
            val encodedHash = testFileContent.copyOfRange(64, testFileContent.size)

            val digest = MessageDigest.getInstance("SHA-256")
            val derivedEncodedHash = digest.digest(testData)

            if (!encodedHash.contentEquals(derivedEncodedHash)) return false

            initialized = true
            if(initPreferences)
                PreferenceManager.init(mContext.filesDir)
            return true
        } catch (e: Exception) {
            Log.e(context.packageName, "decryption of the app data failed!", e)
            initializing = false
            return false
        }
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    fun writeFile(name: String, path: File, content: ByteArray, overwrite: Boolean): Boolean {
        if(!initialized) return false
        if(!path.isDirectory) return false
        val file = File(path, name)
        if(file.isFile && !overwrite) return false

        val fileIv = ByteArray(16)
        SecureRandom().nextBytes(fileIv)
        val fileIvSpec = IvParameterSpec(fileIv)

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKeySpec, fileIvSpec)
        val encryptedContent = cipher.doFinal(content)
        val fileContent = fileIv + encryptedContent
        val fileOutputStream = FileOutputStream(file, false)
        fileOutputStream.write(fileContent)
        fileOutputStream.close()

        return true
    }

    fun readFile(name: String, path: File): ByteArray? {
        if (!initialized) return null
        if (!path.isDirectory) return null
        val file = File(path, name)
        if (!file.isFile) return null

        val fileInputStream = FileInputStream(file)
        val fileBytesStream = BufferedInputStream(fileInputStream)
        val fileBytes = fileBytesStream.readBytes()
        fileBytesStream.close()
        fileInputStream.close()
        if (fileBytes.size <= 16) return null

        val fileIv = fileBytes.copyOfRange(0, 16)
        val fileEncryptedContent = fileBytes.copyOfRange(16, fileBytes.size)

        val fileDecryptionCipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val fileIvSpec = IvParameterSpec(fileIv)
        fileDecryptionCipher.init(Cipher.DECRYPT_MODE, encryptionKeySpec, fileIvSpec)

        return fileDecryptionCipher.doFinal(fileEncryptedContent)
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
        val encryptionKey = ByteArray(32)
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
        val keyFileContent = keyFileIv + encryptedKey
        val keyFileOutputStream = FileOutputStream(keyFile, false)
        keyFileOutputStream.write(keyFileContent)
        keyFileOutputStream.close()

        val testData = ByteArray(64)
        SecureRandom().nextBytes(testData)

        val digest = MessageDigest.getInstance("SHA-256")
        val encodedHash = digest.digest(testData)

        val testDataContentToEncrypt = testData + encodedHash

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKeySpec, testFileIvSpec)

        val encrypted = cipher.doFinal(testDataContentToEncrypt)

        val encryptedTestFileContent = testFileIv + encrypted

        val mEncryptedFileOutputStream = FileOutputStream(testFile, false)
        mEncryptedFileOutputStream.write(encryptedTestFileContent)
        mEncryptedFileOutputStream.close()

        return true
    }

    fun getLocation(loc: Location): File {
        return when(loc) {
            Location.ROOT -> mContext.filesDir
            Location.CHATS -> File(mContext.filesDir, "chats")
            Location.PROFILES -> File(mContext.filesDir, "profiles")
        }
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