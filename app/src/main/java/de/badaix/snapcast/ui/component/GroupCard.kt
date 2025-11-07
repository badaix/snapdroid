package de.badaix.snapcast.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GroupCard(
    name: String,
    isMuted: Boolean,
    onIsMutedChange: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    sinks: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GroupCardHeader(
                name = name,
                isMuted = isMuted,
                onIsMutedChange = onIsMutedChange,
                volume = volume,
                onVolumeChange = onVolumeChange
            )

            HorizontalDivider(Modifier.fillMaxWidth())

            sinks()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCardHeader(
    name: String,
    isMuted: Boolean,
    onIsMutedChange: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Row {
                IconButton(
                    onClick = { onIsMutedChange(!isMuted) }
                ) {
                    if (isMuted) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Unmute"
                        )
                    }
                }

                IconButton(
                    onClick = {

                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More"
                    )
                }
            }
        }

        CustomSlider(
            value = volume,
            onValueChange = { onVolumeChange(it) },
            steps = 99, // 100 values: 0, 1, 2, ..., 100 → 99 intervals between them
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCardSink(
    name: String,
    isMuted: Boolean,
    onIsMutedChange: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit

) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Speaker,
                contentDescription = null,
            )

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Row {
                IconButton(
                    onClick = { onIsMutedChange(!isMuted) }
                ) {
                    if (isMuted) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Unmute"
                        )
                    }
                }

                IconButton(
                    onClick = {

                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More"
                    )
                }
            }
        }

        CustomSlider(
            value = volume,
            onValueChange = { onVolumeChange(it) },
            steps = 99, // 100 values: 0, 1, 2, ..., 100 → 99 intervals between them
            modifier = Modifier.fillMaxWidth()
        )
    }
}
