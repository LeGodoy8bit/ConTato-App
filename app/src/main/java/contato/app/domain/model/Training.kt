package contato.app.domain.model

/** Um módulo do treino (Letras, Palavras, Frases). Cada item é sentido e depois respondido na cela. */
data class TrainingModule(
    val id: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    val items: List<String>,
)
