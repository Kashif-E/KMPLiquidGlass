package com.kashif_e.backdrop.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.backdrops.LayerBackdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.catalog.components.LiquidButton
import com.kashif_e.backdrop.catalog.utils.toImageBitmap
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.mohamedrejeb.calf.core.LocalPlatformContext
import kmpliquidglass.catalog.sharedui.generated.resources.Res
import kmpliquidglass.catalog.sharedui.generated.resources.lions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun BackdropDemoScaffold(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        var selectedImageBitmap: ImageBitmap? by remember { mutableStateOf(null) }
        val scope = rememberCoroutineScope()
        val context = LocalPlatformContext.current

        val backdrop = rememberLayerBackdrop()

        val pickerLauncher = rememberFilePickerLauncher(
            type = FilePickerFileType.Image,
            selectionMode = FilePickerSelectionMode.Single,
            onResult = { files ->
                scope.launch {
                    files.firstOrNull()?.let { file ->
                        val byteArray = file.readByteArray(context)
                        selectedImageBitmap = byteArray.toImageBitmap()
                    }
                }
            }
        )

        if (selectedImageBitmap != null) {
            Image(
                selectedImageBitmap!!,
                contentDescription = null,
                Modifier
                    .layerBackdrop(backdrop)
                    .then(modifier)
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painterResource(Res.drawable.lions),
                contentDescription = null,
                Modifier
                    .layerBackdrop(backdrop)
                    .then(modifier)
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        content(backdrop)


        if (onBack != null) {
            LiquidButton(
                onBack,
                backdrop,
                Modifier.clip(CircleShape)
                    .padding(16f.dp)
                    .align(Alignment.TopStart),
                tint = Color(0xFF0088FF),
            ) {
                BasicText(
                    "Back",
                    Modifier.padding(horizontal = 16f.dp, vertical = 12f.dp),
                    style = TextStyle(Color.White, 16f.sp)
                )
            }
        }

        LiquidButton(
            { pickerLauncher.launch() },
            backdrop,
            Modifier
                .padding(16f.dp)
                .navigationBarsPadding()
                .height(56f.dp)
                .align(Alignment.BottomCenter),
            tint = Color(0xFF0088FF)
        ) {
            BasicText(
                "Pick an image",
                Modifier.padding(horizontal = 8f.dp),
                style = TextStyle(Color.White, 16f.sp)
            )
        }
    }
}
