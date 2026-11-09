package contato.app.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Ilustrações do ConTato (res/drawable/ill_*.xml): pessoas e cenas no estilo "flat", nas cores
 * do app. São decorativas — o texto ao lado já diz tudo —, então o leitor de tela as ignora.
 */
@Composable
internal fun Illustration(@DrawableRes res: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
