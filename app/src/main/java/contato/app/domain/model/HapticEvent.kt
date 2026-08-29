package contato.app.domain.model

data class HapticEvent(
    val type : HapticEventType,
    val duration : Long, //duração evento (ex: 100 ms)
    val amplitude: Int // intensidade das vibrações
)
