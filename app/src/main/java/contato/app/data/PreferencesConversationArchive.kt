package contato.app.data

import android.content.Context
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.MessageType
import contato.app.domain.model.UserMode
import contato.app.domain.repository.ConversationArchive
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda as últimas mensagens no próprio aparelho (JSON em SharedPreferences).
 * Só é usado quando o Comunicador liga "Guardar conversas neste aparelho".
 * org.json já vem com o Android: nenhuma dependência nova.
 */
class PreferencesConversationArchive(context: Context) : ConversationArchive {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): List<Message> {
        val raw = preferences.getString(KEY_MESSAGES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i -> fromJson(array.getJSONObject(i)) }
        }.getOrDefault(emptyList())
    }

    override fun save(messages: List<Message>) {
        val array = JSONArray()
        messages.takeLast(MAX_MESSAGES).forEach { array.put(toJson(it)) }
        preferences.edit().putString(KEY_MESSAGES, array.toString()).apply()
    }

    override fun clear() {
        preferences.edit().remove(KEY_MESSAGES).apply()
    }

    private fun toJson(message: Message) = JSONObject()
        .put("id", message.id)
        .put("content", message.content)
        .put("sender", message.sender.name)
        .put("timestamp", message.timestamp)
        .put("status", message.status.name)
        .put("type", message.type.name)

    private fun fromJson(json: JSONObject) = Message(
        id = json.getString("id"),
        content = json.getString("content"),
        sender = UserMode.valueOf(json.getString("sender")),
        timestamp = json.getLong("timestamp"),
        // Mensagens que ficaram "no meio do caminho" quando o app fechou não voltam como em andamento.
        status = when (val s = MessageStatus.valueOf(json.getString("status"))) {
            MessageStatus.SENDING, MessageStatus.SENT -> MessageStatus.FAILED
            MessageStatus.PLAYING -> MessageStatus.DELIVERED
            else -> s
        },
        type = runCatching { MessageType.valueOf(json.getString("type")) }.getOrDefault(MessageType.TEXT),
    )

    companion object {
        private const val PREFERENCES_NAME = "contato_history"
        private const val KEY_MESSAGES = "messages"
        private const val MAX_MESSAGES = 200
    }
}
