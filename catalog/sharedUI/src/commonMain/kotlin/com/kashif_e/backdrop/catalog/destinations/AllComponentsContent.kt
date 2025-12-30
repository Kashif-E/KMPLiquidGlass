package com.kashif_e.backdrop.catalog.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.catalog.BackdropDemoScaffold
import com.kashif_e.backdrop.catalog.components.LiquidBottomTab
import com.kashif_e.backdrop.catalog.components.LiquidBottomTabs
import com.kashif_e.backdrop.catalog.components.LiquidButton
import com.kashif_e.backdrop.catalog.components.LiquidSlider
import com.kashif_e.backdrop.catalog.components.LiquidToggle
import kmpliquidglass.catalog.sharedui.generated.resources.Res
import kmpliquidglass.catalog.sharedui.generated.resources.ic_cyclone
import org.jetbrains.compose.resources.painterResource

@Composable
fun AllComponentsContent(onBack: () -> Unit = {}) {
    BackdropDemoScaffold(onBack = onBack) { backdrop ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                "All Components",
                style = TextStyle(Color.White, 28.sp, FontWeight.Medium),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ComponentSection(title = "Buttons") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiquidButton(
                        {},
                        backdrop
                    ) {
                        BasicText(
                            "Transparent Button",
                            style = TextStyle(Color.Black, 15.sp)
                        )
                    }
                    LiquidButton(
                        {},
                        backdrop,
                        surfaceColor = Color.White.copy(0.3f)
                    ) {
                        BasicText(
                            "Surface Button",
                            style = TextStyle(Color.Black, 15.sp)
                        )
                    }
                    LiquidButton(
                        {},
                        backdrop,
                        tint = Color(0xFF0088FF)
                    ) {
                        BasicText(
                            "Blue Tinted Button",
                            style = TextStyle(Color.White, 15.sp)
                        )
                    }
                }
            }


            ComponentSection(title = "Toggles") {
                var toggle1 by remember { mutableStateOf(false) }
                var toggle2 by remember { mutableStateOf(true) }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidToggle({ toggle1 }, { toggle1 = it }, backdrop)
                        BasicText("Toggle Off", style = TextStyle(Color.White, 14.sp))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidToggle({ toggle2 }, { toggle2 = it }, backdrop)
                        BasicText("Toggle On", style = TextStyle(Color.White, 14.sp))
                    }
                }
            }


            ComponentSection(title = "Slider") {
                var sliderValue by remember { mutableStateOf(0.5f) }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiquidSlider(
                        { sliderValue },
                        { sliderValue = it },
                        0f..1f,
                        0.01f,
                        backdrop,
                        Modifier.fillMaxWidth()
                    )
                    BasicText(
                        "Value: ${(sliderValue * 100).toInt()}%",
                        style = TextStyle(Color.White, 14.sp)
                    )
                }
            }

            ComponentSection(title = "Bottom Tabs") {
                var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
                val airplaneModeIcon = painterResource(Res.drawable.ic_cyclone)
                val iconColorFilter = ColorFilter.tint(Color.White)

                LiquidBottomTabs(
                    selectedTabIndex = { selectedTabIndex },
                    onTabSelected = { selectedTabIndex = it },
                    backdrop = backdrop,
                    tabsCount = 3
                ) {
                    repeat(3) { index ->
                        LiquidBottomTab({ selectedTabIndex = index }) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .paint(airplaneModeIcon, colorFilter = iconColorFilter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            title,
            style = TextStyle(Color(0xFF0088FF), 18.sp, FontWeight.Medium)
        )
        content()
    }
}
