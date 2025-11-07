package de.badaix.snapcast.ui.component

import androidx.annotation.IntRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    @IntRange(from = 0) steps: Int = 0,
    thumb: @Composable (SliderState) -> Unit = {
        val colors = SliderDefaults.colors()
        Canvas(modifier = Modifier.size(20.dp)) {
            drawCircle(color = colors.thumbColor)
        }
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        val colors = SliderDefaults.colors()
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            val fraction = sliderState.coercedValueAsFraction

            // Position where active track ends (thumb center)
            val thumbCenterX = fraction * size.width

            val centerY = size.height / 2
            val strokeWidth = size.height

            // Active track: from left edge (adjusted) to thumb center
            drawLine(
                color = colors.activeTrackColor,
                start = Offset(0f, centerY),
                end = Offset(thumbCenterX, centerY),
                strokeWidth = strokeWidth
            )

            // Inactive track: from thumb center to right edge (adjusted)
            drawLine(
                color = colors.disabledInactiveTrackColor,
                start = Offset(thumbCenterX, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokeWidth
            )
        }
    },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        steps = steps, // 100 values: 0, 1, 2, ..., 100 → 99 intervals between them
        thumb = thumb,
        track = track,
        valueRange = valueRange
    )
}