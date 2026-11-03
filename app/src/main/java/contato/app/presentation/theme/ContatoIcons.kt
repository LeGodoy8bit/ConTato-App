package contato.app.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ícones do ConTato: traço arredondado (24 × 24, traço 2), no lugar dos ícones padrão do Android.
 * Desenhos baseados no Lucide (lucide.dev, licença ISC — uso livre, mantendo este aviso).
 * Ficam aqui dentro para não precisar de biblioteca nova (e não arriscar a versão do Kotlin).
 * A cor vem do `tint` do Icon, como nos ícones do Material.
 */
object ContatoIcons {

    val Check: ImageVector by lazy { stroke("Check", "M20 6 9 17 l-5 -5") }
    val ChevronRight: ImageVector by lazy { stroke("ChevronRight", "m9 18 6 -6 -6 -6") }
    val ChevronLeft: ImageVector by lazy { stroke("ChevronLeft", "m15 18 -6 -6 6 -6") }
    val ChevronUp: ImageVector by lazy { stroke("ChevronUp", "m18 15 -6 -6 -6 6") }
    val ChevronDown: ImageVector by lazy { stroke("ChevronDown", "m6 9 6 6 6 -6") }
    val Close: ImageVector by lazy { stroke("Close", "M18 6 6 18", "m6 6 12 12") }
    val Sparkles: ImageVector by lazy { stroke("Sparkles", "M9.937 15.5 A2 2 0 0 0 8.5 14.063 l-6.135 -1.582 a.5 .5 0 0 1 0 -.962 L8.5 9.936 A2 2 0 0 0 9.937 8.5 l1.582 -6.135 a.5 .5 0 0 1 .963 0 L14.063 8.5 A2 2 0 0 0 15.5 9.937 l6.135 1.581 a.5 .5 0 0 1 0 .964 L15.5 14.063 a2 2 0 0 0 -1.437 1.437 l-1.582 6.135 a.5 .5 0 0 1 -.963 0 z", "M20 3 v4", "M22 5 h-4", "M4 17 v2", "M5 18 H3") }
    val Settings: ImageVector by lazy { stroke("Settings", "M12.22 2 h-.44 a2 2 0 0 0 -2 2 v.18 a2 2 0 0 1 -1 1.73 l-.43 .25 a2 2 0 0 1 -2 0 l-.15 -.08 a2 2 0 0 0 -2.73 .73 l-.22 .38 a2 2 0 0 0 .73 2.73 l.15 .1 a2 2 0 0 1 1 1.72 v.51 a2 2 0 0 1 -1 1.74 l-.15 .09 a2 2 0 0 0 -.73 2.73 l.22 .38 a2 2 0 0 0 2.73 .73 l.15 -.08 a2 2 0 0 1 2 0 l.43 .25 a2 2 0 0 1 1 1.73 V20 a2 2 0 0 0 2 2 h.44 a2 2 0 0 0 2 -2 v-.18 a2 2 0 0 1 1 -1.73 l.43 -.25 a2 2 0 0 1 2 0 l.15 .08 a2 2 0 0 0 2.73 -.73 l.22 -.39 a2 2 0 0 0 -.73 -2.73 l-.15 -.08 a2 2 0 0 1 -1 -1.74 v-.5 a2 2 0 0 1 1 -1.74 l.15 -.09 a2 2 0 0 0 .73 -2.73 l-.22 -.38 a2 2 0 0 0 -2.73 -.73 l-.15 .08 a2 2 0 0 1 -2 0 l-.43 -.25 a2 2 0 0 1 -1 -1.73 V4 a2 2 0 0 0 -2 -2 z", "M9 12 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0") }
    val Lock: ImageVector by lazy { stroke("Lock", "M5 11 h14 a2 2 0 0 1 2 2 v7 a2 2 0 0 1 -2 2 h-14 a2 2 0 0 1 -2 -2 v-7 a2 2 0 0 1 2 -2 z", "M7 11 V7 a5 5 0 0 1 10 0 v4") }
    val Info: ImageVector by lazy { stroke("Info", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0", "M12 16 v-4", "M12 8 h.01") }
    val Plus: ImageVector by lazy { stroke("Plus", "M5 12 h14", "M12 5 v14") }
    val LogOut: ImageVector by lazy { stroke("LogOut", "M9 21 H5 a2 2 0 0 1 -2 -2 V5 a2 2 0 0 1 2 -2 h4", "m16 17 5 -5 -5 -5", "M21 12 H9") }
    val Warning: ImageVector by lazy { stroke("Warning", "m21.73 18 -8 -14 a2 2 0 0 0 -3.48 0 l-8 14 A2 2 0 0 0 4 21 h16 a2 2 0 0 0 1.73 -3", "M12 9 v4", "M12 17 h.01") }
    val Refresh: ImageVector by lazy { stroke("Refresh", "M3 12 a9 9 0 0 1 9 -9 9.75 9.75 0 0 1 6.74 2.74 L21 8", "M21 3 v5 h-5", "M21 12 a9 9 0 0 1 -9 9 9.75 9.75 0 0 1 -6.74 -2.74 L3 16", "M8 16 H3 v5") }
    val Smartphone: ImageVector by lazy { stroke("Smartphone", "M7 2 h10 a2 2 0 0 1 2 2 v16 a2 2 0 0 1 -2 2 h-10 a2 2 0 0 1 -2 -2 v-16 a2 2 0 0 1 2 -2 z", "M12 18 h.01") }
    val Edit: ImageVector by lazy { stroke("Edit", "M21.174 6.812 a1 1 0 0 0 -3.986 -3.987 L3.842 16.174 a2 2 0 0 0 -.5 .83 l-1.321 4.352 a.5 .5 0 0 0 .623 .622 l4.353 -1.32 a2 2 0 0 0 .83 -.497 z", "m15 5 4 4") }
    val Trash: ImageVector by lazy { stroke("Trash", "M3 6 h18", "M19 6 v14 c0 1 -1 2 -2 2 H7 c-1 0 -2 -1 -2 -2 V6", "M8 6 V4 c0 -1 1 -2 2 -2 h4 c1 0 2 1 2 2 v2", "M10 11 v6", "M14 11 v6") }
    val Send: ImageVector by lazy { stroke("Send", "M14.536 21.686 a.5 .5 0 0 0 .937 -.024 l6.5 -19 a.496 .496 0 0 0 -.635 -.635 l-19 6.5 a.5 .5 0 0 0 -.024 .937 l7.93 3.18 a2 2 0 0 1 1.112 1.11 z", "m21.854 2.147 -10.94 10.939") }
    val Search: ImageVector by lazy { stroke("Search", "M3 11 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0", "m21 21 -4.3 -4.3") }
    val User: ImageVector by lazy { stroke("User", "M19 21 v-2 a4 4 0 0 0 -4 -4 H9 a4 4 0 0 0 -4 4 v2", "M8 7 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0") }
    val Menu: ImageVector by lazy { stroke("Menu", "M4 12 h16", "M4 6 h16", "M4 18 h16") }
    val MapPin: ImageVector by lazy { stroke("MapPin", "M20 10 c0 4.993 -5.539 10.193 -7.399 11.799 a1 1 0 0 1 -1.202 0 C9.539 20.193 4 14.993 4 10 a8 8 0 0 1 16 0", "M9 10 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0") }
    val UserCircle: ImageVector by lazy { stroke("UserCircle", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0", "M9 10 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0", "M7 20.662 V19 a2 2 0 0 1 2 -2 h6 a2 2 0 0 1 2 2 v1.662") }
    val ArrowRight: ImageVector by lazy { stroke("ArrowRight", "M5 12 h14", "m12 5 7 7 -7 7") }
    val ArrowLeft: ImageVector by lazy { stroke("ArrowLeft", "m12 19 -7 -7 7 -7", "M19 12 H5") }
    val ChatBubble: ImageVector by lazy { stroke("ChatBubble", "M7.9 20 A9 9 0 1 0 4 16.1 L2 22 Z") }
    val Chat: ImageVector by lazy { stroke("Chat", "M7.9 20 A9 9 0 1 0 4 16.1 L2 22 Z", "M8 12 h.01", "M12 12 h.01", "M16 12 h.01") }
    val Vibration: ImageVector by lazy { stroke("Vibration", "m2 8 2 2 -2 2 2 2 -2 2", "m22 8 -2 2 2 2 -2 2 2 2", "M9 5 h6 a1 1 0 0 1 1 1 v12 a1 1 0 0 1 -1 1 h-6 a1 1 0 0 1 -1 -1 v-12 a1 1 0 0 1 1 -1 z") }
    val School: ImageVector by lazy { stroke("School", "M21.42 10.922 a1 1 0 0 0 -.019 -1.838 L12.83 5.18 a2 2 0 0 0 -1.66 0 L2.6 9.08 a1 1 0 0 0 0 1.832 l8.57 3.908 a2 2 0 0 0 1.66 0 z", "M22 10 v6", "M6 12.5 V16 a6 3 0 0 0 12 0 v-3.5") }
    val Cloud: ImageVector by lazy { stroke("Cloud", "M17.5 19 H9 a7 7 0 1 1 6.71 -9 h1.79 a4.5 4.5 0 1 1 0 9 Z") }
    val TouchApp: ImageVector by lazy { stroke("TouchApp", "M22 14 a8 8 0 0 1 -8 8", "M18 11 v-1 a2 2 0 0 0 -2 -2 a2 2 0 0 0 -2 2", "M14 10 V9 a2 2 0 0 0 -2 -2 a2 2 0 0 0 -2 2 v1", "M10 9.5 V4 a2 2 0 0 0 -2 -2 a2 2 0 0 0 -2 2 v10", "M18 11 a2 2 0 1 1 4 0 v3 a8 8 0 0 1 -8 8 h-2 c-2.8 0 -4.5 -.86 -5.99 -2.34 l-3.6 -3.6 a2 2 0 0 1 2.83 -2.82 L7 15") }
    val Sensors: ImageVector by lazy { stroke("Sensors", "M4.9 19.1 C1 15.2 1 8.8 4.9 4.9", "M7.8 16.2 c-2.3 -2.3 -2.3 -6.1 0 -8.5", "M10 12 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0", "M16.2 7.8 c2.3 2.3 2.3 6.1 0 8.5", "M19.1 4.9 C23 8.8 23 15.1 19.1 19") }
    val DoneAll: ImageVector by lazy { stroke("DoneAll", "M18 6 7 17 l-5 -5", "m22 10 -7.5 7.5 L13 16") }
    val PhotoCamera: ImageVector by lazy { stroke("PhotoCamera", "M14.5 4 h-5 L7 7 H4 a2 2 0 0 0 -2 2 v9 a2 2 0 0 0 2 2 h16 a2 2 0 0 0 2 -2 V9 a2 2 0 0 0 -2 -2 h-3 l-2.5 -3 z", "M9 13 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0") }
    val Clock: ImageVector by lazy { stroke("Clock", "M2 12 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0", "M12 6 v6 l4 2") }
    val Link: ImageVector by lazy { stroke("Link", "M10 13 a5 5 0 0 0 7.54 .54 l3 -3 a5 5 0 0 0 -7.07 -7.07 l-1.72 1.71", "M14 11 a5 5 0 0 0 -7.54 -.54 l-3 3 a5 5 0 0 0 7.07 7.07 l1.71 -1.71") }
    val Bluetooth: ImageVector by lazy { stroke("Bluetooth", "m7 7 10 10 -5 5 V2 l5 5 L7 17") }
    val Heart: ImageVector by lazy { stroke("Heart", "M19 14 c1.49 -1.46 3 -3.21 3 -5.5 A5.5 5.5 0 0 0 16.5 3 c-1.76 0 -3 .5 -4.5 2 -1.5 -1.5 -2.74 -2 -4.5 -2 A5.5 5.5 0 0 0 2 8.5 c0 2.3 1.5 4.05 3 5.5 l7 7 Z") }
    val Shield: ImageVector by lazy { stroke("Shield", "M20 13 c0 5 -3.5 7.5 -7.66 8.95 a1 1 0 0 1 -.67 -.01 C7.5 20.5 4 18 4 13 V6 a1 1 0 0 1 1 -1 c2 0 4.5 -1.2 6.24 -2.72 a1.17 1.17 0 0 1 1.52 0 C14.51 3.81 17 5 19 5 a1 1 0 0 1 1 1 z", "m9 12 2 2 4 -4") }
    val Unlink: ImageVector by lazy { stroke("Unlink", "m18.84 12.25 1.72 -1.71 h-.02 a5.004 5.004 0 0 0 -.12 -7.07 5.006 5.006 0 0 0 -6.95 0 l-1.72 1.71", "m5.17 11.75 -1.71 1.71 a5.004 5.004 0 0 0 .12 7.07 5.006 5.006 0 0 0 6.95 0 l1.71 -1.71", "M8 2 v3", "M2 8 h3", "M16 22 v-3", "M22 16 h-3") }

    private fun stroke(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
