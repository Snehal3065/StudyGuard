package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.StudyUiState
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoLight
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.util.MarathonConfig
import com.example.util.MarathonState

@Composable
fun StudyRoomScreen(
    uiState: StudyUiState,
    marathonState: MarathonState = MarathonState(),
    onStartSession: (subject: String, minutes: Int) -> Unit,
    onStopSession: () -> Unit,
    onToggleStudyTimer: () -> Unit,
    onStartMarathon: (MarathonConfig) -> Unit = {},
    onPauseMarathon: () -> Unit = {},
    onResumeMarathon: () -> Unit = {},
    onSkipMarathonBreak: () -> Unit = {},
    onStopMarathon: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onToggleFloatingWidget: (Boolean) -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var subjectInput by remember { mutableStateOf("Calculus & Physics") }
    var selectedMinutes by remember { mutableStateOf(25) }
    var customMinutesInput by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var studyModeTab by remember(marathonState.isActive) {
        mutableStateOf(if (marathonState.isActive) 1 else 0)
    }

    val presetDurations = listOf(15, 25, 45, 60, 90)
    val popularSubjects = listOf("Calculus", "Physics", "Computer Science", "Biology", "Literature", "Exam Prep")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Hero Card featuring the user's intense studying Luffy image!
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1B4B)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intense Studying Luffy Image Avatar
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(2.dp, PrimaryIndigoLight, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.luffy_study_image_1790402150695),
                        contentDescription = "Intense Studying Focus Mascot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = TertiaryAmber.copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "LOCKED IN FOCUS",
                            color = TertiaryAmber,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pure Study Sanctuary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Distraction apps locked away. YouTube Shorts blocked, study videos allowed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Quick Study Timer Toggle Switch Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (uiState.isStudyActive) Icons.Filled.Timer else Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = if (uiState.isStudyActive) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (uiState.isStudyActive) "Study Timer Active" else "Study Timer Inactive",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.isStudyActive) "Tap switch to stop early" else "Tap switch to lock in 25m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Switch(
                    checked = uiState.isStudyActive,
                    onCheckedChange = { onToggleStudyTimer() },
                    modifier = Modifier.testTag("study_timer_toggle_switch")
                )
            }
        }

        // Smart Study Reminders Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenReminders() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TertiaryAmber.copy(alpha = 0.12f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, TertiaryAmber.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Alarm, contentDescription = null, tint = TertiaryAmber)
                    Column {
                        Text("Smart Study Reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Morning kickoff, afternoon sprint & posture nudges", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = onOpenReminders,
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryAmber),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("open_reminders_button")
                ) {
                    Text("Nudges", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Study Mode Tab: Sprint Timer vs Long Study Marathon
        TabRow(
            selectedTabIndex = studyModeTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = studyModeTab == 0,
                onClick = { studyModeTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Focus Sprint", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Tab(
                selected = studyModeTab == 1,
                onClick = { studyModeTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(if (marathonState.isActive) "Active Marathon ⏳" else "Study Marathon ⏳", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        // Mode Content
        if (studyModeTab == 1) {
            // Marathon Mode: Long Study Hours (e.g. 45m study / 15m break x 4 cycles)
            if (marathonState.isActive) {
                ActiveMarathonCard(
                    marathonState = marathonState,
                    onPause = onPauseMarathon,
                    onResume = onResumeMarathon,
                    onSkipBreak = onSkipMarathonBreak,
                    onStop = onStopMarathon
                )
            } else {
                MarathonSetupCard(
                    onStartMarathon = onStartMarathon
                )
            }
        } else {
            // Single Focus Session Card
            if (uiState.isStudyActive && !marathonState.isActive) {
                ActiveSessionCard(
                    uiState = uiState,
                    onStopSession = onStopSession
                )
            } else {
                SessionSetupCard(
                    subjectInput = subjectInput,
                    onSubjectChange = { subjectInput = it },
                    popularSubjects = popularSubjects,
                    selectedMinutes = selectedMinutes,
                    presetDurations = presetDurations,
                    onSelectDuration = { selectedMinutes = it },
                    onCustomDurationClick = { showCustomDialog = true },
                    onStartSession = {
                        onStartSession(subjectInput, selectedMinutes)
                    }
                )
            }
        }

        // Accessibility & Floating Focus Overlay Widget Controller Card
        AccessibilityWidgetControlCard(
            uiState = uiState,
            onToggleFloatingWidget = { enable ->
                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    context.startActivity(intent)
                } else {
                    onToggleFloatingWidget(enable)
                }
            }
        )

        // Protection Status Overview
        ProtectionStatusCard(
            uiState = uiState,
            onConfigureClick = onNavigateToPermissions
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Study Duration", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customMinutesInput,
                    onValueChange = { customMinutesInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Minutes (1 - 360)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_minutes_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = customMinutesInput.toIntOrNull() ?: 25
                        selectedMinutes = mins.coerceIn(5, 360)
                        showCustomDialog = false
                    }
                ) {
                    Text("Set Duration")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AccessibilityWidgetControlCard(
    uiState: StudyUiState,
    onToggleFloatingWidget: (Boolean) -> Unit
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SecondaryTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = SecondaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Accessibility Floating Widget",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Floating study bubble on screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Switch(
                    checked = uiState.isFloatingWidgetActive,
                    onCheckedChange = onToggleFloatingWidget,
                    modifier = Modifier.testTag("floating_widget_switch")
                )
            }

            Text(
                text = "Keeps a floating countdown bubble with your mascot on top of educational apps & study videos. Also add the StudyGuard Widget to your Android Home Screen!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ActiveSessionCard(
    uiState: StudyUiState,
    onStopSession: () -> Unit
) {
    val totalPlannedSeconds = (uiState.plannedMinutes * 60L).coerceAtLeast(1L)
    val elapsedSeconds = (totalPlannedSeconds - uiState.remainingSeconds).coerceAtLeast(0L)
    val progress = (elapsedSeconds.toFloat() / totalPlannedSeconds.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    val minutes = uiState.remainingSeconds / 60
    val seconds = uiState.remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "STUDY SESSION IN PROGRESS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigoLight,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = uiState.currentSubject,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Circular progress or prominent timer with mascot
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = TertiaryAmber,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.luffy_study_image_1790402150695),
                        contentDescription = null,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, PrimaryIndigoLight, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeFormatted,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Active Shield Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActiveShieldPill(
                    icon = Icons.Default.OndemandVideo,
                    label = "Shorts Blocked",
                    active = uiState.isYouTubeShortsBlocked
                )
                ActiveShieldPill(
                    icon = Icons.Default.Lock,
                    label = "Apps Locked",
                    active = true
                )
                ActiveShieldPill(
                    icon = Icons.Default.Security,
                    label = "Uninstall Protected",
                    active = uiState.isDeviceAdminEnabled
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onStopSession,
                modifier = Modifier.fillMaxWidth().testTag("end_study_session_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("End Study Session Early")
            }
        }
    }
}

@Composable
fun ActiveShieldPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (active) PrimaryIndigo.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) PrimaryIndigoLight else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}

@Composable
fun SessionSetupCard(
    subjectInput: String,
    onSubjectChange: (String) -> Unit,
    popularSubjects: List<String>,
    selectedMinutes: Int,
    presetDurations: List<Int>,
    onSelectDuration: (Int) -> Unit,
    onCustomDurationClick: () -> Unit,
    onStartSession: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Start Study Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Subject TextField
            OutlinedTextField(
                value = subjectInput,
                onValueChange = onSubjectChange,
                label = { Text("What are you studying?") },
                placeholder = { Text("e.g., Organic Chemistry Exam") },
                leadingIcon = {
                    Icon(Icons.Default.School, contentDescription = null, tint = PrimaryIndigo)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("subject_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Subject quick chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(popularSubjects) { subj ->
                    FilterChip(
                        selected = subjectInput == subj,
                        onClick = { onSubjectChange(subj) },
                        label = { Text(subj, fontSize = 12.sp) }
                    )
                }
            }

            Text(
                text = "Session Duration",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            // Duration chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetDurations.forEach { mins ->
                    FilterChip(
                        selected = selectedMinutes == mins,
                        onClick = { onSelectDuration(mins) },
                        label = {
                            Text("${mins}m", fontWeight = FontWeight.Bold)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            TextButton(
                onClick = onCustomDurationClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Custom duration: ${selectedMinutes}m", fontSize = 12.sp)
            }

            Button(
                onClick = onStartSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_study_session_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryIndigo
                )
            ) {
                Icon(Icons.Default.LockClock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lock In & Start Studying ($selectedMinutes min)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProtectionStatusCard(
    uiState: StudyUiState,
    onConfigureClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Protection System Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onConfigureClick) {
                    Text("Configure", fontSize = 13.sp)
                }
            }

            ProtectionRow(
                title = "YouTube Shorts Shield",
                subtitle = "Shorts blocked; educational YouTube videos allowed",
                isActive = uiState.isYouTubeShortsBlocked
            )

            ProtectionRow(
                title = "Uninstall Protection",
                subtitle = "Device Admin blocks deletion during study hours",
                isActive = uiState.isDeviceAdminEnabled
            )

            ProtectionRow(
                title = "Accessibility Monitoring",
                subtitle = "Detects distraction apps instantly",
                isActive = uiState.isAccessibilityEnabled
            )

            ProtectionRow(
                title = "Usage Stats Manager",
                subtitle = "Tracks real foreground app screen time and distraction stats",
                isActive = uiState.isUsageStatsEnabled
            )
        }
    }
}

@Composable
fun ProtectionRow(title: String, subtitle: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isActive) Color(0xFF10B981) else Color(0xFFF59E0B),
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
