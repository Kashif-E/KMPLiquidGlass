package com.kyant.backdrop.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.kyant.backdrop.catalog.destinations.AdaptiveLuminanceGlassContent
import com.kyant.backdrop.catalog.destinations.BottomTabsContent
import com.kyant.backdrop.catalog.destinations.ButtonsContent
import com.kyant.backdrop.catalog.destinations.ControlCenterContent
import com.kyant.backdrop.catalog.destinations.DialogContent
import com.kyant.backdrop.catalog.destinations.GlassPlaygroundContent
import com.kyant.backdrop.catalog.destinations.HomeContent
import com.kyant.backdrop.catalog.destinations.LazyScrollContainerContent
import com.kyant.backdrop.catalog.destinations.MagnifierContent
import com.kyant.backdrop.catalog.destinations.ProgressiveBlurContent
import com.kyant.backdrop.catalog.destinations.ScrollContainerContent
import com.kyant.backdrop.catalog.destinations.SliderContent
import com.kyant.backdrop.catalog.destinations.ToggleContent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainContent() {
    var destination by rememberSaveable { mutableStateOf(CatalogDestination.Home) }

    BackHandler(destination != CatalogDestination.Home) {
        destination = CatalogDestination.Home
    }

    when (destination) {
        CatalogDestination.Home -> HomeContent(onNavigate = { destination = it })

        CatalogDestination.Buttons -> ButtonsContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.Toggle -> ToggleContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.Slider -> SliderContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.BottomTabs -> BottomTabsContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.Dialog -> DialogContent(onBack = { destination = CatalogDestination.Home })

        CatalogDestination.ControlCenter -> {
            ControlCenterContent(onBack = { destination = CatalogDestination.Home })
        }

        CatalogDestination.Magnifier -> MagnifierContent(onBack = { destination = CatalogDestination.Home })

        CatalogDestination.GlassPlayground -> GlassPlaygroundContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.AdaptiveLuminanceGlass -> {
            AdaptiveLuminanceGlassContent(onBack = { destination = CatalogDestination.Home })
        }

        CatalogDestination.ProgressiveBlur -> ProgressiveBlurContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.ScrollContainer -> ScrollContainerContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.LazyScrollContainer -> LazyScrollContainerContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.Playground -> ShaderPlayGround()
    }
}
