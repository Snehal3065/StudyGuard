package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DistractionEvent
import com.example.data.model.StudySession
import com.example.ui.StudyUiState
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoLight
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.util.AppUsageInfo
import com.example.util.LaptopStats
import com.example.util.UsageStatsManagerHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(
    uiState: StudyUiState,
    sessions: List<StudySession>,
    totalDistractions: Int,
    shortsBlocked: Int,
    recentDistractions: List<DistractionEvent>,
    onRefreshUsageStats: () -> Unit,
    laptopStats: LaptopStats = LaptopStats(),
    onQuickSyncLaptop: () -> Unit = {},
    onUpdateLaptopStats: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onAddStudyMinutes: (Int, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var deviceFilter by remember { mutableStateOf(0) } // 0: Combined, 1: Phone, 2: Laptop
    var showAdjustLaptopDialog by remember { mutableStateOf(false) }
    var adjustMinutesInput by remember { mutableStateOf(laptopStats.focusMinutes.toString()) }
    var showManualLogDialog by remember { mutableStateOf(false) }
    var manualMinutesInput by remember { mutableStateOf("30") }
    var manualSubjectInput by remember { mutableStateOf("Deep Study Session") }

    // Live phone study minutes: sum all saved sessions + currently running session
    val activeElapsedMinutes = if (uiState.isStudyActive) {
        val start = com.example.util.StudyPreferences.getSessionStartTime(context)
        val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(0L)
        ((elapsed + 55_000L) / 60_000L).coerceAtLeast(1L)
    } else 0L

    val phoneStudyMinutes = remember(sessions, activeElapsedMinutes) {
        sessions.sumOf { it.actualMinutes } + activeElapsedMinutes
    }
    val phoneSessionsCount = remember(sessions, uiState.isStudyActive) {
        sessions.count { it.actualMinutes > 0 || it.isCompleted } + (if (uiState.isStudyActive) 1 else 0)
    }

    val laptopStudyMinutes = laptopStats.focusMinutes
    val laptopSessionsCount = laptopStats.sessionsCount
    val laptopDistractions = laptopStats.distractionsBlocked

    val combinedStudyMinutes = phoneStudyMinutes + laptopStudyMinutes
    val combinedSessionsCount = phoneSessionsCount + laptopSessionsCount
    val combinedDistractions = totalDistractions + laptopDistractions

    fun formatDuration(minutes: Long): String {
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Study & Focus Insights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track phone discipline, laptop study time, and combined focus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Cross-Device Unified Stats Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = SecondaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Cross-Device Unified Focus",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = laptopStats.lastSyncFormatted,
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 3-Pillar Device Comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Phone Box
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(14.dp))
                                    Text("Phone", fontSize = 11.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatDuration(phoneStudyMinutes.toLong()),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Text("${phoneSessionsCount} sessions", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        // Laptop Box
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Laptop, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(14.dp))
                                    Text("Laptop", fontSize = 11.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatDuration(laptopStudyMinutes),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Text("${laptopSessionsCount} sessions", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        // Combined Box
                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.AllInclusive, contentDescription = null, tint = TertiaryAmber, modifier = Modifier.size(14.dp))
                                    Text("Combined", fontSize = 11.sp, color = TertiaryAmber, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatDuration(combinedStudyMinutes),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Text("${combinedSessionsCount} total", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                            }
                        }
                    }

                    // Action buttons: Sync Now + Adjust Laptop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onQuickSyncLaptop() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                adjustMinutesInput = laptopStudyMinutes.toString()
                                showAdjustLaptopDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adjust Laptop", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // View Mode Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = deviceFilter == 0,
                    onClick = { deviceFilter = 0 },
                    label = { Text("Combined (Phone + Laptop)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.AllInclusive, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                FilterChip(
                    selected = deviceFilter == 1,
                    onClick = { deviceFilter = 1 },
                    label = { Text("📱 Phone", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = deviceFilter == 2,
                    onClick = { deviceFilter = 2 },
                    label = { Text("💻 Laptop", fontSize = 11.sp) }
                )
            }
        }

        // Quick Log Study Time Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ Quick Study Time Credit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                        TextButton(
                            onClick = { showManualLogDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Custom Log ✏️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Studied offline or want to log past time? Tap to immediately credit your study stats:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddStudyMinutes(15, "15m Focus Sprint") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Text("+15m", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onAddStudyMinutes(30, "30m Deep Work") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Text("+30m", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onAddStudyMinutes(45, "45m Marathon Sprint") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                        ) {
                            Text("+45m", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onAddStudyMinutes(60, "60m Deep Focus") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TertiaryAmber)
                        ) {
                            Text("+60m", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Summary Metric Grid reflecting the chosen device filter
        item {
            val displayMins = when (deviceFilter) {
                1 -> phoneStudyMinutes.toLong()
                2 -> laptopStudyMinutes
                else -> combinedStudyMinutes
            }
            val displaySessions = when (deviceFilter) {
                1 -> phoneSessionsCount
                2 -> laptopSessionsCount
                else -> combinedSessionsCount
            }
            val displayDistractions = when (deviceFilter) {
                1 -> totalDistractions
                2 -> laptopDistractions
                else -> combinedDistractions
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = when (deviceFilter) {
                            1 -> "Phone Focus Time"
                            2 -> "Laptop Focus Time"
                            else -> "Total Focus Time"
                        },
                        value = formatDuration(displayMins),
                        icon = Icons.Default.Timer,
                        tint = PrimaryIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = when (deviceFilter) {
                            1 -> "Phone Sessions"
                            2 -> "Laptop Sessions"
                            else -> "Total Sessions"
                        },
                        value = displaySessions.toString(),
                        icon = Icons.Default.CheckCircle,
                        tint = SecondaryTeal,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "YouTube Shorts Blocked",
                        value = shortsBlocked.toString(),
                        icon = Icons.Default.SmartDisplay,
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = when (deviceFilter) {
                            1 -> "Phone Apps Blocked"
                            2 -> "Laptop Sites Blocked"
                            else -> "Total Distractions Blocked"
                        },
                        value = displayDistractions.toString(),
                        icon = Icons.Default.Block,
                        tint = TertiaryAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Usage Stats Manager Section
        item {
            UsageStatsManagerSection(
                uiState = uiState,
                onGrantPermission = {
                    UsageStatsManagerHelper.openUsageAccessSettings(context)
                },
                onRefresh = onRefreshUsageStats
            )
        }

        // Recent Distractions Intercepted
        item {
            Text(
                text = "Live Distraction Interceptions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (recentDistractions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No distractions recorded yet. Clean focus! 🌟",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentDistractions.take(15)) { event ->
                DistractionItemRow(event)
            }
        }

        // Past Sessions
        item {
            Text(
                text = "Study Session History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start your first study session from the Study Room tab.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sessions) { session ->
                SessionHistoryRow(session)
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAdjustLaptopDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustLaptopDialog = false },
            title = { Text("Update Laptop Study Stats", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the minutes you studied on your laptop to update your cross-device combined focus stats:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = adjustMinutesInput,
                        onValueChange = { adjustMinutesInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Laptop Study Minutes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(onClick = { adjustMinutesInput = "30" }, label = { Text("30m") })
                        SuggestionChip(onClick = { adjustMinutesInput = "45" }, label = { Text("45m") })
                        SuggestionChip(onClick = { adjustMinutesInput = "60" }, label = { Text("60m") })
                        SuggestionChip(onClick = { adjustMinutesInput = "90" }, label = { Text("90m") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = adjustMinutesInput.toLongOrNull() ?: laptopStats.focusMinutes
                        val sessions = (mins / 30L).toInt().coerceAtLeast(1)
                        onUpdateLaptopStats(mins, sessions, laptopStats.distractionsBlocked)
                        showAdjustLaptopDialog = false
                    }
                ) {
                    Text("Save Stats")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustLaptopDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showManualLogDialog) {
        AlertDialog(
            onDismissRequest = { showManualLogDialog = false },
            title = { Text("Log Study Session", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Manually credit study time to your daily stats & achievements:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = manualSubjectInput,
                        onValueChange = { manualSubjectInput = it },
                        label = { Text("Study Subject / Topic") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualMinutesInput,
                        onValueChange = { manualMinutesInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Minutes Studied") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(onClick = { manualMinutesInput = "15" }, label = { Text("15m") })
                        SuggestionChip(onClick = { manualMinutesInput = "25" }, label = { Text("25m") })
                        SuggestionChip(onClick = { manualMinutesInput = "45" }, label = { Text("45m") })
                        SuggestionChip(onClick = { manualMinutesInput = "60" }, label = { Text("60m") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = manualMinutesInput.toIntOrNull() ?: 25
                        if (mins > 0) {
                            onAddStudyMinutes(mins, manualSubjectInput)
                        }
                        showManualLogDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Credit Study Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualLogDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UsageStatsManagerSection(
    uiState: StudyUiState,
    onGrantPermission: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Usage Stats Manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp).testTag("refresh_usage_stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Usage Stats",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!uiState.isUsageStatsEnabled) {
                // Permission required card
                Surface(
                    color = TertiaryAmber.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Usage Access Required",
                            fontWeight = FontWeight.Bold,
                            color = TertiaryAmber,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Grant Usage Stats Access to let StudyGuard track exact foreground screen time and identify time sinks.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = onGrantPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = TertiaryAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("enable_usage_access_button")
                        ) {
                            Text("Enable Usage Access", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Usage stats active summary
                val totalMinutes = (uiState.todayScreenTimeMs / 60000L).toInt()
                val distractionMinutes = (uiState.todayDistractionTimeMs / 60000L).toInt()
                val productiveMinutes = (totalMinutes - distractionMinutes).coerceAtLeast(0)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Today's Total Screen Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Distraction Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = if (distractionMinutes >= 60) "${distractionMinutes / 60}h ${distractionMinutes % 60}m" else "${distractionMinutes}m",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                // Progress Bar: Productive vs Distraction
                val ratio = if (totalMinutes > 0) (distractionMinutes.toFloat() / totalMinutes.toFloat()) else 0f
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Color(0xFFEF4444),
                    trackColor = Color(0xFF10B981)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "● Productive: ${productiveMinutes}m",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "● Distractions: ${distractionMinutes}m",
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // List of top used apps today
                if (uiState.usageStatsList.isNotEmpty()) {
                    Text(
                        text = "Most Used Apps Today:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.usageStatsList.take(6).forEach { appUsage ->
                            AppUsageRow(appUsage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageRow(app: AppUsageInfo) {
    val totalMins = (app.totalTimeInForegroundMs / 60000L).toInt()
    val formattedDuration = if (totalMins >= 60) "${totalMins / 60}h ${totalMins % 60}m" else "${totalMins}m"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (app.isDistraction) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (app.isDistraction) Color(0xFFEF4444) else Color(0xFF10B981)
                )
            }
            Text(
                text = app.appName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = formattedDuration,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (app.isDistraction) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }

            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DistractionItemRow(event: DistractionEvent) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(event.timestamp) { timeFormat.format(Date(event.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (event.eventType == "YOUTUBE_SHORTS_BLOCKED") Icons.Default.SmartDisplay else Icons.Default.Block,
                    contentDescription = null,
                    tint = if (event.eventType == "YOUTUBE_SHORTS_BLOCKED") Color(0xFFFF0000) else Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = event.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (event.eventType == "YOUTUBE_SHORTS_BLOCKED") "Shorts blocked (study videos allowed)" else event.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun SessionHistoryRow(session: StudySession) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(session.startTime) { dateFormat.format(Date(session.startTime)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$formattedDate • Planned ${session.plannedMinutes}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = if (session.isCompleted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (session.isCompleted) "Completed (${session.actualMinutes}m)" else "Stopped (${session.actualMinutes}m)",
                    color = if (session.isCompleted) Color(0xFF10B981) else Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
