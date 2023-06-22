package dawn.android

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class TempId(val status: String, val id: String? = null)

object LibraryConnector {

    init {
        System.loadLibrary("dawn")
    }

    fun mGetTempId(id: String): TempId {
        val libraryResponseJSON = getTempId(id)
        return Json.decodeFromString(libraryResponseJSON)
    }

    private external fun getTempId(id: String): String
}