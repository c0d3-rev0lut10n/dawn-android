package dawn.android

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class InitCrypto(
    val status: String,
    val id: String? = null
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
data class TempId(
    val status: String,
    val id: String? = null,
    val own_pubkey_kyber: String? = null,
    val own_seckey_kyber: String? = null,
    val own_pubkey_curve: String? = null,
    val own_seckey_curve: String? = null
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

    fun mGetTempId(id: String): TempId {
        val libraryResponseJSON = getTempId(id)
        return Json.decodeFromString(libraryResponseJSON)
    }

    private external fun initCrypto(): String
    private external fun signKeygen(): String
    private external fun symKeygen(): String
    private external fun getTempId(id: String): String
}