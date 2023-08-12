package dawn.android.data

data class Chat(
    val dataId: String,
    var id: String,
    var idStamp: String,
    var idSalt: String,
    var lastMessageId: UShort,
    var name: String
)