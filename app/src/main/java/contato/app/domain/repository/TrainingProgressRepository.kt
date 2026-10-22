package contato.app.domain.repository

/** Quantos itens de cada módulo já foram concluídos, e se a introdução do treino já foi vista. */
interface TrainingProgressRepository {
    fun completed(moduleId: String): Int
    fun setCompleted(moduleId: String, count: Int)
    fun introSeen(): Boolean
    fun setIntroSeen()
}
