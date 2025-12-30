import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.catalog.BackdropDemoScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Backdrop Catalog - Desktop",
        state = rememberWindowState(width = 400.dp, height = 800.dp)
    ) {
        BackdropDemoScreen()
    }
}
