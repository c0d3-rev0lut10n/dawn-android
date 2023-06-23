package dawn.android

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class InitCrypto(val status: String, val id: String? = null)

@Serializable
data class TempId(val status: String, val id: String? = null)

object LibraryConnector {

    init {
        System.loadLibrary("dawn")
    }

    fun mInitCrypto(): InitCrypto {
        val libraryResponseJSON = initCrypto()
        return Json.decodeFromString(libraryResponseJSON)
    }

    fun mGetTempId(id: String): TempId {
        val libraryResponseJSON = getTempId(id)
        return Json.decodeFromString(libraryResponseJSON)
    }

    private external fun initCrypto(): String
    private external fun getTempId(id: String): String
}