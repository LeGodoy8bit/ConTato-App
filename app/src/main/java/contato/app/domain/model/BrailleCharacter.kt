package contato.app.domain.model

//atributos para cada caractere inserido

data class BrailleCharacter(
    val character : Char, //somente uma letra
    val points : List<Boolean>

) //lista dos ponto ativos(true) e inativos (false)

