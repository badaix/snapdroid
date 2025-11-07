package de.badaix.snapcast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SpeakerGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.badaix.snapcast.ui.component.GroupCard
import de.badaix.snapcast.ui.component.GroupCardSink

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    var selectedDestination by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MainTopBar(
                subtitle = "rk3318-box"
            )
        },
//        bottomBar = {
//            MainBottomBar(
//                selectedDestination = selectedDestination,
//                onDestinationSelected = { selectedDestination = it }
//            )
//        },
        floatingActionButton = {
            MainFloatingActionButton(
                started = started,
                onStart = {
                    started = true
                },
                onStop = {
                    started = false
                }
            )
        }
    ) { innerPadding ->
        MainContent(
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    subtitle: String?,
) {
    TopAppBar(
        title = {
            Column {
                Text("Snapcast", maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    )
}

@Composable
private fun MainFloatingActionButton(
    started: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    FloatingActionButton(
        onClick = {
            if (started) {
                onStop()
            } else {
                onStart()
            }
        },
    ) {
        if (started) {
            Icon(Icons.Filled.Stop, "Stop")
        } else {
            Icon(Icons.Filled.PlayArrow, "Start")
        }
    }
}

private enum class Destination(
    val icon: ImageVector,
    val contentDescription: String,
    val label: String,
) {
    GROUPS(
        icon = Icons.Outlined.SpeakerGroup,
        contentDescription = "Groups",
        label = "Groups"
    ),
    SETTINGS(
        icon = Icons.Outlined.Settings,
        contentDescription = "Settings",
        label = "Settings"
    ),
    ABOUT(
        icon = Icons.Outlined.Info,
        contentDescription = "About",
        label = "About"
    )
}

@Composable
private fun MainBottomBar(
    selectedDestination: Int,
    onDestinationSelected: (Int) -> Unit
) {
    NavigationBar {
        Destination.entries.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = selectedDestination == index,
                onClick = {
                    onDestinationSelected(index)
                },
                icon = {
                    Icon(
                        destination.icon,
                        contentDescription = destination.contentDescription
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(3) { index ->
            GroupCard(
                name = "Group $index",
                isMuted = false,
                onIsMutedChange = {  },
                volume = 0.5f,
                onVolumeChange = {  },
                sinks = {
                    for (i in 1..3) {
                        GroupCardSink(
                            name = "Sink $i",
                            isMuted = false,
                            onIsMutedChange = {  },
                            volume = 0.5f,
                            onVolumeChange = {  }
                        )
                    }
                }
            )
        }
    }
}
