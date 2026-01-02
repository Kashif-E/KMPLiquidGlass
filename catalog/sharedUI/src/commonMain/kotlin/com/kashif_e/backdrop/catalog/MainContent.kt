package com.kashif_e.backdrop.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kashif_e.backdrop.catalog.destinations.AdaptiveLuminanceGlassContent
import com.kashif_e.backdrop.catalog.destinations.BottomTabsContent
import com.kashif_e.backdrop.catalog.destinations.ButtonsContent
import com.kashif_e.backdrop.catalog.destinations.ControlCenterContent
import com.kashif_e.backdrop.catalog.destinations.DialogContent
import com.kashif_e.backdrop.catalog.destinations.GlassPlaygroundContent
import com.kashif_e.backdrop.catalog.destinations.HomeContent
import com.kashif_e.backdrop.catalog.destinations.AllComponentsContent
import com.kashif_e.backdrop.catalog.destinations.LazyScrollContainerContent
import com.kashif_e.backdrop.catalog.destinations.LockScreenContent
import com.kashif_e.backdrop.catalog.destinations.MagnifierContent
import com.kashif_e.backdrop.catalog.destinations.ProgressiveBlurContent
import com.kashif_e.backdrop.catalog.destinations.ScrollContainerContent
import com.kashif_e.backdrop.catalog.destinations.SliderContent
import com.kashif_e.backdrop.catalog.destinations.ToggleContent

@Composable
fun MainContent() {
    var destination by rememberSaveable { mutableStateOf(CatalogDestination.Home) }

    when (destination) {
        CatalogDestination.Home -> HomeContent(onNavigate = { destination = it })

        CatalogDestination.Buttons -> ButtonsContent(onBack = { destination = CatalogDestination.Home })
    CatalogDestination.AllComponents -> AllComponentsContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.Toggle -> ToggleContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.Slider -> SliderContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.BottomTabs -> BottomTabsContent(onBack = { destination = CatalogDestination.Home })
        CatalogDestination.Dialog -> DialogContent(onBack = { destination = CatalogDestination.Home })

        CatalogDestination.ControlCenter -> {
            ControlCenterContent(onBack = { destination = CatalogDestination.Home })
        }

        CatalogDestination.LockScreen -> {
            LockScreenContent(onBack = { destination = CatalogDestination.Home })
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
