package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoLight
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.util.MarathonConfig
import com.example.util.MarathonPhase
import com.example.util.MarathonState

@Composable
fun ActiveMarathonCard(
    marathonState: MarathonState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkipBreak: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isStudyPhase = marathonState.currentPhase == MarathonPhase.STUDY
    val isNuclear = marathonState.isNuclear
    val animatedProgress by animateFloatAsState(
        targetValue = marathonState.progressFraction,
        label = "phase_progress"
    )

    var showConfirmStop by remember { mutableStateOf(false) }

    val containerGradient = when {
        isNuclear && isStudyPhase -> Brush.verticalGradient(listOf(Color(0xFF7F1D1D), Color(0xFF0F172A)))
        isNuclear && !isStudyPhase -> Brush.verticalGradient(listOf(Color(0xFF312E81), Color(0xFF0F172A)))
        isStudyPhase -> Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF0F172A)))
        else -> Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerGradient)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = when {
                        isNuclear && isStudyPhase -> Color(0xFFDC2626).copy(alpha = 0.25f)
                        isNuclear && !isStudyPhase -> Color(0xFF6366F1).copy(alpha = 0.25f)
                        isStudyPhase -> Color(0xFF10B981).copy(alpha = 0.2f)
                        else -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                    },
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isNuclear && isStudyPhase -> Color(0xFFEF4444)
                                        isNuclear && !isStudyPhase -> Color(0xFF818CF8)
                                        isStudyPhase -> Color(0xFF10B981)
                                        else -> Color(0xFF38BDF8)
                                    }
                                )
                        )
                        Text(
                            text = when {
                                isNuclear && isStudyPhase -> "☢️ NUCLEAR CYCLE ${marathonState.currentCycle} OF ${marathonState.config.totalCycles} • STRICT FOCUS"
                                isNuclear && !isStudyPhase -> "☕ NUCLEAR BREAK • CYCLE ${marathonState.currentCycle} (SETTINGS FROZEN)"
                                isStudyPhase -> "CYCLE ${marathonState.currentCycle} OF ${marathonState.config.totalCycles} • STUDY"
                                else -> "CYCLE ${marathonState.currentCycle} BREAK • REST"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                isNuclear && isStudyPhase -> Color(0xFFFCA5A5)
                                isNuclear && !isStudyPhase -> Color(0xFFA5B4FC)
                                isStudyPhase -> Color(0xFF34D399)
                                else -> Color(0xFF38BDF8)
                            },
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = "Total: ${marathonState.config.totalHoursFormatted}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Subject / Title
            Text(
                text = marathonState.config.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            // Large Digital Timer
            Text(
                text = marathonState.phaseTimeString,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = if (isStudyPhase) Color.White else Color(0xFF7DD3FC),
                letterSpacing = 2.sp
            )

            // Phase Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (isStudyPhase) Color(0xFF10B981) else Color(0xFF38BDF8),
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isStudyPhase) "🔒 Shields active (Shorts locked)" else "☕ Relax, hydrate, or stretch!",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = if (isStudyPhase) "${marathonState.config.studyDurationMinutes}m block" else "${marathonState.config.breakDurationMinutes}m break",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Cycles Grid Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (cycle in 1..marathonState.config.totalCycles) {
                    val isPast = cycle < marathonState.currentCycle
                    val isCurrent = cycle == marathonState.currentCycle
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isPast -> Color(0xFF10B981)
                            isCurrent && isStudyPhase -> PrimaryIndigo
                            isCurrent && !isStudyPhase -> Color(0xFF0284C7)
                            else -> Color.White.copy(alpha = 0.1f)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "C$cycle",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isPast) "Done" else if (isCurrent) (if (isStudyPhase) "Study" else "Break") else "${marathonState.config.studyDurationMinutes}m",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Marathon Controls
            if (isNuclear) {
                if (!isStudyPhase) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSkipBreak,
                            modifier = Modifier.fillMaxWidth().testTag("skip_break_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Skip Break & Study Now 📚", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            color = Color(0xFF1E1B4B),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "☕ Nuclear Break: Distraction apps unlock for rest, but settings & app lock configuration remain strictly frozen.",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Surface(
                        color = Color(0xFF450A0A),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "☢️ Unbreakable Nuclear Focus: Distractions & notifications blocked. Pausing and early cancellation are disabled.",
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isStudyPhase) {
                        Button(
                            onClick = onSkipBreak,
                            modifier = Modifier.weight(1f).testTag("skip_break_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Skip Break 📚", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        if (marathonState.isPaused) {
                            Button(
                                onClick = onResume,
                                modifier = Modifier.weight(1f).testTag("resume_marathon_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onPause,
                                modifier = Modifier.weight(1f).testTag("pause_marathon_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showConfirmStop = true },
                        modifier = Modifier.testTag("stop_marathon_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171))
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "End", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End Early", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showConfirmStop) {
        AlertDialog(
            onDismissRequest = { showConfirmStop = false },
            title = { Text("End Study Marathon?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Your completed study time (${marathonState.completedStudyMinutes} min) will be logged to your Focus History.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmStop = false
                        onStop()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("End Marathon")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmStop = false }) {
                    Text("Keep Going")
                }
            }
        )
    }
}

@Composable
fun MarathonSetupCard(
    onStartMarathon: (MarathonConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var subjectTitle by remember { mutableStateOf("Semester Exam Deep Work") }
    var studyMinutes by remember { mutableStateOf(45) }
    var breakMinutes by remember { mutableStateOf(15) }
    var cycleCount by remember { mutableStateOf(4) }
    var isCustomExpanded by remember { mutableStateOf(false) }

    val totalHoursText = remember(studyMinutes, breakMinutes, cycleCount) {
        val totalMins = cycleCount * (studyMinutes + breakMinutes)
        val h = totalMins / 60
        val m = totalMins % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = PrimaryIndigo)
                    }
                    Column {
                        Text(
                            text = "Long Study Marathon",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Multi-cycle study & break routine",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = TertiaryAmber.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "Pomodoro Engine",
                        color = TertiaryAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }

            Text(
                text = "FEATURED MARATHON PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Preset 1: 4-Hour Marathon (45m study / 15m break x 4 cycles) — Highlighted!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onStartMarathon(
                            MarathonConfig(
                                title = "4-Hour Focus Marathon",
                                studyDurationMinutes = 45,
                                breakDurationMinutes = 15,
                                totalCycles = 4
                            )
                        )
                    }
                    .testTag("start_4h_marathon_preset"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryIndigo)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⭐", fontSize = 16.sp)
                            Text(
                                text = "4-Hour Marathon",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = PrimaryIndigo
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = PrimaryIndigo
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "45m Study • 15m Break • 4 Cycles (4 Hours Total)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "The ultimate balance of deep flow state and recovery to prevent cognitive burnout.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = {
                            onStartMarathon(
                                MarathonConfig(
                                    title = "4-Hour Focus Marathon",
                                    studyDurationMinutes = 45,
                                    breakDurationMinutes = 15,
                                    totalCycles = 4
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start 4-Hour Marathon", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }

            // Preset 2: 3-Hour Ultradian (50m study / 10m break x 3)
            PresetCompactCard(
                title = "3-Hour Ultradian Flow",
                details = "50 min Study • 10 min Break • 3 Cycles (3 Hours)",
                onClick = {
                    onStartMarathon(
                        MarathonConfig(
                            title = "3-Hour Ultradian Flow",
                            studyDurationMinutes = 50,
                            breakDurationMinutes = 10,
                            totalCycles = 3
                        )
                    )
                }
            )

            // Preset 3: 2-Hour Classic Pomodoro (25m study / 5m break x 4)
            PresetCompactCard(
                title = "2-Hour Classic Pomodoro",
                details = "25 min Study • 5 min Break • 4 Cycles (2 Hours)",
                onClick = {
                    onStartMarathon(
                        MarathonConfig(
                            title = "2-Hour Classic Pomodoro",
                            studyDurationMinutes = 25,
                            breakDurationMinutes = 5,
                            totalCycles = 4
                        )
                    )
                }
            )

            // Custom Marathon Builder Expandable
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCustomExpanded = !isCustomExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Custom Marathon Builder",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    imageVector = if (isCustomExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = isCustomExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = subjectTitle,
                        onValueChange = { subjectTitle = it },
                        label = { Text("Marathon Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("marathon_subject_input")
                    )

                    // Study Duration Picker
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Study Block Duration:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf(25, 30, 45, 50, 60, 90).forEach { mins ->
                                FilterChip(
                                    selected = studyMinutes == mins,
                                    onClick = { studyMinutes = mins },
                                    label = { Text("${mins}m", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Break Duration Picker
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Break Block Duration:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf(5, 10, 15, 20, 30).forEach { mins ->
                                FilterChip(
                                    selected = breakMinutes == mins,
                                    onClick = { breakMinutes = mins },
                                    label = { Text("${mins}m", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Cycles Count Picker
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Total Cycles:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf(2, 3, 4, 5, 6).forEach { c ->
                                FilterChip(
                                    selected = cycleCount == c,
                                    onClick = { cycleCount = c },
                                    label = { Text("$c Cycles", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Live Summary Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Marathon Commitment:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$totalHoursText (${cycleCount * studyMinutes}m focus + ${cycleCount * breakMinutes}m breaks)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onStartMarathon(
                                MarathonConfig(
                                    title = subjectTitle.ifBlank { "Custom Focus Marathon" },
                                    studyDurationMinutes = studyMinutes,
                                    breakDurationMinutes = breakMinutes,
                                    totalCycles = cycleCount
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("start_custom_marathon_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                    ) {
                        Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Custom Marathon ($totalHoursText)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCompactCard(
    title: String,
    details: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "Start", tint = PrimaryIndigo)
        }
    }
}
