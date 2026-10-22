package contato.app.domain.repository

import contato.app.domain.model.CloudProfile
import contato.app.domain.model.Conversation
import contato.app.domain.model.Message

/**
 * Conversas na nuvem de uma conta. A implementação (Firestore) fica em data/firebase;
 * o domínio não conhece o Firebase. Tudo assíncrono por callback (sem bibliotecas extras).
 */
interface CloudConversationRepository {
    fun fetchAll(uid: String, onResult: (Result<List<Conversation>>) -> Unit)

    /** Grava o cabeçalho da conversa e as mensagens que mudaram. */
    fun saveConversation(uid: String, conversation: Conversation, changed: List<Message>)

    /** Nome e preferências deste modo (substitui o que havia). */
    fun saveProfile(uid: String, profile: CloudProfile)

    /** null = ainda não há perfil deste modo na nuvem (primeiro login). */
    fun fetchProfile(uid: String, onResult: (Result<CloudProfile?>) -> Unit)

    /** Apaga as conversas e o perfil deste modo (Comunicador ou Surdocego) na nuvem. */
    fun deleteAll(uid: String, onDone: (Result<Unit>) -> Unit)
}
