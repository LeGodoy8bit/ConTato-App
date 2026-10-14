
package contato.app.presentation.onboarding

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val ContatoBlue = Color(0xFF006FEE)
private val ContatoNavy = Color(0xFF202A3A)
private val ContatoWhite = Color.White

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current

    var message by remember { mutableStateOf("") }
    var stage by remember { mutableIntStateOf(0) }

    val circleScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {

        suspend fun typeText(text: String) {
            message = ""

            text.forEach { character ->
                message += character

                // Um pulso por caractere digitado.
                vibrateOnce(context)

                delay(120)
            }
        }

        suspend fun eraseText() {
            while (message.isNotEmpty()) {
                message = message.dropLast(1)
                delay(65)
            }
        }

        // 1. Conectando
        stage = 0
        typeText("Conectando")
        delay(350)
        eraseText()

        // 2. Pelo toque
        stage = 1
        typeText("pelo toque")
        delay(450)
        eraseText()

        // 3. Tecnologia
        stage = 2
        typeText("Tecnologia")
        delay(500)

        // 4. Símbolo completo
        stage = 3
        message = ""
        delay(700)

        // 5. Círculo de transição
        stage = 4

        circleScale.animateTo(
            targetValue = 16f,
            animationSpec = tween(
                durationMillis = 550,
                easing = FastOutSlowInEasing
            )
        )

        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ContatoWhite),
        contentAlignment = Alignment.Center
    ) {
        if (stage < 4) {
            Column(
                modifier = Modifier.wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (stage) {
                    0 -> {
                        TwoDots()

                        Spacer(Modifier.height(5.dp))

                        SplashLabel(
                            text = message,
                            color = ContatoNavy
                        )
                    }

                    1 -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SplashLabel(
                                text = message,
                                color = ContatoBlue
                            )

                            Spacer(Modifier.width(5.dp))

                            Box(
                                Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(ContatoBlue)
                            )
                        }
                    }

                    2 -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(5.dp)
                        ) {
                            SplashLabel(
                                text = message,
                                color = ContatoNavy
                            )

                            Box(
                                Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .background(ContatoBlue)
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        DotSymbol()
                    }

                    3 -> DotSymbol()
                }
            }

            if (stage >= 2) {
                ContatoWordmark(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }

        if (stage >= 4) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(circleScale.value)
                    .clip(CircleShape)
                    .background(ContatoBlue)
            )
        }
    }
}

@Composable
private fun SplashLabel(
    text: String,
    color: Color
) {
    Text(
        text = text,
        color = color,
        fontSize = 32.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun TwoDots() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(2) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(ContatoNavy)
            )
        }
    }
}

@Composable
private fun DotSymbol() {
    Box(Modifier.size(44.dp)) {
        Dot(
            ContatoBlue,
            Modifier.offset(x = 27.dp, y = 0.dp)
        )

        Dot(
            ContatoNavy,
            Modifier.offset(x = 0.dp, y = 18.dp)
        )

        Dot(
            ContatoNavy,
            Modifier.offset(x = 18.dp, y = 18.dp)
        )

        Dot(
            ContatoBlue,
            Modifier.offset(x = 9.dp, y = 36.dp)
        )
    }
}

@Composable
private fun Dot(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun ContatoWordmark(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = ContatoNavy,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("Con")
                }

                withStyle(
                    SpanStyle(
                        color = ContatoBlue,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("T")
                }

                withStyle(
                    SpanStyle(
                        color = ContatoNavy,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("ato")
                }
            },
            fontSize = 14.sp,
            lineHeight = 16.sp
        )

        Text(
            text = "Comunicação pelo toque",
            color = ContatoNavy,
            fontSize = 4.sp,
            lineHeight = 6.sp
        )
    }
}

@Suppress("DEPRECATION")
private fun vibrateOnce(context: Context) {
    val vibrator = context.getSystemService(
        Context.VIBRATOR_SERVICE
    ) as? Vibrator ?: return

    if (!vibrator.hasVibrator()) return

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    20L,
                    80
                )
            )
        } else {
            vibrator.vibrate(20L)
        }
    } catch (_: SecurityException) {
        // A animação continua mesmo sem vibração.
    }
}

