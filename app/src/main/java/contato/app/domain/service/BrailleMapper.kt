package contato.app.domain.service
import contato.app.domain.model.BrailleCharacter

//Objeto  para criar uma estancia compartilhada

object BrailleMapper {

    val brailleTable = mapOf(
        'A' to listOf(true,false,false,false,false,false), //100000
        'B' to listOf(true,true,false,false,false,false),  //110000
        'C' to listOf(true,false,false,true,false,false),  //100100
        'D' to listOf(true,false,false,true,true,false),   //100110
        'E' to listOf(true,false,false,false,true,false),  //100010
        'F' to listOf(true,true,false,true,false,false),   //110100
        'G' to listOf(true,true,false,true,true,false),    //110110
        'H' to listOf(true,true,false,false,true,false),   //110010
        'I' to listOf(false,true,false,true,false,false),  //010100
        'J' to listOf(false,true,false,true,true,false),   //010110
        'K' to listOf(true,false,true,false,false,false),  //101000
        'L' to listOf(true,true,true,false,false,false),   //111000
        'M' to listOf(true,false,true,true,false,false),   //101100
        'N' to listOf(true,false,true,true,true,false),    //101110
        'O' to listOf(true,false,true,false,true,false),   //101010
        'P' to listOf(true,true,true,true,false,false),    //111100
        'Q' to listOf(true,true,true,true,true,false),     //111110
        'R' to listOf(true,true,true,false,true,false),    //111010
        'S' to listOf(false,true,true,true,false,false),   //011100
        'T' to listOf(false,true,true,true,true,false),    //011110
        'U' to listOf(true,false,true,false,false,true),   //101001
        'V' to listOf(true,true,true,false,false,true),    //111001
        'W' to listOf(false,true,false,true,true,true),    //010111
        'X' to listOf(true,false,true,true,false,true),    //101101
        'Y' to listOf(true,false,true,true,true,true),     //101111
        'Z' to listOf(true,false,true,false,true,true),    //101011
    )

    fun map (character: Char): BrailleCharacter {

        val points = brailleTable[character]
        return BrailleCharacter (
            character = character,
            points = points ?: throw IllegalArgumentException(
                "Caractere não suportado: $character"
            )
        )

    }

    /**
     * Caminho inverso do mapeamento: dado um padrão de pontos pressionados
     * na célula Braille virtual, retorna o caractere correspondente.
     *
     * Usado pela composição de mensagens do Surdocego (seção 3.8 da
     * especificação): o usuário pressiona os pontos, e o sistema precisa
     * descobrir qual letra isso representa — o oposto do que `map()` faz.
     *
     * @param points os 6 pontos pressionados, na ordem P1..P6.
     * @return o caractere correspondente, ou null se o padrão não
     *         corresponder a nenhum caractere conhecido.
     */
    fun reverseMap(points: List<Boolean>): Char? {
        return brailleTable.entries.find { it.value == points }?.key
    }


}