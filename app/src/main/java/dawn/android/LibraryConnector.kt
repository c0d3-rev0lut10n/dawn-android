package dawn.android

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class InitCrypto(
    val status: String,
    val id: String? = null,
    val own_pubkey_kyber: String? = null,
    val own_seckey_kyber: String? = null,
    val own_pubkey_curve: String? = null,
    val own_seckey_curve: String? = null,
    val own_pubkey_kyber_for_salt: String? = null,
    val own_seckey_kyber_for_salt: String? = null,
    val own_pubkey_curve_for_salt: String? = null,
    val own_seckey_curve_for_salt: String? = null
)

@Serializable
data class SignKeys(
    val status: String,
    val own_pubkey_sig: String? = null,
    val own_seckey_sig: String? = null
)

@Serializable
data class SymKey(
    val status: String,
    val key: String? = null
)

@Serializable
data class GenId(
    val status: String,
    val id: String? = null
)

@Serializable
data class TempId(
    val status: String,
    val id: String? = null
)

@Serializable
data class NextId(
    val status: String,
    val id: String? = null
)

@Serializable
data class SecurityNumber(
    val status: String,
    val number: String? = null
)

@Serializable
data class SendMessage(
    val status: String,
    val new_pfs_key: String? = null,
    val mdc: String? = null,
    val ciphertext: String? = null
)

@Serializable
data class ParseMessage(
    val status: String,
    val msg_type: Short? = null,
    val msg_text: String? = null,
    val msg_bytes: String? = null,
    val new_pfs_key: String? = null,
    val mdc: String? = null
)

@Serializable
data class EncryptFile(
    val status: String,
    val key: String? = null,
    val ciphertext: String? = null
)

@Serializable
data class DecryptFile(
    val status: String,
    val file: String
)

object LibraryConnector {

    init {
        System.loadLibrary("dawn")
    }

    fun mInitCrypto(): InitCrypto {
        val libraryResponseJSON = initCrypto()
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mSignKeygen(): SignKeys {
        val libraryResponseJSON = signKeygen()
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mSymKeygen(): SymKey {
        val libraryResponseJSON = symKeygen()
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mGenId(): GenId {
        val libraryResponseJSON = genId()
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mGetTempId(id: String): TempId {
        val libraryResponseJSON = getTempId(id)
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mGetCustomTempId(id: String, modifier: String): TempId {
        val libraryResponseJSON = getCustomTempId(id, modifier)
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mGetNextId(id: String, salt: String): NextId {
        val libraryResponseJSON = getNextId(id, salt)
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mDeriveSecurityNumber(key_a: String, key_b: String): SecurityNumber {
        val libraryResponseJSON = deriveSecurityNumber(key_a, key_b)
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mSendMsg(msg_type: Short, msg_string: String, msg_bytes: ByteArray, remote_pubkey_kyber: String, own_pubkey_sig: String, pfs_key: String, pfs_salt: String): SendMessage {
        val libraryResponseJSON = sendMsg(msg_type, msg_string, msg_bytes, remote_pubkey_kyber, own_pubkey_sig, pfs_key, pfs_salt)
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mParseMsg(msg_ciphertext: ByteArray, own_seckey_kyber: String, remote_pubkey_sig: String, pfs_key: String, pfs_salt: String): ParseMessage {
        val libraryResponseJSON = parseMsg(msg_ciphertext, own_seckey_kyber, remote_pubkey_sig, pfs_key, pfs_salt)
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mEncryptFile(file: ByteArray): EncryptFile {
        val libraryResponseJSON = encryptFile(file)
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mDecryptFile(ciphertext: ByteArray, key: String): DecryptFile {
        val libraryResponseJSON = decryptFile(ciphertext, key)
        return Json.decodeFromString(libraryResponseJSON)
    }

    private external fun initCrypto(): String
    private external fun signKeygen(): String
    private external fun symKeygen(): String
    private external fun genId(): String
    private external fun getTempId(id: String): String
    private external fun getCustomTempId(id: String, modifier: String): String
    private external fun getNextId(id: String, salt: String): String
    private external fun deriveSecurityNumber(key_a: String, key_b: String): String
    private external fun sendMsg(msg_type: Short, msg_string: String, msg_bytes: ByteArray, remote_pubkey_kyber: String, own_seckey_sig: String, pfs_key: String, pfs_salt: String): String
    private external fun parseMsg(msg_ciphertext: ByteArray, own_seckey_kyber: String, remote_pubkey_sig: String, pfs_key: String, pfs_salt: String): String
    private external fun encryptFile(file: ByteArray): String
    private external fun decryptFile(ciphertext: ByteArray, key: String): String
}