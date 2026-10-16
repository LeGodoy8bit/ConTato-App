package contato.app.presentation.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import contato.app.R

// Arquivo só de desenvolvimento: serve para ver o tema, a fonte Inter e o logo
// no painel de Preview do Android Studio, sem precisar rodar o app.

@Preview(name = "Tema claro", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ThemePreviewLight() {
    PreviewContent(darkTheme = false, logo = R.drawable.logo_contato_light)
}

@Preview(name = "Tema escuro", showBackground = true, backgroundColor = 0xFF1E2838)
@Composable
private fun ThemePreviewDark() {
    PreviewContent(darkTheme = true, logo = R.drawable.logo_contato_dark)
}

@Composable
private fun PreviewContent(darkTheme: Boolean, @DrawableRes logo: Int) {
    ContatoTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Conectando",
                    style = SplashTitleStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "pelo toque",
                    style = SplashTitleStyle,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Texto de corpo em Inter",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Image(
                    painter = painterResource(logo),
                    contentDescription = "Logotipo ConTato",
                )
            }
        }
    }
}
