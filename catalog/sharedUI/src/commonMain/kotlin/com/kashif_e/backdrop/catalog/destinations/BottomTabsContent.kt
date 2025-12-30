package com.kashif_e.backdrop.catalog.destinations

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.catalog.BackdropDemoScaffold
import com.kashif_e.backdrop.catalog.Block
import com.kashif_e.backdrop.catalog.components.LiquidBottomTab
import com.kashif_e.backdrop.catalog.components.LiquidBottomTabs
import kmpliquidglass.catalog.sharedui.generated.resources.Res
import kmpliquidglass.catalog.sharedui.generated.resources.ic_cyclone
import org.jetbrains.compose.resources.painterResource

@Composable
fun BottomTabsContent(onBack: () -> Unit = {}) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White

    val airplaneModeIcon = painterResource(Res.drawable.ic_cyclone)
    val iconColorFilter = ColorFilter.tint(contentColor)

    BackdropDemoScaffold(
        onBack = onBack
    ) { backdrop ->
        Column(verticalArrangement = Arrangement.spacedBy(32f.dp)) {
            Block {
                var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

                LiquidBottomTabs(
                    selectedTabIndex = { selectedTabIndex },
                    onTabSelected = { selectedTabIndex = it },
                    backdrop = backdrop,
                    tabsCount = 3,
                    modifier = Modifier.padding(horizontal = 36f.dp)
                ) {
                    repeat(3) { index ->
                        LiquidBottomTab({ selectedTabIndex = index }) {
                            Box(
                                Modifier
                                    .size(28f.dp)
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            )
                            BasicText(
                                "Tab ${index + 1}",
                                style = TextStyle(contentColor, 12f.sp)
                            )
                        }
                    }
                }
            }
            Block {
                var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

                LiquidBottomTabs(
                    selectedTabIndex = { selectedTabIndex },
                    onTabSelected = { selectedTabIndex = it },
                    backdrop = backdrop,
                    tabsCount = 4,
                    modifier = Modifier.padding(horizontal = 36f.dp)
                ) {
                    repeat(4) { index ->
                        LiquidBottomTab({ selectedTabIndex = index }) {
                            Box(
                                Modifier
                                    .size(28f.dp)
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            )
                            BasicText(
                                "Tab ${index + 1}",
                                style = TextStyle(contentColor, 12f.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
