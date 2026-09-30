package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudyUiState
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TertiaryAmber
import com.example.util.LaptopStats
import com.example.util.SyncDevice

@Composable
fun SyncHubScreen(
    syncCode: String,
    lastSyncFormatted: String,
    devices: List<SyncDevice>,
    onTriggerSync: (onComplete: () -> Unit) -> Unit,
    onExportSyncData: () -> String,
    onImportSyncData: (String) -> Boolean,
    onOpenChromeHelp: () -> Unit,
    laptopStats: LaptopStats = LaptopStats(),
    phoneStudyMinutes: Long = 0L,
    phoneSessionsCount: Int = 0,
    phoneDistractionsBlocked: Int = 0,
    onUpdateLaptopStats: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onQuickAddLaptopStudy: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSyncing by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var showAdjustLaptopDialog by remember { mutableStateOf(false) }
    var adjustMinutesInput by remember { mutableStateOf(laptopStats.focusMinutes.toString()) }

    val combinedStudyMinutes = phoneStudyMinutes + laptopStats.focusMinutes
    val combinedSessionsCount = phoneSessionsCount + laptopStats.sessionsCount
    val combinedDistractions = phoneDistractionsBlocked + laptopStats.distractionsBlocked

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
    }

    fun formatDuration(minutes: Long): String {
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1B4B)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SecondaryTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SyncAlt,
                                contentDescription = null,
                                tint = SecondaryTeal
                            )
                        }
                        Column {
                            Text(
                                text = "Cross-Device Sync Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Keep phone & laptop in lockstep",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = lastSyncFormatted,
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Syncs active timer sessions, YouTube Shorts shields, and distraction website blocklists between your Android phone and desktop browser extension.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = {
                        isSyncing = true
                        onTriggerSync {
                            isSyncing = false
                            Toast.makeText(context, "All devices synchronized! 🔄", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_now_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synchronizing...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Cross-Device Combined Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Icon(Icons.Filled.AllInclusive, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "Cross-Device Combined Stats",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Phone + Laptop study time aggregated",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "LIVE SYNC",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // 3 Column Grid: Phone, Laptop, Combined
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Phone Stats Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                                Icon(Icons.Filled.Smartphone, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                                Text("Phone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatDuration(phoneStudyMinutes),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = PrimaryIndigo
                            )
                            Text("${phoneSessionsCount} sessions", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Laptop Stats Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                                Icon(Icons.Filled.Laptop, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(14.dp))
                                Text("Laptop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatDuration(laptopStats.focusMinutes),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = SecondaryTeal
                            )
                            Text("${laptopStats.sessionsCount} sessions", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Combined Box
                    Surface(
                        color = TertiaryAmber.copy(alpha = 0.15f),
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
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = TertiaryAmber, modifier = Modifier.size(14.dp))
                                Text("Combined", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFB45309))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatDuration(combinedStudyMinutes),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFFB45309)
                            )
                            Text("${combinedSessionsCount} total", fontSize = 10.sp, color = Color(0xFF78350F))
                        }
                    }
                }

                // Quick Laptop Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onQuickAddLaptopStudy(30L)
                            Toast.makeText(context, "+30m study added from Laptop! 💻", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+30m Laptop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            adjustMinutesInput = laptopStats.focusMinutes.toString()
                            showAdjustLaptopDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Laptop Time", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Quick Pair Code Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Quick Pairing Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(onClick = { copyToClipboard("Sync Pairing Code", syncCode) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Code", tint = PrimaryIndigo)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = syncCode,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = PrimaryIndigo,
                            letterSpacing = 4.sp
                        )
                    }
                }

                Text(
                    text = "Enter this 6-digit code in the StudyGuard Chrome extension popup on your laptop to pair instantly.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Connected / Paired Devices
        Text(
            text = "CONNECTED DEVICES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        devices.forEach { device ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (device.isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (device.type.contains("Phone")) Icons.Filled.Smartphone else Icons.Filled.Laptop,
                                contentDescription = null,
                                tint = if (device.isOnline) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(text = device.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = device.type, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(
                        text = device.lastActive,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (device.isOnline) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Data Snapshot Backup / Export Card
        Text(
            text = "SNAPSHOT BACKUP & RESTORE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Sync Snapshot Transfer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Transfer your blocked website lists, timers, and focus preferences using JSON snapshots.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            exportedJsonText = onExportSyncData()
                            showExportDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("export_sync_button")
                    ) {
                        Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Config", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            importJsonText = ""
                            showImportDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("import_sync_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Config", fontSize = 12.sp)
                    }
                }
            }
        }

        // Chrome Extension Helper Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4285F4).copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Extension, contentDescription = null, tint = Color(0xFF4285F4))
                    Column {
                        Text(
                            text = "Need Chrome Extension Help?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "Step-by-step instructions to install on laptop",
                            fontSize = 11.sp,
                            color = Color(0xFF1E3A8A).copy(alpha = 0.8f)
                        )
                    }
                }

                Button(
                    onClick = onOpenChromeHelp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exported Sync Snapshot", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copy this configuration to sync with your laptop or store as backup:", fontSize = 12.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
                    ) {
                        Text(
                            text = exportedJsonText,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    copyToClipboard("Sync JSON", exportedJsonText)
                    showExportDialog = false
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Sync Snapshot", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste JSON sync config from laptop or backup:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("{ \"syncCode\": ... }") }
                    )
                    OutlinedButton(
                        onClick = {
                            importJsonText = """
                                {
                                  "syncCode": "$syncCode",
                                  "timestamp": ${System.currentTimeMillis()},
                                  "laptopFocusMinutes": 75,
                                  "laptopSessionsCount": 3,
                                  "laptopDistractionsBlocked": 12,
                                  "deviceName": "Google Chrome (Laptop)"
                                }
                            """.trimIndent()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Laptop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Sample 75m Laptop Data", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onImportSyncData(importJsonText)
                        showImportDialog = false
                        if (success) {
                            Toast.makeText(context, "Successfully imported sync config! ✅", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid sync config JSON ❌", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = importJsonText.isNotBlank()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdjustLaptopDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustLaptopDialog = false },
            title = { Text("Adjust Laptop Study Stats", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter total study minutes completed on your laptop to sync with phone:",
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
                        listOf(30, 45, 60, 90).forEach { preset ->
                            SuggestionChip(
                                onClick = { adjustMinutesInput = preset.toString() },
                                label = { Text("${preset}m") }
                            )
                        }
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
                        Toast.makeText(context, "Laptop stats updated! 💻", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustLaptopDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
