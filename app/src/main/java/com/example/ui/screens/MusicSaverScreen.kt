package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaylistEntity
import com.example.data.model.StudyMusicTrack
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoLight
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.util.AudioPlayerState
import com.example.util.StudyAudioPlayer

@Composable
fun MusicSaverScreen(
    musicTracks: List<StudyMusicTrack>,
    playlists: List<PlaylistEntity>,
    playerState: AudioPlayerState,
    onSaveAudioUri: (Uri, String, String) -> Unit,
    onPlayTrack: (StudyMusicTrack) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStopAudio: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleFavorite: (Long, Boolean) -> Unit,
    onDeleteTrack: (StudyMusicTrack) -> Unit,
    onCreatePlaylist: (name: String, desc: String, colorHex: String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onAddTrackToPlaylist: (playlistId: Long, trackId: Long) -> Unit,
    onPlayPlaylist: (PlaylistEntity, Boolean) -> Unit,
    onLoadPresets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf(0) } // 0: Tracks, 1: Playlists

    // Import Audio Dialog State
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var customTitleInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Lo-Fi Study") }

    // Create Playlist Dialog State
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var playlistDescInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#6366F1") }

    // Add Track to Playlist State
    var trackToAddToPlaylist by remember { mutableStateOf<StudyMusicTrack?>(null) }
    var playlistToAddTracksTo by remember { mutableStateOf<PlaylistEntity?>(null) }

    val categories = listOf("Lo-Fi Study", "Classical Focus", "Binaural Beats", "Ambient Sounds", "Lecture Audio")
    val playlistColors = listOf("#6366F1", "#10B981", "#06B6D4", "#F59E0B", "#EC4899", "#8B5CF6")

    // Modern SAF Document Picker launcher (Supported on all Android versions, opens system File Manager / Files app)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUri = uri
            customTitleInput = ""
            showImportDialog = true
        }
    }

    // Secondary fallback content picker launcher
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUri = uri
            customTitleInput = ""
            showImportDialog = true
        }
    }

    fun launchMusicPicker() {
        try {
            // Standard Storage Access Framework document picker — launches Google Files, My Files, or device File Manager
            openDocumentLauncher.launch(arrayOf("audio/*", "application/ogg", "application/octet-stream", "*/*"))
        } catch (e: Exception) {
            try {
                getContentLauncher.launch("audio/*")
            } catch (e2: Exception) {
                Toast.makeText(context, "Please install or enable your device's File Manager app.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Music Saver Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Study Music & Playlists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Offline study audio & custom focus playlists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Two prominent, clean action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { launchMusicPicker() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_music_button")
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Music", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onLoadPresets,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigo),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_presets_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load Presets", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        // Active Player Bar (if track is loaded/playing)
        if (playerState.currentTrack != null) {
            StudyMusicPlayerBar(
                playerState = playerState,
                onTogglePlayPause = onTogglePlayPause,
                onStop = onStopAudio,
                onToggleLoop = onToggleLoop,
                onToggleShuffle = onToggleShuffle
            )
        }

        // Segmented Sub-tabs: Tracks vs Playlists
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Tracks (${musicTracks.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Playlists (${playlists.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        // Subtab 0: Tracks List
        if (activeSubTab == 0) {
            if (musicTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = PrimaryIndigo.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No Saved Music Tracks Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Import MP3, M4A, or WAV audio from your phone's file manager, or load built-in focus audio presets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { launchMusicPicker() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open File Manager")
                            }
                            OutlinedButton(
                                onClick = onLoadPresets,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load Presets")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(musicTracks, key = { it.id }) { track ->
                        val isCurrent = playerState.currentTrack?.id == track.id
                        MusicTrackItemCard(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && playerState.isPlaying,
                            onPlayClick = { onPlayTrack(track) },
                            onToggleFavorite = { onToggleFavorite(track.id, !track.isFavorite) },
                            onDeleteClick = { onDeleteTrack(track) },
                            onAddToPlaylistClick = { trackToAddToPlaylist = track }
                        )
                    }
                }
            }
        }

        // Subtab 1: Playlists List
        if (activeSubTab == 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "My Focus Playlists",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Button(
                    onClick = {
                        playlistNameInput = ""
                        playlistDescInput = ""
                        showCreatePlaylistDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier.testTag("make_playlist_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Make Playlist", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No focus playlists created yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistItemCard(
                            playlist = playlist,
                            onPlayPlaylistOrdered = { onPlayPlaylist(playlist, false) },
                            onPlayPlaylistShuffled = { onPlayPlaylist(playlist, true) },
                            onAddTracksClick = { playlistToAddTracksTo = playlist },
                            onDeleteClick = { onDeletePlaylist(playlist) }
                        )
                    }
                }
            }
        }
    }

    // Save Music Dialog
    if (showImportDialog && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Save Audio to StudyGuard", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Give this music track a title and choose a category:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = customTitleInput,
                        onValueChange = { customTitleInput = it },
                        label = { Text("Track Title (e.g. Chill Lo-Fi Beats)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_audio_title_input")
                    )
                    Text("Category:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingUri
                        if (uri != null) {
                            val title = customTitleInput.ifBlank { "Study Audio ${System.currentTimeMillis() % 1000}" }
                            onSaveAudioUri(uri, title, selectedCategory)
                            showImportDialog = false
                            pendingUri = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_music_button")
                ) {
                    Text("Save to Library")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    pendingUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Make New Focus Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text("Playlist Name (e.g. Late Night Math)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("playlist_name_input")
                    )
                    OutlinedTextField(
                        value = playlistDescInput,
                        onValueChange = { playlistDescInput = it },
                        label = { Text("Description (e.g. Binaural & Lofi for deep study)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Theme Color:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        playlistColors.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColorHex == hex) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            onCreatePlaylist(playlistNameInput.trim(), playlistDescInput.trim(), selectedColorHex)
                            showCreatePlaylistDialog = false
                            Toast.makeText(context, "Playlist '$playlistNameInput' created! 🎵", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = playlistNameInput.isNotBlank(),
                    modifier = Modifier.testTag("confirm_make_playlist_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Track to Playlist Picker Dialog
    if (trackToAddToPlaylist != null) {
        AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a playlist for '${trackToAddToPlaylist?.title}':", fontSize = 12.sp)
                    if (playlists.isEmpty()) {
                        Text("No playlists yet! Tap 'Make Playlist' first.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(playlists) { pl ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            val t = trackToAddToPlaylist
                                            if (t != null) {
                                                onAddTrackToPlaylist(pl.id, t.id)
                                                Toast.makeText(context, "Added to '${pl.name}'! 🎶", Toast.LENGTH_SHORT).show()
                                                trackToAddToPlaylist = null
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = PrimaryIndigo)
                                        Text(text = pl.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { trackToAddToPlaylist = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Tracks TO a Playlist Dialog
    if (playlistToAddTracksTo != null) {
        val pl = playlistToAddTracksTo!!
        AlertDialog(
            onDismissRequest = { playlistToAddTracksTo = null },
            title = { Text("Add Tracks to '${pl.name}'", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select tracks from your library:", fontSize = 12.sp)
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(musicTracks) { track ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onAddTrackToPlaylist(pl.id, track.id)
                                        Toast.makeText(context, "Added '${track.title}' to '${pl.name}'", Toast.LENGTH_SHORT).show()
                                        playlistToAddTracksTo = null
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = PrimaryIndigo)
                                        Column {
                                            Text(text = track.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(text = track.artistOrCategory, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    Icon(Icons.Filled.AddCircleOutline, contentDescription = null, tint = PrimaryIndigo)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { playlistToAddTracksTo = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun PlaylistItemCard(
    playlist: PlaylistEntity,
    onPlayPlaylistOrdered: () -> Unit,
    onPlayPlaylistShuffled: () -> Unit,
    onAddTracksClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val barColor = try {
        Color(android.graphics.Color.parseColor(playlist.colorHex))
    } catch (e: Exception) {
        PrimaryIndigo
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(barColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = barColor)
                    }
                    Column {
                        Text(text = playlist.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (playlist.description.isNotBlank()) {
                            Text(text = playlist.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayPlaylistOrdered,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = barColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("In Order ▶️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onPlayPlaylistShuffled,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp), tint = barColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shuffle 🔀", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = barColor)
                }

                FilledTonalIconButton(
                    onClick = onAddTracksClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add Tracks", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
}

@Composable
fun StudyMusicPlayerBar(
    playerState: AudioPlayerState,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleShuffle: () -> Unit = {}
) {
    val track = playerState.currentTrack ?: return

    val progressFraction = if (playerState.totalDurationMs > 0) {
        (playerState.progressMs.toFloat() / playerState.totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = track.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artistOrCategory} • ${formatTimeMs(playerState.progressMs)} / ${formatTimeMs(playerState.totalDurationMs)}",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.isShuffle) TertiaryAmber else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onToggleLoop, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = if (playerState.isLooping) TertiaryAmber else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(36.dp).testTag("player_play_pause_button")) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = TertiaryAmber,
                trackColor = Color(0xFF334155)
            )
        }
    }
}

@Composable
fun MusicTrackItemCard(
    track: StudyMusicTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(38.dp).testTag("play_track_${track.id}")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = if (isCurrent) PrimaryIndigo else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isCurrent) PrimaryIndigo else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = track.artistOrCategory,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track.fileSizeFormatted.isNotBlank()) {
                            Text(
                                text = "• ${track.fileSizeFormatted}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAddToPlaylistClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!track.isPreset) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
