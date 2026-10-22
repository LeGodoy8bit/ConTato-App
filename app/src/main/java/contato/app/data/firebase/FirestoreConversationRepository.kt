package contato.app.data.firebase

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import contato.app.data.PreferencesConversationArchive
import contato.app.domain.model.CloudProfile
import contato.app.domain.model.Conversation
import contato.app.domain.model.Message
import contato.app.domain.model.UserMode
import contato.app.domain.repository.CloudConversationRepository

/**
 * Conversas no Cloud Firestore. Modelo (cada conta só enxerga a própria pasta, ver firestore.rules):
 *
 * users/{uid}/modes/{MODO}                                     displayName, updatedAt
 * users/{uid}/modes/{MODO}/conversations/{conversationId}      peerName, lastText, updatedAt, lastReadAt
 * users/{uid}/modes/{MODO}/conversations/{id}/messages/{id}    text, fromMe, sentAt, status
 *
 * {MODO} = COMMUNICATOR ou DEAFBLIND. Separado por modo porque a pessoa de apoio pode usar a
 * mesma conta Google nos dois celulares: cada um guarda a sua cópia sem misturar com a do outro.
 *
 * - Campos mínimos (LGPD): só o texto, quem mandou, horário e status. Nada de localização ou contatos.
 * - "fromMe" em vez do modo: o mesmo modelo serve para o Comunicador e para o Surdocego.
 * - O Firestore guarda escritas offline e envia quando a internet volta.
 */
class FirestoreConversationRepository(
    private val localMode: UserMode,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : CloudConversationRepository {

    private fun modeDoc(uid: String) = db.collection(USERS).document(uid).collection(MODES).document(localMode.name)
    private fun conversations(uid: String) = modeDoc(uid).collection(CONVERSATIONS)

    override fun fetchAll(uid: String, onResult: (Result<List<Conversation>>) -> Unit) {
        conversations(uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(MAX_CONVERSATIONS)
            .get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                if (docs.isEmpty()) {
                    onResult(Result.success(emptyList()))
                    return@addOnSuccessListener
                }
                val reads: List<Task<QuerySnapshot>> = docs.map { doc ->
                    doc.reference.collection(MESSAGES)
                        .orderBy("sentAt")
                        .limitToLast(Conversation.MAX_MESSAGES.toLong())
                        .get()
                }
                Tasks.whenAllSuccess<QuerySnapshot>(reads)
                    .addOnSuccessListener { messageSnapshots ->
                        val result = docs.zip(messageSnapshots).mapNotNull { (doc, messages) ->
                            conversationFrom(doc, messages.documents.mapNotNull(::messageFrom))
                        }
                        onResult(Result.success(result))
                    }
                    .addOnFailureListener { onResult(Result.failure(it)) }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun saveConversation(uid: String, conversation: Conversation, changed: List<Message>) {
        val batch = db.batch()
        val ref = conversations(uid).document(conversation.id)
        batch.set(
            ref,
            mapOf(
                "peerName" to conversation.peerName.take(MAX_NAME),
                "lastText" to (conversation.lastMessage?.content ?: "").take(MAX_TEXT),
                "updatedAt" to conversation.updatedAt,
                "lastReadAt" to conversation.lastReadAt,
            ),
        )
        changed.take(MAX_BATCH).forEach { message ->
            batch.set(
                ref.collection(MESSAGES).document(message.id),
                mapOf(
                    "text" to message.content.take(MAX_TEXT),
                    "fromMe" to (message.sender == localMode),
                    "sentAt" to message.timestamp,
                    "status" to message.status.name,
                ),
            )
        }
        batch.commit() // falhas ficam na fila offline do Firestore; nada a fazer na tela
    }

    override fun saveProfile(uid: String, profile: CloudProfile) {
        modeDoc(uid).set(
            mapOf(
                "displayName" to profile.displayName.take(MAX_NAME),
                "prefs" to profile.preferences.entries.take(MAX_PREFS).associate { it.key.take(32) to it.value.take(32) },
                "updatedAt" to System.currentTimeMillis(),
            ),
        )
    }

    override fun fetchProfile(uid: String, onResult: (Result<CloudProfile?>) -> Unit) {
        modeDoc(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(Result.success(null))
                    return@addOnSuccessListener
                }
                val prefs = (doc.get("prefs") as? Map<*, *>)
                    ?.mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }
                    ?.toMap()
                    .orEmpty()
                onResult(Result.success(CloudProfile(doc.getString("displayName") ?: "", prefs)))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun deleteAll(uid: String, onDone: (Result<Unit>) -> Unit) {
        // Source.SERVER: sem internet, falha (e a tela avisa) em vez de apagar só o que está em cache.
        conversations(uid).get(Source.SERVER)
            .continueWithTask { task ->
                val docs = task.result.documents
                Tasks.whenAllSuccess<QuerySnapshot>(docs.map { it.reference.collection(MESSAGES).get(Source.SERVER) })
                    .continueWithTask { messagesTask ->
                        val refs = messagesTask.result.flatMap { it.documents.map { d -> d.reference } } +
                                docs.map { it.reference } + modeDoc(uid)
                        // Um lote aceita até 500 operações.
                        Tasks.whenAll(refs.chunked(MAX_BATCH).map { chunk ->
                            db.batch().apply { chunk.forEach { delete(it) } }.commit()
                        })
                    }
            }
            .addOnSuccessListener { onDone(Result.success(Unit)) }
            .addOnFailureListener { onDone(Result.failure(it)) }
    }

    private fun conversationFrom(doc: DocumentSnapshot, messages: List<Message>): Conversation? {
        val peer = doc.getString("peerName") ?: return null
        return Conversation(
            id = doc.id,
            peerName = peer,
            messages = messages,
            updatedAt = doc.getLong("updatedAt") ?: 0L,
            lastReadAt = doc.getLong("lastReadAt") ?: 0L,
        )
    }

    private fun messageFrom(doc: DocumentSnapshot): Message? {
        val text = doc.getString("text") ?: return null
        val fromMe = doc.getBoolean("fromMe") ?: return null
        return Message(
            id = doc.id,
            content = text,
            sender = if (fromMe) localMode else localMode.other(),
            timestamp = doc.getLong("sentAt") ?: 0L,
            status = PreferencesConversationArchive.restoredStatus(doc.getString("status") ?: ""),
        )
    }

    private fun UserMode.other() = if (this == UserMode.COMMUNICATOR) UserMode.DEAFBLIND else UserMode.COMMUNICATOR

    companion object {
        private const val USERS = "users"
        private const val MODES = "modes"
        private const val CONVERSATIONS = "conversations"
        private const val MESSAGES = "messages"
        private const val MAX_CONVERSATIONS = 50L
        private const val MAX_BATCH = 450
        private const val MAX_NAME = 64
        private const val MAX_TEXT = 200
        private const val MAX_PREFS = 20
    }
}
