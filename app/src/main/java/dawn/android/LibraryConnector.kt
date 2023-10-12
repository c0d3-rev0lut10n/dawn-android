package dawn.android

import dawn.android.data.Result
import dawn.android.data.Result.Companion.ok
import dawn.android.data.Result.Companion.err
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
data class KyberKeys(
    val status: String,
    val own_pubkey_kyber: String? = null,
    val own_seckey_kyber: String? = null
)

@Serializable
data class CurveKeys(
    val status: String,
    val own_pubkey_curve: String? = null,
    val own_seckey_curve: String? = null
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
data class Hash(
    val status: String,
    val hash: String?
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

@Serializable
data class Timestamp(
    val status: String,
    val timestamp: String? = null
)

@Serializable
data class MultiTimestamp(
    val status: String,
    val timestamps: Array<String>? = null
)

@Serializable
data class GenHandle(
    val status: String,
    val handle: String? = null
)

@Serializable
data class ParseHandle(
    val status: String,
    val init_pk_kyber: String? = null,
    val init_pk_curve: String? = null,
    val init_pk_curve_pfs_2: String? = null,
    val init_pk_kyber_for_salt: String? = null,
    val init_pk_curve_for_salt: String? = null,
    val name: String? = null
)

@Serializable
data class GenInitRequest(
    val status: String,
    val own_pubkey_kyber: String? = null,
    val own_seckey_kyber: String? = null,
    val own_pubkey_curve: String? = null,
    val own_seckey_curve: String? = null,
    val own_pfs_key: String? = null,
    val remote_pfs_key: String? = null,
    val pfs_salt: String?= null,
    val id: String? = null,
    val id_salt: String? = null,
    val mdc: String? = null,
    val mdc_seed: String? = null,
    val ciphertext: String? = null
)

@Serializable
data class ParseInitRequest(
    val status: String,
    val id: String?,
    val id_salt: String?,
    val mdc: String?,
    val remote_pubkey_kyber: String?,
    val remote_pubkey_sig: String?,
    val own_pfs_key: String?,
    val remote_pfs_key: String?,
    val pfs_salt: String?,
    val name: String?,
    val comment: String?,
    val mdc_seed: String? = null
)

object LibraryConnector {

    init {
        System.loadLibrary("dawn")
    }

    fun mInitCrypto(): Result<InitCrypto, String> {
        val libraryResponse: InitCrypto = Json.decodeFromString(initCrypto())
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mKyberKeygen(): Result<KyberKeys, String> {
        val libraryResponse: KyberKeys = Json.decodeFromString(kyberKeygen())
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mCurveKeygen(): Result<CurveKeys, String> {
        val libraryResponse: CurveKeys = Json.decodeFromString(curveKeygen())
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mSignKeygen(): Result<SignKeys, String> {
        val libraryResponse: SignKeys = Json.decodeFromString(signKeygen())
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mSymKeygen(): Result<SymKey, String> {
        val libraryResponse: SymKey = Json.decodeFromString(symKeygen())
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mGenId(): Result<GenId, String> {
        val libraryResponse: GenId = Json.decodeFromString(genId())
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mGetTempId(id: String): Result<TempId, String> {
        val libraryResponse: TempId = Json.decodeFromString(getTempId(id))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mGetCustomTempId(id: String, modifier: String): Result<TempId, String> {
        val libraryResponse: TempId = Json.decodeFromString(getCustomTempId(id, modifier))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mGetNextId(id: String, salt: String): Result<NextId, String> {
        val libraryResponse: NextId = Json.decodeFromString(getNextId(id, salt))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mDeriveSecurityNumber(key_a: String, key_b: String): Result<SecurityNumber, String> {
        val libraryResponse: SecurityNumber = Json.decodeFromString(deriveSecurityNumber(key_a, key_b))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mHash(input: String): Result<Hash, String> {
        val libraryResponse: Hash = Json.decodeFromString(hashString(input))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mHash(input: ByteArray): Result<Hash, String> {
        val libraryResponse: Hash = Json.decodeFromString(hashBytes(input))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mSendMsg(msg_type: Short, msg_string: String, msg_bytes: ByteArray, remote_pubkey_kyber: String, own_pubkey_sig: String, pfs_key: String, pfs_salt: String, id: String, mdc_seed: String):
            Result<SendMessage, String> {
        val libraryResponse: SendMessage = Json.decodeFromString(sendMsg(msg_type, msg_string, msg_bytes, remote_pubkey_kyber, own_pubkey_sig, pfs_key, pfs_salt, id, mdc_seed))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mParseMsg(msg_ciphertext: ByteArray, own_seckey_kyber: String, remote_pubkey_sig: String, pfs_key: String, pfs_salt: String):
            Result<ParseMessage, String> {
        val libraryResponse: ParseMessage = Json.decodeFromString(parseMsg(msg_ciphertext, own_seckey_kyber, remote_pubkey_sig, pfs_key, pfs_salt))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mEncryptFile(file: ByteArray): Result<EncryptFile, String> {
        val libraryResponse: EncryptFile = Json.decodeFromString(encryptFile(file))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mDecryptFile(ciphertext: ByteArray, key: String): Result<DecryptFile, String> {
        val libraryResponse: DecryptFile = Json.decodeFromString(decryptFile(ciphertext, key))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mGetCurrentTimestamp(): Result<Timestamp, String> {
        val libraryResponse: Timestamp = Json.decodeFromString(getCurrentTimestamp())
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mGetAllTimestampsSince(timestamp: String): Result<MultiTimestamp, String> {
        val libraryResponse: MultiTimestamp = Json.decodeFromString(getAllTimestampsSince(timestamp))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mGenHandle(init_pubkey_kyber: String, init_pubkey_curve: String, init_pubkey_kyber_for_salt: String, init_pubkey_curve_for_salt: String, name: String): Result<GenHandle, String> {
        val libraryResponse: GenHandle = Json.decodeFromString(genHandle(init_pubkey_kyber, init_pubkey_curve, init_pubkey_kyber_for_salt, init_pubkey_curve_for_salt, name))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mParseHandle(handle: ByteArray): Result<ParseHandle, String> {
        val libraryResponse: ParseHandle = Json.decodeFromString(parseHandle(handle))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mGenInitRequest(remote_pubkey_kyber: String, remote_pubkey_kyber_for_salt: String, remote_pubkey_curve: String, remote_pubkey_curve_pfs_2: String, remote_pubkey_curve_for_salt: String, own_pubkey_sig: String, own_seckey_sig: String, name: String, comment: String):
            Result<GenInitRequest, String> {
        val libraryResponse: GenInitRequest = Json.decodeFromString(genInitRequest(remote_pubkey_kyber, remote_pubkey_kyber_for_salt, remote_pubkey_curve, remote_pubkey_curve_pfs_2, remote_pubkey_curve_for_salt, own_pubkey_sig, own_seckey_sig, name, comment))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    fun mParseInitRequest(ciphertext: ByteArray, own_seckey_kyber: String, own_seckey_curve: String, own_seckey_curve_pfs_2: String, own_seckey_kyber_for_salt: String, own_seckey_curve_for_salt: String):
            Result<ParseInitRequest, String> {
        val libraryResponse: ParseInitRequest = Json.decodeFromString(parseInitRequest(ciphertext, own_seckey_kyber, own_seckey_curve, own_seckey_curve_pfs_2, own_seckey_kyber_for_salt, own_seckey_curve_for_salt))
        if(libraryResponse.status != "ok") return err(libraryResponse.status)
        return ok(libraryResponse)
    }

    private external fun initCrypto(): String
    private external fun kyberKeygen(): String
    private external fun curveKeygen(): String
    private external fun signKeygen(): String
    private external fun symKeygen(): String
    private external fun genId(): String
    private external fun getTempId(id: String): String
    private external fun getCustomTempId(id: String, modifier: String): String
    private external fun getNextId(id: String, salt: String): String
    private external fun deriveSecurityNumber(key_a: String, key_b: String): String
    private external fun hashString(input: String): String
    private external fun hashBytes(input: ByteArray): String
    private external fun sendMsg(msg_type: Short, msg_string: String, msg_bytes: ByteArray, remote_pubkey_kyber: String, own_seckey_sig: String, pfs_key: String, pfs_salt: String, id: String, mdc_seed: String): String
    private external fun parseMsg(msg_ciphertext: ByteArray, own_seckey_kyber: String, remote_pubkey_sig: String, pfs_key: String, pfs_salt: String): String
    private external fun encryptFile(file: ByteArray): String
    private external fun decryptFile(ciphertext: ByteArray, key: String): String
    private external fun getCurrentTimestamp(): String
    private external fun getAllTimestampsSince(timestamp: String): String
    private external fun genHandle(init_pubkey_kyber: String, init_pubkey_curve: String, init_pubkey_kyber_for_salt: String, init_pubkey_curve_for_salt: String, name: String): String
    private external fun parseHandle(handle: ByteArray): String
    private external fun genInitRequest(remote_pubkey_kyber: String, remote_pubkey_kyber_for_salt: String, remote_pubkey_curve: String, remote_pubkey_curve_pfs_2: String, remote_pubkey_curve_for_salt: String, own_pubkey_sig: String, own_seckey_sig: String, name: String, comment: String): String
    private external fun parseInitRequest(ciphertext: ByteArray, own_seckey_kyber: String, own_seckey_curve: String, own_seckey_curve_pfs_2: String, own_seckey_kyber_for_salt: String, own_seckey_curve_for_salt: String): String
}