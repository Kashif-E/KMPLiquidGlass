# Helpers — required by the interactive recipes

The interactive recipes in `components.md` (button, toggle, slider, bottom tabs) rely on three helper classes that the catalog ships with. They handle the spring-physics + drag-velocity feel that makes the components look like Apple's. None of these are part of the `backdrop` library itself — they're catalog code you copy into your own project.

Drop these three files (or the equivalent) into your project before using the recipes that depend on them:

1. `inspectDragGestures` — a low-level pointer pipeline that exposes `down → drag → up/cancel` events with no slop, used by both helpers below.
2. `InteractiveHighlight` — tracks a press and a moving touch position, exposes `pressProgress` and `offset`, plus a drawing modifier and a gesture modifier. Used by `LiquidButton` for the tanh-deformation drag, and by `LiquidBottomTabs` for the spotlight position.
3. `DampedDragAnimation` — a beefier version: holds a `value` in a custom range with springs for value, velocity, scaleX, scaleY, and pressProgress. Used by `LiquidToggle`, `LiquidSlider`, `LiquidBottomTabs`. The squish-on-drag and overscroll-on-velocity feel comes from this class.

You can also use simpler `animateFloatAsState` versions for low-fidelity glass — the recipes are written assuming you want the catalog's exact feel.

---

## inspectDragGestures

Compose's `detectDragGestures` waits for a "real" drag (slop) before reporting `onDragStart`, which makes the press animation feel late. This version fires `onDragStart` on the very first down event, then continues reporting every position change until up.

```kotlin
package your.app.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull

suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)
        val down = awaitFirstDown(false)
        val drag = initialDown

        onDragStart(down)
        onDrag(drag, Offset.Zero)
        val upEvent = drag(
            pointerId = drag.id,
            onDrag = { onDrag(it, it.positionChange()) }
        )
        if (upEvent == null) onDragCancel() else onDragEnd(upEvent)
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) return null
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) return null
        if (change.changedToUpIgnoreConsumed()) return change
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) return dragEvent
            else pointer = otherDown.id
        } else if (dragEvent.previousPosition != dragEvent.position) {
            return dragEvent
        }
    }
}
```

---

## InteractiveHighlight

A lightweight "pressed + cursor offset" tracker. The `position` lambda lets you remap the touch position to anywhere on the surface — used by the bottom tab bar to snap the spotlight to the active tab's center while still tracking finger drag.

```kotlin
package your.app.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {
    private val pressProgressSpec = spring<Float>(0.5f, 300f, 0.001f)
    private val positionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    /** Paints a soft white "ink" over the surface at the touch point while pressed. */
    val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0f) {
            drawRect(Color.White.copy(0.25f * progress), blendMode = BlendMode.Plus)
        }
        drawContent()
    }

    /** Attach to the element that should react to drags. Drives press + position springs. */
    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                startPosition = down.position
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(1f, pressProgressSpec) }
                    launch { positionAnimation.snapTo(startPosition) }
                }
            },
            onDragEnd = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionSpec) }
                }
            },
            onDragCancel = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionSpec) }
                }
            }
        ) { change, _ ->
            animationScope.launch { positionAnimation.snapTo(change.position) }
        }
    }
}
```

The original Android-13+ version also runs an AGSL shader to do an additive radial highlight at the touch point. The version above uses the catalog's cross-platform fallback (a flat additive white overlay) — it works everywhere and is what the catalog ships in commonMain. If you want the shader version, see `references/advanced.md`.

---

## DampedDragAnimation

The big one. This is what gives the toggle / slider / tabs their physics. It owns:

- `value` — the data value, range-clamped, animated by a stiff spring.
- `progress` — `(value - min) / (max - min)`, useful for layout.
- `velocity` — animated velocity, used for the squish-on-fling effect.
- `pressProgress` — 0 when idle, 1 while held.
- `scaleX` / `scaleY` — animated to `pressedScale` while held, back to `initialScale` on release. Use them in `layerBlock` so the modifier reads them per frame.
- `modifier` — attach to the draggable element.

```kotlin
package your.app.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
    private val valueSpec = spring<Float>(1f, 1000f, visibilityThreshold)
    private val velocitySpec = spring<Float>(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressSpec = spring<Float>(1f, 1000f, 0.001f)
    private val scaleXSpec = spring<Float>(0.6f, 250f, 0.001f)
    private val scaleYSpec = spring<Float>(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val progress: Float
        get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down -> onDragStarted(down.position); press() },
            onDragEnd = { onDragStopped(); release() },
            onDragCancel = { onDragStopped(); release() }
        ) { _, dragAmount -> onDrag(size, dragAmount) }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            delay(16L)
            // Wait for the value spring to almost settle before un-pressing,
            // so the squish doesn't snap back mid-fling.
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, valueSpec) { updateVelocity() } }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocitySpec) }
                }
                release()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun updateVelocity() {
        velocityTracker.addPosition(
            Clock.System.now().toEpochMilliseconds(),
            Offset(value, 0f)
        )
        val targetVelocity =
            velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocitySpec) }
    }
}
```

### How recipes use these fields

Inside `effects = { ... }`, `highlight = { ... }`, `layerBlock = { ... }`, `onDrawSurface = { ... }`, you read the helper's properties directly. They're backed by `Animatable`s, so reads from inside the lambdas re-trigger the modifier at exactly the frequency the lambdas are evaluated:

```kotlin
.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedCornerShape(100.dp) },
    effects = {
        val p = drag.pressProgress
        blur(8.dp.toPx() * (1f - p))
        lens(5.dp.toPx() * p, 10.dp.toPx() * p, chromaticAberration = true)
    },
    layerBlock = {
        scaleX = drag.scaleX
        scaleY = drag.scaleY
        // velocity-driven squish: long axis stretches with fling direction
        val v = drag.velocity / 10f
        scaleX /= 1f - (v * 0.75f).fastCoerceIn(-0.2f, 0.2f)
        scaleY *= 1f - (v * 0.25f).fastCoerceIn(-0.2f, 0.2f)
    }
)
```

That's the canonical pattern — recipes below all use it.
