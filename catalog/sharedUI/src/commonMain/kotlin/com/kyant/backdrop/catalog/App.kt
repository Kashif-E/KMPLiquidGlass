package com.kyant.backdrop.catalog


// Common Backdrop library imports (work on both Android and iOS)


// Common Backdrop library imports (work on both Android and iOS)

// Import the cross-platform LiquidBottomTabs components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.backdropdemo.components.LiquidBottomTab
import com.example.backdropdemo.components.LiquidBottomTabs
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.CanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.opacity
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

@Composable
fun BackdropDemoScreen() {
    MainContent()
}

@Composable
private fun MainContentX() {
    val gradientBackdrop = CanvasBackdrop {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF667eea),
                    Color(0xFF764ba2),
                    Color(0xFFf093fb),
                    Color(0xFFf5576c),
                    Color(0xFF4facfe),
                    Color(0xFF00f2fe)
                )
            )
        )
        // Add some circles for visual interest
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = 150f,
            center = Offset(100f, 200f)
        )
        drawCircle(
            color = Color.Yellow.copy(alpha = 0.4f),
            radius = 100f,
            center = Offset(300f, 400f)
        )
        drawCircle(
            color = Color.Cyan.copy(alpha = 0.3f),
            radius = 120f,
            center = Offset(250f, 150f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background layer with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = gradientBackdrop,
                    shape = { RoundedCornerShape(0.dp) },
                    effects = {},
                    shadow = null,
                    highlight = null
                )
        )

        // Scrollable content with demo cards
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoSection("Basic Blur") {
                BlurDemo(gradientBackdrop)
            }

            DemoSection("Blur with Saturation") {
                BlurSaturationDemo(gradientBackdrop)
            }

            DemoSection("Color Filter") {
                ColorFilterDemo(gradientBackdrop)
            }

            DemoSection("Lens Refraction") {
                LensDemo(gradientBackdrop)
            }

            DemoSection("Shadow Effects") {
                ShadowDemo(gradientBackdrop)
            }

            DemoSection("Inner Shadow") {
                InnerShadowDemo(gradientBackdrop)
            }

            DemoSection("Highlight Styles") {
                HighlightDemo(gradientBackdrop)
            }

            DemoSection("Combined Effects (Liquid Glass)") {
                CombinedEffectsDemo(gradientBackdrop)
            }

            DemoSection("Different Shapes") {
                ShapesDemo(gradientBackdrop)
            }

            DemoSection("Bottom Navigation Bar") {
                BottomNavBarDemo(gradientBackdrop)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DemoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun BlurDemo(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(8.dp.toPx()) },
            label = "Light"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            label = "Medium"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(32.dp.toPx()) },
            label = "Heavy"
        )
    }
}

@Composable
private fun BlurSaturationDemo(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(16.dp.toPx())
                colorControls(saturation = 0.5f)
            },
            label = "Desat"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(16.dp.toPx())
                colorControls(saturation = 1f)
            },
            label = "Normal"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(16.dp.toPx())
                colorControls(saturation = 2f)
            },
            label = "Vivid"
        )
    }
}

@Composable
private fun ColorFilterDemo(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(16.dp.toPx())
                // Cooler appearance with reduced brightness
                colorControls(brightness = -0.1f, saturation = 1.2f)
            },
            label = "Cool"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(16.dp.toPx())
                // Warmer appearance with increased brightness
                colorControls(brightness = 0.05f, contrast = 1.1f, saturation = 1.3f)
            },
            label = "Warm"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(16.dp.toPx())
                colorControls(saturation = 0f)
            },
            label = "Gray"
        )
    }
}

@Composable
private fun LensDemo(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(8.dp.toPx())
                lens(
                    refractionHeight = 16.dp.toPx(),
                    refractionAmount = 8.dp.toPx()
                )
            },
            label = "Subtle"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(8.dp.toPx())
                lens(
                    refractionHeight = 24.dp.toPx(),
                    refractionAmount = 16.dp.toPx(),
                    depthEffect = true
                )
            },
            label = "Strong"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(4.dp.toPx())
                lens(
                    refractionHeight = 20.dp.toPx(),
                    refractionAmount = 12.dp.toPx(),
                    chromaticAberration = true
                )
            },
            label = "Chroma"
        )
    }
}

@Composable
private fun ShadowDemo(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            shadow = {
                Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = 8.dp,
                    offset = DpOffset(2.dp, 4.dp)
                )
            },
            label = "Small"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            shadow = {
                Shadow(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = 16.dp,
                    offset = DpOffset(4.dp, 8.dp)
                )
            },
            label = "Medium"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            shadow = {
                Shadow(
                    color = Color(0xFF6366F1).copy(alpha = 0.5f),
                    radius = 24.dp,
                    offset = DpOffset(0.dp, 12.dp)
                )
            },
            label = "Glow"
        )
    }
}

@Composable
private fun InnerShadowDemo(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            innerShadow = {
                InnerShadow(
                    color = Color.Black.copy(alpha = 0.2f),
                    radius = 4.dp,
                    offset = DpOffset(2.dp, 2.dp)
                )
            },
            label = "Subtle"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            innerShadow = {
                InnerShadow(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = 8.dp,
                    offset = DpOffset(4.dp, 4.dp)
                )
            },
            label = "Strong"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            innerShadow = {
                InnerShadow(
                    color = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                    radius = 12.dp,
                    offset = DpOffset(-2.dp, -2.dp)
                )
            },
            label = "Colored"
        )
    }
}

@Composable
private fun HighlightDemo(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            highlight = { Highlight.Default },
            label = "Default"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            highlight = { Highlight.Plain },
            label = "Plain"
        )
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = { blur(16.dp.toPx()) },
            highlight = { Highlight.Ambient },
            label = "Ambient"
        )
    }
}

@Composable
private fun CombinedEffectsDemo(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Full glass effect
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(20.dp.toPx())
                colorControls(saturation = 1.2f)
                opacity(0.9f)
            },
            shadow = {
                Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = 16.dp,
                    offset = DpOffset(0.dp, 8.dp)
                )
            },
            highlight = { Highlight.Default },
            label = "Glass"
        )
        // Frosted glass
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(32.dp.toPx())
                colorControls(saturation = 0.8f)
                opacity(0.85f)
            },
            innerShadow = {
                InnerShadow(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = 4.dp,
                    offset = DpOffset(-1.dp, -1.dp)
                )
            },
            highlight = { Highlight.Ambient },
            label = "Frosted"
        )
        // Liquid glass with lens
        GlassCard(
            backdrop = backdrop,
            modifier = Modifier.weight(1f),
            effects = {
                blur(12.dp.toPx())
                lens(
                    refractionHeight = 20.dp.toPx(),
                    refractionAmount = 10.dp.toPx(),
                    depthEffect = true
                )
                colorControls(saturation = 1.1f)
            },
            shadow = {
                Shadow(
                    color = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                    radius = 20.dp,
                    offset = DpOffset(0.dp, 10.dp)
                )
            },
            highlight = { Highlight.Default },
            label = "Liquid"
        )
    }
}

@Composable
private fun ShapesDemo(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Rounded rectangle
        Box(
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(16.dp) },
                    effects = { blur(16.dp.toPx()) },
                    shadow = { Shadow.Default },
                    highlight = { Highlight.Default }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Rounded",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = { blur(16.dp.toPx()) },
                    shadow = { Shadow.Default },
                    highlight = { Highlight.Default }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Circle",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Pill shape
        Box(
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(percent = 50) },
                    effects = { blur(16.dp.toPx()) },
                    shadow = { Shadow.Default },
                    highlight = { Highlight.Default }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pill",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BottomNavBarDemo(backdrop: Backdrop) {
    val isLightTheme = true // For demo purposes
    val contentColor = if (isLightTheme) Color.Black else Color.White

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 3-tab version (like original catalog)
        var selectedTabIndex1 by remember { mutableIntStateOf(0) }

        LiquidBottomTabs(
            selectedTabIndex = { selectedTabIndex1 },
            onTabSelected = { selectedTabIndex1 = it },
            backdrop = backdrop,
            tabsCount = 3,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            repeat(3) { index ->
                LiquidBottomTab({ selectedTabIndex1 = index }) {
                    Text(
                        text = listOf("🏠", "🔍", "👤")[index],
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Tab ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor
                    )
                }
            }
        }

        // 4-tab version
        var selectedTabIndex2 by remember { mutableIntStateOf(0) }

        LiquidBottomTabs(
            selectedTabIndex = { selectedTabIndex2 },
            onTabSelected = { selectedTabIndex2 = it },
            backdrop = backdrop,
            tabsCount = 4,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            repeat(4) { index ->
                LiquidBottomTab({ selectedTabIndex2 = index }) {
                    Text(
                        text = listOf("🏠", "🔍", "❤️", "👤")[index],
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        listOf("Home", "Search", "Likes", "Profile")[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    effects: BackdropEffectScope.() -> Unit,
    shadow: (() -> Shadow?)? = null,
    innerShadow: (() -> InnerShadow?)? = null,
    highlight: (() -> Highlight?)? = null,
    label: String
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = effects,
                shadow = shadow,
                innerShadow = innerShadow,
                highlight = highlight
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
