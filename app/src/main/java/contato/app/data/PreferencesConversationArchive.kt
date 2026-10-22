package contato.app.data

import android.content.Context
import contato.app.domain.model.Conversation
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.MessageType
import contato.app.domain.model.UserMode
import contato.app.domain.repository.ConversationArchive
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Guarda as conversas no próprio aparelho (JSON em SharedPreferences; org.json já vem no Android).
 *
 * Cada modo usa um arquivo próprio ([preferencesName]), para o Comunicador e o Surdocego
 * no mesmo celular (testes) não misturarem históricos.
 *
 * Migração: a versão anterior guardava uma lista solta de mensagens na chave "messages".
 * Ela vira uma conversa com "Celular tátil" na primeira leitura e a chave antiga é apagada.
 */
class PreferencesConversationArchive(
    context: Context,
    preferencesName: String = COMMUNICATOR_FILE,
) : ConversationArchive {

    private val preferences =
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun loadAll(): List<Conversation> {
        migrateLegacy()
        val raw = preferences.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i -> conversationFromJson(array.getJSONObject(i)) }
        }.getOrDefault(emptyList())
    }

    override fun saveAll(conversations: List<Conversation>) {
        val array = JSONArray()
        conversations.forEach { array.put(conversationToJson(it)) }
        preferences.edit().putString(KEY_CONVERSATIONS, array.toString()).apply()
    }

    override fun clear() {
        preferences.edit().remove(KEY_CONVERSATIONS).remove(KEY_LEGACY_MESSAGES).apply()
    }

    private fun migrateLegacy() {
        val legacy = preferences.getString(KEY_LEGACY_MESSAGES, null) ?: return
        val messages = runCatching {
            val array = JSONArray(legacy)
            List(array.length()) { i -> messageFromJson(array.getJSONObject(i)) }
        }.getOrDefault(emptyList())
        if (messages.isNotEmpty() && preferences.getString(KEY_CONVERSATIONS, null) == null) {
            saveAll(listOf(Conversation(UUID.randomUUID().toString(), LEGACY_PEER, messages,
                updatedAt = messages.maxOf { it.timestamp }, lastReadAt = messages.maxOf { it.timestamp })))
        }
        preferences.edit().remove(KEY_LEGACY_MESSAGES).apply()
    }

    private fun conversationToJson(c: Conversation): JSONObject {
        val messages = JSONArray()
        c.messages.takeLast(Conversation.MAX_MESSAGES).forEach { messages.put(messageToJson(it)) }
        return JSONObject()
            .put("id", c.id)
            .put("peerName", c.peerName)
            .put("updatedAt", c.updatedAt)
            .put("lastReadAt", c.lastReadAt)
            .put("messages", messages)
    }

    private fun conversationFromJson(json: JSONObject): Conversation {
        val array = json.optJSONArray("messages") ?: JSONArray()
        return Conversation(
            id = json.getString("id"),
            peerName = json.getString("peerName"),
            messages = List(array.length()) { i -> messageFromJson(array.getJSONObject(i)) },
            updatedAt = json.optLong("updatedAt"),
            lastReadAt = json.optLong("lastReadAt"),
        )
    }

    private fun messageToJson(message: Message) = JSONObject()
        .put("id", message.id)
        .put("content", message.content)
        .put("sender", message.sender.name)
        .put("timestamp", message.timestamp)
        .put("status", message.status.name)
        .put("type", message.type.name)

    private fun messageFromJson(json: JSONObject) = Message(
        id = json.getString("id"),
        content = json.getString("content"),
        sender = UserMode.valueOf(json.getString("sender")),
        timestamp = json.getLong("timestamp"),
        status = restoredStatus(json.getString("status")),
        type = runCatching { MessageType.valueOf(json.getString("type")) }.getOrDefault(MessageType.TEXT),
    )

    companion object {
        const val COMMUNICATOR_FILE = "contato_history"
        const val DEAFBLIND_FILE = "contato_history_deafblind"
        private const val KEY_CONVERSATIONS = "conversations"
        private const val KEY_LEGACY_MESSAGES = "messages"
        private const val LEGACY_PEER = "Celular tátil"

        /** Mensagens que ficaram "no meio do caminho" quando o app fechou não voltam como em andamento. */
        fun restoredStatus(name: String): MessageStatus =
            when (val s = runCatching { MessageStatus.valueOf(name) }.getOrDefault(MessageStatus.DELIVERED)) {
                MessageStatus.SENDING, MessageStatus.SENT -> MessageStatus.FAILED
                MessageStatus.PLAYING -> MessageStatus.DELIVERED
                else -> s
            }
    }
}
