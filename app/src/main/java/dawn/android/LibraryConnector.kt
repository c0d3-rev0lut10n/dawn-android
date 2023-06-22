package dawn.android

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class TempId(val status: String, val id: String? = null)

object LibraryConnector {

    init {
        System.loadLibrary("dawn")
    }

    fun mGetTempId(id: String): String? {
        val libraryResponseJSON = getTempId(id)
        val libraryResponse = Json.decodeFromString<TempId>(libraryResponseJSON)
        if(libraryResponse.status != "ok") return null
        return libraryResponse.id
    }

    private external fun getTempId(id: String): String
}